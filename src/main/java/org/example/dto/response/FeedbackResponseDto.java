package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record FeedbackResponseDto(
        Long id,
        String email,
        String type,
        String message,
        String status,
        String adminReply,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime respondedAt,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime createdAt
) {
}
