package org.example.service;

import java.util.List;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;

public interface UserService {

    List<UserSearchResponseDto> searchByNickName(String nickname);

    UserProfileResponseDto getCurrentUserProfile();
}
