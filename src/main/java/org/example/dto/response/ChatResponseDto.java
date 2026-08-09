package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Set;
import org.example.entity.type.ChatType;

public record ChatResponseDto(
        Long id,
        ChatType type,
        String name,
        Long ownerId,
        Boolean isPrivate,
        String avatarUrl,
        Set<Long> participantIds,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime createdAt,
        String lastSeen
) {
}
