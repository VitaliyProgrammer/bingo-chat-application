package org.example.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import org.example.dto.response.ChatResponseDto;
import org.example.entity.Chat;
import org.example.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "participantIds", expression = "java(participants(chat))")
    ChatResponseDto toDto(Chat chat);

    default Set<Long> participants(Chat chat) {

        return chat.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }
}
