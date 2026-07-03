package org.example.service;

import java.util.List;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;

public interface UserService {

    List<UserSearchResponseDto> searchByNickname(String nickname);

    UserProfileResponseDto getCurrentUserProfile();
}
