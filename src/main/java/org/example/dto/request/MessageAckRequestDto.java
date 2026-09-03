package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record MessageAckRequestDto(
        @NotNull(message = "{messageId.notNull}")
        Long messageId,
        @NotNull(message = "{chatId.notNull}")
        Long chatId
) {
}
