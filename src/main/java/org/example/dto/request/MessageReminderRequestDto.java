package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MessageReminderRequestDto(
        @NotNull(message = "{messageId.notNull}")
        Long messageId,
        @NotNull(message = "{reminderAt.notNull}")
        LocalDateTime reminderAt
) {
}
