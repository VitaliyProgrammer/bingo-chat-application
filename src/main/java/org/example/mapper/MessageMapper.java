package org.example.mapper;

import java.util.List;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.dto.response.ReactionSummaryDto;
import org.example.entity.Message;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "replyTo.id", target = "replyToMessageId")
    @Mapping(target = "reactions", expression = "java(java.util.List.of())")
    MessageResponseDto toDto(Message message);

    default MessageResponseDto toDto(Message message, List<ReactionSummaryDto> reactions) {

        MessageResponseDto base = toDto(message);

        return new MessageResponseDto(base.id(), base.senderId(), base.chatId(), base.content(),
                base.status(), base.createdAt(), base.replyToMessageId(), base.isPinned(),
                base.editedAt(), base.reminderAt(), reactions);
    }

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
