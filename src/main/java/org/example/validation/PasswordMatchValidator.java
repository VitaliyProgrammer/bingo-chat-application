package org.example.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.dto.request.UserRegistrationRequestDto;

public class PasswordMatchValidator
        implements ConstraintValidator<PasswordMatch, UserRegistrationRequestDto> {

    @Override
    public boolean isValid(UserRegistrationRequestDto dto, ConstraintValidatorContext context) {

        if (dto == null) {
            return true;
        }

        if (dto.password() == null || dto.repeatPassword() == null) {
            return true;
        }
        return dto.password().equals(dto.repeatPassword());
    }
}
