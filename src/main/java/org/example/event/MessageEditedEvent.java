package org.example.event;

import org.example.dto.response.MessageResponseDto;

public record MessageEditedEvent(
        Long chatId,
        MessageResponseDto message
) {
}
