package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditMessageRequestDto(
        @NotBlank(message = "{message.notBlank}")
        String content
) {
}
