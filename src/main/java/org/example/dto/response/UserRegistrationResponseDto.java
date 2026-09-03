package org.example.dto.response;

public record UserRegistrationResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String nickName
) {
}
