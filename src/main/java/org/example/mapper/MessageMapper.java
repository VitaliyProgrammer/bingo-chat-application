package org.example.mapper;

import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "replyTo.id", target = "replyToMessageId")
    MessageResponseDto toDto(Message message);

    default MessagePageResponseDto toPageDto(Page<Message> page) {

        return new MessagePageResponseDto(
                page.getContent().stream()
                        .map(this::toDto)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
