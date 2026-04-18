package org.example.mapper;

import org.example.dto.response.ChatListItemResponseDto;
import org.example.entity.Chat;
import org.example.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatListMapper {

    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target = "userId", source = "companion.id")
    @Mapping(target = "userName", source = "companion.nickname")
    @Mapping(target = "avatarUrl", source = "companion.avatarUrl")
    @Mapping(target = "unreadCount", source = "unreadMessages")
    @Mapping(target = "lastActivityTime", source = "chat.lastActivityTime")
    ChatListItemResponseDto toDto(Chat chat, User companion, String lastMessage,
                                  int unreadMessages, String presenceStatus, boolean isOnline);
}
