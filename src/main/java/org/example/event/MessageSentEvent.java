package org.example.event;

import org.example.dto.response.MessageResponseDto;

public record MessageSentEvent(
        Long chatId,
        MessageResponseDto message
) {
}
