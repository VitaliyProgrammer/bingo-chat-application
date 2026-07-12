package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateSelfChatRequestDto(
        @NotNull(message = "{userId.notNull}")
        Long userId
) {
}
