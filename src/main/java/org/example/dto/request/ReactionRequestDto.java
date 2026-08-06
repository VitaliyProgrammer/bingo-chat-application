package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequestDto(
        @NotBlank(message = "{reaction.emoji.notBlank}")
        @Size(max = 16, message = "{reaction.emoji.size}")
        String emoji
) {
}
