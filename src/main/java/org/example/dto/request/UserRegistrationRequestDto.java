package org.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.validation.PasswordMatch;

@PasswordMatch
public record UserRegistrationRequestDto(
        @Email(message = "{email.invalid}")
        @NotBlank(message = "{email.notBlank}")
        String email,

        @NotBlank(message = "{password.notBlank}")
        @Size(min = 8, message = "{password.size}")
        String password,

        @NotBlank(message = "{repeatPassword.notBlank}")
        String repeatPassword,

        @NotBlank(message = "{firstName.notBlank}")
        String firstName,

        @NotBlank(message = "{lastName.notBlank}")
        String lastName,

        @NotBlank(message = "{nickName.notBlank}")
        String nickName
) {
}
