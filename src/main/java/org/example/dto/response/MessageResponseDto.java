package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import org.example.entity.status.MessageStatus;

public record MessageResponseDto(
        Long id,
        Long senderId,
        Long chatId,
        String content,
        MessageStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime createdAt,
        Long replyToMessageId,
        Boolean isPinned,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime editedAt,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime reminderAt,
        List<ReactionSummaryDto> reactions
) {
}
