package org.example.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.repository.FeedbackRepository;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.FileService;
import org.example.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final CurrentUserProvider currentUserProvider;

    private final FileService fileService;
    private final org.example.service.RedisService redisService;
    private final FeedbackRepository feedbackRepository;

    private static final String AVATARS_DIR = "avatars";

    @Override
    public List<UserSearchResponseDto> searchByNickname(String nickname) {

        return userRepository.findByNicknameContainingIgnoreCase(nickname).stream()
                .map(userMapper::toSearchDto)
                .toList();
    }

    @Override
    public UserProfileResponseDto getCurrentUserProfile() {

        User user = currentUserProvider.getAuthenticatedUser();
        boolean isOnline = redisService.isUserOnline(user.getId());

        int unreadFeedbackReplies = (int) feedbackRepository
                .countByUserIdAndAdminReplyIsNotNullAndReplySeenFalse(user.getId());

        return userMapper.toProfileDto(user, isOnline, unreadFeedbackReplies);
    }

    @Override
    @Transactional
    public String updateAvatar(MultipartFile file) {
        User user = currentUserProvider.getAuthenticatedUser();

        if (user.getAvatarUrl() != null) {
            fileService.deleteFile(user.getAvatarUrl());
        }

        String filePath = fileService.saveFile(AVATARS_DIR, file);
        user.setAvatarUrl(filePath);
        userRepository.save(user);

        return filePath;
    }

    @Override
    @Transactional
    public void deleteAvatar() {
        User user = currentUserProvider.getAuthenticatedUser();

        if (user.getAvatarUrl() != null) {
            fileService.deleteFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }
    }
}
