package org.example.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.example.entity.Chat;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.repository.ChatRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ChatRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    private User userA;
    private User userB;
    private User userC;
    private User userD;
    private Chat privateChat;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(user("a@chat.test", "chatUserA"));
        userB = userRepository.save(user("b@chat.test", "chatUserB"));
        userC = userRepository.save(user("c@chat.test", "chatUserC"));
        userD = userRepository.save(user("d@chat.test", "chatUserD"));

        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);
        chat.setOwnerId(userA.getId());
        chat.getParticipants().add(userA);
        chat.getParticipants().add(userB);
        privateChat = chatRepository.save(chat);

        Chat group = new Chat();
        group.setChatType(ChatType.GROUP);
        group.setName("A and B in a group too");
        group.setOwnerId(userA.getId());
        group.getParticipants().add(userA);
        group.getParticipants().add(userB);
        group.getParticipants().add(userC);
        chatRepository.save(group);
    }

    @Test
    void findPrivateChatBetweenUsers_queriedInEitherOrder_returnsSameChat() {
        Optional<Chat> forward = chatRepository.findPrivateChatBetweenUsers(userA.getId(), userB.getId());
        Optional<Chat> reverse = chatRepository.findPrivateChatBetweenUsers(userB.getId(), userA.getId());

        assertThat(forward).isPresent();
        assertThat(reverse).isPresent();
        assertThat(forward.get().getId()).isEqualTo(privateChat.getId());
        assertThat(reverse.get().getId()).isEqualTo(privateChat.getId());
    }

    @Test
    void findPrivateChatBetweenUsers_noChatExistsAtAll_returnsEmpty() {
        Optional<Chat> result = chatRepository.findPrivateChatBetweenUsers(userA.getId(), userD.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findPrivateChatBetweenUsers_sharedGroupChatIsNotMistakenForPrivateChat() {
        Optional<Chat> result = chatRepository.findPrivateChatBetweenUsers(userA.getId(), userC.getId());

        assertThat(result).isEmpty();
    }

    private User user(String email, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword("password123");
        user.setFirstName("Chat");
        user.setLastName("Tester");
        return user;
    }
}
