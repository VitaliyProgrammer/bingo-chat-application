package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ChatListItemResponseDto(
        Long chatId,
        Long companionId,
        String userName,
        String avatarUrl,
        String lastMessage,
        int unreadCount,
        String presenceStatus,
        boolean isOnline,
        boolean selfChat,
        Boolean isPrivate,
        @JsonFormat(pattern = "yyyy-MM-dd' T 'HH:mm")
        LocalDateTime lastActivityTime
) {
}
