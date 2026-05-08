package org.example.dto.request;

import org.example.entity.type.FeedbackType;

public record FeedbackRequestDto(
        String email,
        FeedbackType type,
        String message
) {
}
