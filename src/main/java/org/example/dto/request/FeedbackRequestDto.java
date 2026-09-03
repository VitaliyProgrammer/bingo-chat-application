package org.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.entity.type.FeedbackType;

public record FeedbackRequestDto(
        @NotBlank(message = "{email.notBlank}")
        @Email(message = "{email.invalid}")
        String email,
        @NotNull(message = "{feedback.type.notNull}")
        FeedbackType type,
        @NotBlank(message = "{feedback.message.notBlank}")
        @Size(max = 5000, message = "{feedback.message.size}")
        String message
) {
}
