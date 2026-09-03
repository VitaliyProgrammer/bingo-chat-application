package org.example.event;

public record MessageReadEvent(
        Long chatId,
        Long userId
) {
}
