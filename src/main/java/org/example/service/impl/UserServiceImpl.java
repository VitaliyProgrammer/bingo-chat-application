package org.example.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.UserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final CurrentUserProvider currentUserProvider;

    @Override
    public List<UserSearchResponseDto> searchByNickName(String nickname) {

        return userRepository.findByNickNameContainingIgnoreCase(nickname).stream()
                .map(userMapper::toSearchDto)
                .toList();
    }

    @Override
    public UserProfileResponseDto getCurrentUserProfile() {

        User user = currentUserProvider.getAuthenticatedUser();

        return userMapper.toProfileDto(user);
    }
}
