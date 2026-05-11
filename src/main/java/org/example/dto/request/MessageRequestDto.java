package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageRequestDto(
        @NotNull(message = "{chatId.notNull}")
        Long chatId,
        @NotBlank(message = "{message.content.notBlank}")
        @Size(max = 2000, message = "{message.content.size}")
        String content,
        Long replyToMessageId
) {
}
