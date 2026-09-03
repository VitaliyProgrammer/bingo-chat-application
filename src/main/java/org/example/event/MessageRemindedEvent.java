package org.example.event;

import java.time.LocalDateTime;

public record MessageRemindedEvent(
        Long messageId,
        Long userId,
        Long chatId,
        String content,
        LocalDateTime reminderAt
) {
}
