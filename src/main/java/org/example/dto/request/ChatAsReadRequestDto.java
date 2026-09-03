package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatAsReadRequestDto(
        @NotNull(message = "{chatId.notNull}")
        Long chatId
) {
}
