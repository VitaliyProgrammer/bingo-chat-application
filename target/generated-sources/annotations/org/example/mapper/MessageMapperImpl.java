package org.example.mapper;

import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.example.dto.response.MessageResponseDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.entity.status.MessageStatus;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-17T19:43:15+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 19.0.1 (Oracle Corporation)"
)
@Component
public class MessageMapperImpl implements MessageMapper {

    @Override
    public MessageResponseDto toDto(Message message) {
        if ( message == null ) {
            return null;
        }

        Long senderId = null;
        Long chatId = null;
        Long replyToMessageId = null;
        Long id = null;
        String content = null;
        MessageStatus status = null;
        LocalDateTime createdAt = null;

        senderId = messageSenderId( message );
        chatId = messageChatId( message );
        replyToMessageId = messageReplyToId( message );
        id = message.getId();
        content = message.getContent();
        status = message.getStatus();
        createdAt = message.getCreatedAt();

        MessageResponseDto messageResponseDto = new MessageResponseDto( id, senderId, chatId, content, status, createdAt, replyToMessageId );

        return messageResponseDto;
    }

    private Long messageSenderId(Message message) {
        if ( message == null ) {
            return null;
        }
        User sender = message.getSender();
        if ( sender == null ) {
            return null;
        }
        Long id = sender.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long messageChatId(Message message) {
        if ( message == null ) {
            return null;
        }
        Chat chat = message.getChat();
        if ( chat == null ) {
            return null;
        }
        Long id = chat.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long messageReplyToId(Message message) {
        if ( message == null ) {
            return null;
        }
        Message replyTo = message.getReplyTo();
        if ( replyTo == null ) {
            return null;
        }
        Long id = replyTo.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
