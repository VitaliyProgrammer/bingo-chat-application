package org.example.mapper;

import java.time.LocalDateTime;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.example.dto.response.ChatResponseDto;
import org.example.entity.Chat;
import org.example.entity.status.ChatType;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-11T16:46:02+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 19.0.1 (Oracle Corporation)"
)
@Component
public class ChatMapperImpl implements ChatMapper {

    @Override
    public ChatResponseDto toDto(Chat chat) {
        if ( chat == null ) {
            return null;
        }

        Long id = null;
        LocalDateTime createdAt = null;

        id = chat.getId();
        createdAt = chat.getCreatedAt();

        Set<Long> participantIds = participants(chat);
        ChatType type = null;

        ChatResponseDto chatResponseDto = new ChatResponseDto( id, type, participantIds, createdAt );

        return chatResponseDto;
    }
}
