package org.example.dto.response;

import java.time.LocalDateTime;

public record FeedbackResponseDto(
        Long id,
        String status,
        LocalDateTime createdAt
) {
}
