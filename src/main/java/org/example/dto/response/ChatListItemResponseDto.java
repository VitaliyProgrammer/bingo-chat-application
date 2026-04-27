package org.example.dto.response;

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
        LocalDateTime lastActivityTime
) {
}
