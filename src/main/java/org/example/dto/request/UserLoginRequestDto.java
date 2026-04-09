package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequestDto(
        @NotBlank(message = "{userName.notBlank}")
        String email,

        @NotBlank(message = "{password.notBlank}")
        String password
) {
}
