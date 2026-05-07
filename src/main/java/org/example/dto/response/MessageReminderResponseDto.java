package org.example.dto.response;

import java.time.LocalDateTime;

public record MessageReminderResponseDto(
        String type,
        Long messageId,
        Long chatId,
        String content,
        LocalDateTime reminderAt
) {
}
