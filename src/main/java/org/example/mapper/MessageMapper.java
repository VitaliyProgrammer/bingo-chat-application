package org.example.mapper;

import org.example.dto.response.MessageResponseDto;
import org.example.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "replyTo.id", target = "replyToMessageId")
    MessageResponseDto toDto(Message message);
}
