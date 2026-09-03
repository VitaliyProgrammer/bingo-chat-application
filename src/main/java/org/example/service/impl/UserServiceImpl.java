package org.example.service.impl;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.entity.BlockedUser;
import org.example.entity.User;
import org.example.entity.type.Language;
import org.example.exception.BadRequestException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.UserMapper;
import org.example.repository.BlockedUserRepository;
import org.example.repository.FeedbackRepository;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.FileService;
import org.example.service.UserService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String AVATARS_DIR = "avatars";

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final CurrentUserProvider currentUserProvider;

    private final FileService fileService;
    private final org.example.service.RedisService redisService;
    private final FeedbackRepository feedbackRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final MessageSource messageSource;

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

    @Override
    @Transactional
    public void blockUser(Long userId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentUserProvider.getCurrentLocale();

        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException(messageSource.getMessage(
                    "user.cannotBlockSelf", null,
                    "You can't block yourself!", locale));
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        if (blockedUserRepository.existsByBlocker_IdAndBlocked_Id(currentUser.getId(), userId)) {
            throw new BadRequestException(messageSource.getMessage(
                    "user.alreadyBlocked", null,
                    "This user is already blocked!", locale));
        }

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setBlocker(currentUser);
        blockedUser.setBlocked(targetUser);
        blockedUserRepository.save(blockedUser);
    }

    @Override
    @Transactional
    public void unblockUser(Long userId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentUserProvider.getCurrentLocale();

        BlockedUser blockedUser = blockedUserRepository
                .findByBlocker_IdAndBlocked_Id(currentUser.getId(), userId)
                .orElseThrow(() -> new BadRequestException(messageSource.getMessage(
                        "user.notBlocked", null,
                        "This user is not in your blacklist!", locale)));

        blockedUserRepository.delete(blockedUser);
    }

    @Override
    public List<UserSearchResponseDto> getBlockedUsers() {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        return blockedUserRepository.findAllByBlockerId(currentUser.getId()).stream()
                .map(blockedUser -> userMapper.toSearchDto(blockedUser.getBlocked()))
                .toList();
    }

    @Override
    @Transactional
    public void updateLanguage(Language language) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        currentUser.setPreferredLanguage(language);
        userRepository.save(currentUser);
    }
}
