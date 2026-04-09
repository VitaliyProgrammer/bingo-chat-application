package org.example.mapper;

import org.example.dto.request.UserRegistrationRequestDto;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserRegistrationRequestDto dto);

    UserRegistrationResponseDto toRegistrationDto(User user);

    UserProfileResponseDto toProfileDto(User user);

    UserSearchResponseDto toSearchDto(User user);
}
