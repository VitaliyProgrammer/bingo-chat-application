package org.example.dto.response;

public record ChatListItemResponseDto(
        Long chatId,
        Long userId,
        String userName,
        String avatarUrl,
        String lastMessage,
        int unreadCount,
        String presenceStatus,
        boolean isOnline
) {
}
