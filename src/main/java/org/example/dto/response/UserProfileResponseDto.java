package org.example.dto.response;

public record UserProfileResponseDto(
        Long id,
        String firstName,
        String lastName,
        String nickName,
        String email,
        boolean isOnline,
        String avatarUrl
) {
}

