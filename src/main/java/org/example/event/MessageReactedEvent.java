package org.example.event;

import org.example.dto.response.MessageResponseDto;

public record MessageReactedEvent(
        Long chatId,
        MessageResponseDto message
) {
}
