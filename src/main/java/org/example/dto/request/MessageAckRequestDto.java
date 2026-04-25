package org.example.dto.request;

public record MessageAckRequestDto(
        Long messageId,
        Long chatId
) {
}
