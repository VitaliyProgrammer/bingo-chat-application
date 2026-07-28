package org.example.dto.request;

import jakarta.validation.constraints.Size;
import org.example.entity.status.FeedbackStatus;

public record FeedbackUpdateRequestDto(
        FeedbackStatus status,
        @Size(max = 5000, message = "{feedback.adminReply.size}")
        String adminReply
) {
}
