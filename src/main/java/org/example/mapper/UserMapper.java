package org.example.mapper;

import org.example.dto.request.UserRegistrationRequestDto;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "nickname", source = "nickName")
    User toEntity(UserRegistrationRequestDto dto);

    @Mapping(target = "nickName", source = "nickname")
    UserRegistrationResponseDto toRegistrationDto(User user);

    @Mapping(target = "nickName", source = "user.nickname")
    @Mapping(target = "isOnline", source = "isOnline")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "unreadFeedbackReplies", source = "unreadFeedbackReplies")
    UserProfileResponseDto toProfileDto(User user, boolean isOnline, int unreadFeedbackReplies);

    @Mapping(target = "nickName", source = "nickname")
    UserSearchResponseDto toSearchDto(User user);
}
