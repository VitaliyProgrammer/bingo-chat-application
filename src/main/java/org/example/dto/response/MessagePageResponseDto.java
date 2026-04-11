package org.example.dto.response;

import java.util.List;

public record MessagePageResponseDto(
        List<MessageResponseDto> messages,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
