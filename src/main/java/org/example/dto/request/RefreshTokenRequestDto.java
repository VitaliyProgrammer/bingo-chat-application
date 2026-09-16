package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
        @NotBlank(message = "{refreshToken.notBlank}")
        String refreshToken
) {
}
