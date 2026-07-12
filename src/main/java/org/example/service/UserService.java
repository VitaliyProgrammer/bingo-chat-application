package org.example.service;

import java.util.List;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    List<UserSearchResponseDto> searchByNickname(String nickname);

    UserProfileResponseDto getCurrentUserProfile();

    String updateAvatar(MultipartFile file);

    void deleteAvatar();
}
