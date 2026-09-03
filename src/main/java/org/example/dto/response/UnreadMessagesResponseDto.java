package org.example.dto.response;

public record UnreadMessagesResponseDto(
        Long chatId,
        int count
) {
}
