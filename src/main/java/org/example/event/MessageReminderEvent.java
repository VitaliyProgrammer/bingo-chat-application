package org.example.event;

import java.time.LocalDateTime;

public record MessageReminderEvent(
        Long messageId,
        Long userId,
        Long chatId,
        String content,
        LocalDateTime reminderAt
) {
}
