package org.example.dto.response;

import java.time.LocalDateTime;
import org.example.entity.status.MessageStatus;

public record MessageResponseDto(
        Long id,
        Long senderId,
        Long chatId,
        String content,
        MessageStatus status,
        LocalDateTime createdAt,
        Long replyToMessageId,
        Boolean isPinned,
        LocalDateTime editedAt,
        LocalDateTime reminderAt
) {
}
