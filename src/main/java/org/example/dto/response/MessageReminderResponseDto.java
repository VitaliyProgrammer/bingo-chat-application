package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record MessageReminderResponseDto(
        String type,
        Long messageId,
        Long chatId,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime reminderAt
) {
}
