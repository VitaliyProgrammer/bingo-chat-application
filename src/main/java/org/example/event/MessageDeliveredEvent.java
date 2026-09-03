package org.example.event;

public record MessageDeliveredEvent(
        Long chatId,
        Long messageId,
        Long userId
) {
}
