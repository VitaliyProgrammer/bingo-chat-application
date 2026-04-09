package org.example.dto.request;

public record MessageRequestDto(
        Long chatId,
        String content,
        Long replyToMessageId
) {
}
