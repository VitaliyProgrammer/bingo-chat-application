package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.example.dto.response.UserSearchResponseDto;
import org.example.entity.BlockedUser;
import org.example.entity.User;
import org.example.exception.BadRequestException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.UserMapper;
import org.example.repository.BlockedUserRepository;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private UserServiceImpl userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = user(CURRENT_USER_ID);
        when(currentUserProvider.getAuthenticatedUser()).thenReturn(currentUser);
    }

    @Test
    void blockUser_notYetBlocked_savesBlockedUser() {

        User targetUser = user(OTHER_USER_ID);
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(targetUser));
        when(blockedUserRepository.existsByBlocker_IdAndBlocked_Id(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(false);

        userService.blockUser(OTHER_USER_ID);

        ArgumentCaptor<BlockedUser> captor = ArgumentCaptor.forClass(BlockedUser.class);
        verify(blockedUserRepository).save(captor.capture());
        assertThat(captor.getValue().getBlocker()).isEqualTo(currentUser);
        assertThat(captor.getValue().getBlocked()).isEqualTo(targetUser);
    }

    @Test
    void blockUser_self_throwsBadRequest() {

        when(messageSource.getMessage(
                eq("user.cannotBlockSelf"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("You can't block yourself!");

        assertThatThrownBy(() -> userService.blockUser(CURRENT_USER_ID))
                .isInstanceOf(BadRequestException.class);

        verify(blockedUserRepository, never()).save(any());
    }

    @Test
    void blockUser_targetNotFound_throwsUserNotFound() {

        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.blockUser(OTHER_USER_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(blockedUserRepository, never()).save(any());
    }

    @Test
    void blockUser_alreadyBlocked_throwsBadRequest() {

        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(user(OTHER_USER_ID)));
        when(blockedUserRepository.existsByBlocker_IdAndBlocked_Id(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(true);
        when(messageSource.getMessage(
                eq("user.alreadyBlocked"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This user is already blocked!");

        assertThatThrownBy(() -> userService.blockUser(OTHER_USER_ID))
                .isInstanceOf(BadRequestException.class);

        verify(blockedUserRepository, never()).save(any());
    }

    @Test
    void unblockUser_existingBlock_deletesIt() {

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setBlocker(currentUser);
        blockedUser.setBlocked(user(OTHER_USER_ID));

        when(blockedUserRepository.findByBlocker_IdAndBlocked_Id(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(Optional.of(blockedUser));

        userService.unblockUser(OTHER_USER_ID);

        verify(blockedUserRepository).delete(blockedUser);
    }

    @Test
    void unblockUser_notBlocked_throwsBadRequest() {

        when(blockedUserRepository.findByBlocker_IdAndBlocked_Id(CURRENT_USER_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());
        when(messageSource.getMessage(
                eq("user.notBlocked"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This user is not in your blacklist!");

        assertThatThrownBy(() -> userService.unblockUser(OTHER_USER_ID))
                .isInstanceOf(BadRequestException.class);

        verify(blockedUserRepository, never()).delete(any());
    }

    @Test
    void getBlockedUsers_returnsMappedList() {

        User blockedTarget = user(OTHER_USER_ID);
        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setBlocker(currentUser);
        blockedUser.setBlocked(blockedTarget);

        UserSearchResponseDto expectedDto = new UserSearchResponseDto(
                OTHER_USER_ID, "Jane", "Doe", "janedoe");

        when(blockedUserRepository.findAllByBlockerId(CURRENT_USER_ID))
                .thenReturn(List.of(blockedUser));
        when(userMapper.toSearchDto(blockedTarget)).thenReturn(expectedDto);

        List<UserSearchResponseDto> result = userService.getBlockedUsers();

        assertThat(result).containsExactly(expectedDto);
    }

    private User user(Long id) {
        User result = new User();
        result.setId(id);
        return result;
    }
}
