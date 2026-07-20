package org.example.mapper;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.dto.response.ChatResponseDto;
import org.example.entity.Chat;
import org.example.entity.User;
import org.example.service.presence.PresenceTimeFormatter;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "type", source = "chatType")
    @Mapping(target = "participantIds", expression = "java(participants(chat))")
    @Mapping(target = "lastSeen", expression = "java(formatLastSeen(chat, timeFormatter, locale))")
    ChatResponseDto toDto(Chat chat, @Context PresenceTimeFormatter timeFormatter,
                          @Context Locale locale);

    default Set<Long> participants(Chat chat) {

        return chat.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    default String formatLastSeen(Chat chat, PresenceTimeFormatter timeFormatter, Locale locale) {

        if (chat.getLastActivityTime() == null) {
            return null;
        }

        long milliSeconds = chat.getLastActivityTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        return timeFormatter.formatLastSeen(milliSeconds, locale);
    }
}
