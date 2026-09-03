package org.example.dto.response;

import java.util.List;

public record FeedbackPageResponseDto(
        List<FeedbackResponseDto> feedback,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
