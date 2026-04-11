package org.example.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import org.example.entity.status.ChatType;

public record ChatResponseDto(
        Long id,
        ChatType type,
        Set<Long> participantIds,
        LocalDateTime createdAt
) {
}
