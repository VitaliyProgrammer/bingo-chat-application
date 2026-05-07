package org.example.dto.request;

import java.time.LocalDateTime;

public record MessageReminderRequestDto(
        Long messageId,
        LocalDateTime reminderAt
) {
}
