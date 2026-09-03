package org.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequestDto(
        @NotBlank(message = "{email.notBlank}")
        @Email(message = "{email.invalid}")
        String email,

        @NotBlank(message = "{password.notBlank}")
        String password
) {
}
