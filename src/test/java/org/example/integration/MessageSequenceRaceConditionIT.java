package org.example.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.example.dto.request.MessageRequestDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.repository.ChatRepository;
import org.example.repository.MessageRepository;
import org.example.repository.UserRepository;
import org.example.security.websocket.model.WebSocketPrincipal;
import org.example.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class MessageSequenceRaceConditionIT extends AbstractIntegrationTest {

    private static final int CONCURRENT_SENDS = 30;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Long chatId;
    private Long senderId;

    @BeforeEach
    void setUp() {
        User sender = userRepository.save(user("sender@race.test", "raceSender"));
        User recipient = userRepository.save(user("recipient@race.test", "raceRecipient"));

        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);
        chat.setOwnerId(sender.getId());
        chat.getParticipants().add(sender);
        chat.getParticipants().add(recipient);
        chat = chatRepository.save(chat);

        this.chatId = chat.getId();
        this.senderId = sender.getId();
    }

    @Test
    void concurrentSends_intoSameChat_produceNoDuplicateSequences() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_SENDS);
        CountDownLatch startGate = new CountDownLatch(1);
        WebSocketPrincipal principal = new WebSocketPrincipal(senderId);

        List<Future<?>> futures = IntStream.range(0, CONCURRENT_SENDS)
                .mapToObj(i -> executor.submit(() -> {
                    awaitUninterruptibly(startGate);
                    messageService.sendMessage(
                            new MessageRequestDto(chatId, "message " + i, null), principal);
                }))
                .collect(Collectors.toList());

        startGate.countDown();

        List<Exception> unexpected = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception exception) {
                unexpected.add(exception);
            }
        }
        executor.shutdown();

        assertThat(unexpected).isEmpty();

        List<Message> messages = messageRepository
                .findAllByChatId(chatId, PageRequest.of(0, CONCURRENT_SENDS + 10))
                .getContent();

        Set<Long> sequences = messages.stream().map(Message::getSequence).collect(Collectors.toSet());

        assertThat(messages).hasSize(CONCURRENT_SENDS);
        assertThat(sequences).hasSize(CONCURRENT_SENDS);
        assertThat(sequences).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, CONCURRENT_SENDS).mapToObj(i -> (long) i).toList());
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
        user.setFirstName("Race");
        user.setLastName("Tester");
        return user;
    }
}
