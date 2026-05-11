package org.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.validation.PasswordMatch;

@PasswordMatch
public record UserRegistrationRequestDto(
        @Email(message = "{email.invalid}")
        @NotBlank(message = "{email.notBlank}")
        @Size(max = 255, message = "{email.size}")
        String email,

        @NotBlank(message = "{password.notBlank}")
        @Size(min = 8, message = "{password.size}")
        String password,

        @NotBlank(message = "{repeatPassword.notBlank}")
        String repeatPassword,

        @NotBlank(message = "{firstName.notBlank}")
        @Size(min = 2, max = 50, message = "{firstName.size}")
        String firstName,

        @NotBlank(message = "{lastName.notBlank}")
        @Size(min = 2, max = 50, message = "{lastName.size}")
        String lastName,

        @NotBlank(message = "{nickName.notBlank}")
        @Size(min = 2, max = 50, message = "{nickName.size}")
        String nickName
) {
}
