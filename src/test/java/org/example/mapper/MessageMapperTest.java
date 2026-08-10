package org.example.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.junit.jupiter.api.Test;

class MessageMapperTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private final MessageMapper messageMapper = new MessageMapperImpl();

    @Test
    void groupOwnerSends_isAdmin() {

        Message message = message(chat(ChatType.GROUP, OWNER_ID), user(OWNER_ID));

        assertThat(messageMapper.toDto(message).isSenderAdmin()).isTrue();
    }

    @Test
    void groupMemberSends_isNotAdmin() {

        Message message = message(chat(ChatType.GROUP, OWNER_ID), user(OTHER_USER_ID));

        assertThat(messageMapper.toDto(message).isSenderAdmin()).isFalse();
    }

    @Test
    void privateChatOwnerSends_isNotAdmin() {

        // The admin badge only exists for GROUP chats - matching ownerId in a
        // PRIVATE chat must not accidentally flip the flag to true.
        Message message = message(chat(ChatType.PRIVATE, OWNER_ID), user(OWNER_ID));

        assertThat(messageMapper.toDto(message).isSenderAdmin()).isFalse();
    }

    private Chat chat(ChatType type, Long ownerId) {

        Chat chat = new Chat();
        chat.setChatType(type);
        chat.setOwnerId(ownerId);
        return chat;
    }

    private User user(Long id) {

        User user = new User();
        user.setId(id);
        return user;
    }

    private Message message(Chat chat, User sender) {

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        return message;
    }
}
