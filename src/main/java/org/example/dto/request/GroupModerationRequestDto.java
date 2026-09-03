package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupModerationRequestDto(
        @NotBlank(message = "{groupBlock.reason.notBlank}")
        @Size(max = 500, message = "{groupBlock.reason.size}")
        String reason
) {
}
