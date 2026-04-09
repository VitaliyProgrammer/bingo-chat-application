package org.example.dto.response;

public record UserSearchResponseDto(
        Long id,
        String firstName,
        String lastName,
        String nickName
) {
}
