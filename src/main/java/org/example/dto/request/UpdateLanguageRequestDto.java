package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.entity.type.Language;

public record UpdateLanguageRequestDto(
        @NotNull(message = "{language.notNull}")
        Language language
) {
}
