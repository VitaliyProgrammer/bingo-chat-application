package org.example.event;

import org.example.dto.response.MessageResponseDto;

public record MessagePinnedEvent(
        Long chatId,
        MessageResponseDto message
) {
}
