package org.example.dto.response;

public record TypingEventResponseDto(
        Long chatId,
        Long senderUserId,
        boolean isTyping
) {
}
