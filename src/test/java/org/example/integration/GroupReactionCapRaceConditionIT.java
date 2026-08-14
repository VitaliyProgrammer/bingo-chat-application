package org.example.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.example.dto.request.ReactionRequestDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.MessageReaction;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.exception.BadRequestException;
import org.example.repository.ChatRepository;
import org.example.repository.MessageReactionRepository;
import org.example.repository.MessageRepository;
import org.example.repository.UserRepository;
import org.example.security.UserSecurity;
import org.example.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

class GroupReactionCapRaceConditionIT extends AbstractIntegrationTest {

    private static final int PARTICIPANT_COUNT = 20;
    private static final int EXPECTED_CAP = 8;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReactionRepository messageReactionRepository;

    private Long messageId;
    private List<User> participants;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(user("owner@reaction.test", "reactionOwner"));

        participants = IntStream.range(0, PARTICIPANT_COUNT)
                .mapToObj(i -> userRepository.save(
                        user("participant" + i + "@reaction.test", "reactionUser" + i)))
                .collect(Collectors.toList());

        Chat chat = new Chat();
        chat.setChatType(ChatType.GROUP);
        chat.setName("Reaction Race Group");
        chat.setOwnerId(owner.getId());
        chat.getParticipants().add(owner);
        chat.getParticipants().addAll(participants);
        chat = chatRepository.save(chat);

        Message message = new Message();
        message.setChat(chat);
        message.setSender(owner);
        message.setContent("react to this");
        message.setSequence(1L);
        message = messageRepository.save(message);

        this.messageId = message.getId();
    }

    @Test
    void concurrentDistinctReactions_neverExceedGroupCap() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(PARTICIPANT_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Future<?>> futures = IntStream.range(0, PARTICIPANT_COUNT)
                .mapToObj(i -> executor.submit(() -> {
                    authenticateAs(participants.get(i));
                    awaitUninterruptibly(startGate);
                    try {
                        messageService.addReaction(messageId, new ReactionRequestDto("emoji" + i));
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }))
                .collect(Collectors.toList());

        startGate.countDown();

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception exception) {
                Throwable cause = exception.getCause();
                if (!(cause instanceof BadRequestException)) {
                    throw exception;
                }
            }
        }
        executor.shutdown();

        List<MessageReaction> reactions = messageReactionRepository.findByMessageId(messageId);
        Set<String> distinctEmojis = reactions.stream()
                .map(MessageReaction::getEmoji)
                .collect(Collectors.toSet());

        assertThat(distinctEmojis).hasSizeLessThanOrEqualTo(EXPECTED_CAP);
        assertThat(distinctEmojis).hasSize(EXPECTED_CAP);
    }

    private void authenticateAs(User user) {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(new UserSecurity(user), null, List.of());
        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private User user(String email, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword("password123");
        user.setFirstName("Reaction");
        user.setLastName("Tester");
        return user;
    }
}
