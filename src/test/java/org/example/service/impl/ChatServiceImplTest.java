package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.example.dto.request.AddParticipantsRequestDto;
import org.example.dto.request.GroupChatRequestDto;
import org.example.entity.BlockedGroup;
import org.example.entity.Chat;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.exception.BadRequestException;
import org.example.exception.ForbiddenActionException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.ChatMapper;
import org.example.repository.BlockedGroupRepository;
import org.example.repository.BlockedUserRepository;
import org.example.repository.ChatRepository;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit tests for group chat creation. All collaborators are mocked - this only
 * proves the business rules (participant count, missing users, dedup of the
 * creator) are enforced correctly; it does not touch a real database.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long CHAT_ID = 10L;
    private static final String TOO_FEW_MESSAGE = "A group needs at least 2 other participants besides you!";

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MessageSource messageSource;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private FileService fileService;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private BlockedGroupRepository blockedGroupRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = user(CURRENT_USER_ID);
        lenient().when(currentUserProvider.getAuthenticatedUser()).thenReturn(currentUser);
    }

    @Test
    void createGroupChat_withEnoughParticipants_savesGroupWithCreatorIncluded() {

        when(userRepository.findAllById(Set.of(2L, 3L)))
                .thenReturn(List.of(user(2L), user(3L)));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.createGroupChat(new GroupChatRequestDto("Team Alpha", Set.of(2L, 3L), null));

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());

        Chat savedChat = captor.getValue();
        assertThat(savedChat.getChatType()).isEqualTo(ChatType.GROUP);
        assertThat(savedChat.getName()).isEqualTo("Team Alpha");
        assertThat(savedChat.getOwnerId()).isEqualTo(CURRENT_USER_ID);
        assertThat(savedChat.isPrivateGroup()).isTrue();
        assertThat(savedChat.getParticipants())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void createGroupChat_explicitlyPublic_savesGroupAsNotPrivate() {

        when(userRepository.findAllById(Set.of(2L, 3L)))
                .thenReturn(List.of(user(2L), user(3L)));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.createGroupChat(new GroupChatRequestDto("Public Club", Set.of(2L, 3L), false));

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());

        assertThat(captor.getValue().isPrivateGroup()).isFalse();
    }

    @Test
    void createGroupChat_creatorIncludedInParticipantIds_isDedupedNotDuplicated() {

        when(userRepository.findAllById(Set.of(2L, 3L)))
                .thenReturn(List.of(user(2L), user(3L)));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Client accidentally included the creator's own ID (1L) alongside the others.
        chatService.createGroupChat(new GroupChatRequestDto("Team Gamma", Set.of(1L, 2L, 3L), null));

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());

        assertThat(captor.getValue().getParticipants())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void createGroupChat_fewerThanTwoOtherParticipants_throwsBadRequestException() {

        when(messageSource.getMessage(
                eq("groupChat.participants.tooFew"), isNull(), anyString(), any(Locale.class)))
                .thenReturn(TOO_FEW_MESSAGE);

        assertThatThrownBy(() ->
                chatService.createGroupChat(new GroupChatRequestDto("Too Small", Set.of(2L), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(TOO_FEW_MESSAGE);

        verify(userRepository, never()).findAllById(any());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void createGroupChat_participantDoesNotExist_throwsUserNotFoundException() {

        // Only 2L is a real user - 999L doesn't exist.
        when(userRepository.findAllById(Set.of(2L, 999L)))
                .thenReturn(List.of(user(2L)));

        assertThatThrownBy(() ->
                chatService.createGroupChat(new GroupChatRequestDto("Team Beta", Set.of(2L, 999L), null)))
                .isInstanceOf(UserNotFoundException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void addParticipants_ownerAddsNewUsers_savesUpdatedGroup() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L), user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(userRepository.findAllById(Set.of(4L, 5L))).thenReturn(List.of(user(4L), user(5L)));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.addParticipants(CHAT_ID, new AddParticipantsRequestDto(Set.of(4L, 5L)));

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void addParticipants_calledByNonOwner_throwsForbidden() {

        // Owner is 2L, not the current user (1L) - just a regular member.
        Chat chat = groupChat(CHAT_ID, 2L, user(2L), currentUser, user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() ->
                chatService.addParticipants(CHAT_ID, new AddParticipantsRequestDto(Set.of(4L))))
                .isInstanceOf(ForbiddenActionException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void addParticipants_onPrivateChat_throwsBadRequest() {

        Chat chat = privateChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageSource.getMessage(eq("chat.notGroup"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("Only group chats support participant management!");

        assertThatThrownBy(() ->
                chatService.addParticipants(CHAT_ID, new AddParticipantsRequestDto(Set.of(4L))))
                .isInstanceOf(BadRequestException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void addParticipants_alreadyMember_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L), user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageSource.getMessage(
                eq("chat.participantAlreadyMember"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("One or more users are already in this group!");

        assertThatThrownBy(() ->
                chatService.addParticipants(CHAT_ID, new AddParticipantsRequestDto(Set.of(2L, 4L))))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).findAllById(any());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void addParticipants_missingUser_throwsUserNotFound() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L), user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(userRepository.findAllById(Set.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() ->
                chatService.addParticipants(CHAT_ID, new AddParticipantsRequestDto(Set.of(999L))))
                .isInstanceOf(UserNotFoundException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void removeParticipant_ownerRemovesMember_savesUpdatedGroup() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L), user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.removeParticipant(CHAT_ID, 2L);

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void removeParticipant_calledByNonOwner_throwsForbidden() {

        Chat chat = groupChat(CHAT_ID, 2L, user(2L), currentUser, user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.removeParticipant(CHAT_ID, 3L))
                .isInstanceOf(ForbiddenActionException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void removeParticipant_targetIsOwner_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L), user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageSource.getMessage(
                eq("chat.ownerCannotBeRemoved"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("Owner can't be removed - transfer ownership or leave the group instead!");

        assertThatThrownBy(() -> chatService.removeParticipant(CHAT_ID, CURRENT_USER_ID))
                .isInstanceOf(BadRequestException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void removeParticipant_targetNotMember_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageSource.getMessage(
                eq("chat.participantNotMember"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("That user is not a member of this group!");

        assertThatThrownBy(() -> chatService.removeParticipant(CHAT_ID, 999L))
                .isInstanceOf(BadRequestException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void transferOwnership_ownerToExistingMember_updatesOwner() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L), user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.transferOwnership(CHAT_ID, 2L);

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());

        Chat savedChat = captor.getValue();
        assertThat(savedChat.getOwnerId()).isEqualTo(2L);
        assertThat(savedChat.getParticipants())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void transferOwnership_calledByNonOwner_throwsForbidden() {

        // Owner is 2L, not the current user (1L) - just a regular member.
        Chat chat = groupChat(CHAT_ID, 2L, user(2L), currentUser, user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.transferOwnership(CHAT_ID, 3L))
                .isInstanceOf(ForbiddenActionException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void transferOwnership_targetNotMember_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageSource.getMessage(
                eq("chat.transferTarget.notMember"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("Ownership can only be transferred to a current group member!");

        assertThatThrownBy(() -> chatService.transferOwnership(CHAT_ID, 999L))
                .isInstanceOf(BadRequestException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void transferOwnership_targetAlreadyOwner_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageSource.getMessage(
                eq("chat.transferTarget.alreadyOwner"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This user is already the owner!");

        assertThatThrownBy(() -> chatService.transferOwnership(CHAT_ID, CURRENT_USER_ID))
                .isInstanceOf(BadRequestException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void updateGroupAvatar_owner_savesFileAndUpdatesChat() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        MultipartFile file = new MockMultipartFile("file", "avatar.png",
                "image/png", new byte[]{1, 2, 3});

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(fileService.saveFile(eq("avatars/groups"), eq(file)))
                .thenReturn("avatars/groups/new-avatar.png");
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = chatService.updateGroupAvatar(CHAT_ID, file);

        assertThat(result).isEqualTo("avatars/groups/new-avatar.png");
        verify(fileService, never()).deleteFile(any());

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getAvatarUrl()).isEqualTo("avatars/groups/new-avatar.png");
    }

    @Test
    void updateGroupAvatar_existingAvatar_deletesOldFileFirst() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        chat.setAvatarUrl("avatars/groups/old-avatar.png");
        MultipartFile file = new MockMultipartFile("file", "avatar.png",
                "image/png", new byte[]{1, 2, 3});

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(fileService.saveFile(eq("avatars/groups"), eq(file)))
                .thenReturn("avatars/groups/new-avatar.png");
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.updateGroupAvatar(CHAT_ID, file);

        verify(fileService).deleteFile("avatars/groups/old-avatar.png");
    }

    @Test
    void updateGroupAvatar_calledByNonOwner_throwsForbidden() {

        // Owner is 2L, not the current user (1L) - just a regular member.
        Chat chat = groupChat(CHAT_ID, 2L, user(2L), currentUser);
        MultipartFile file = new MockMultipartFile("file", "avatar.png",
                "image/png", new byte[]{1, 2, 3});

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.updateGroupAvatar(CHAT_ID, file))
                .isInstanceOf(ForbiddenActionException.class);

        verify(fileService, never()).saveFile(any(), any());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void deleteGroupAvatar_owner_removesFileAndClearsUrl() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        chat.setAvatarUrl("avatars/groups/old-avatar.png");

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.deleteGroupAvatar(CHAT_ID);

        verify(fileService).deleteFile("avatars/groups/old-avatar.png");

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getAvatarUrl()).isNull();
    }

    @Test
    void deleteGroupAvatar_calledByNonOwner_throwsForbidden() {

        Chat chat = groupChat(CHAT_ID, 2L, user(2L), currentUser);
        chat.setAvatarUrl("avatars/groups/old-avatar.png");

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.deleteGroupAvatar(CHAT_ID))
                .isInstanceOf(ForbiddenActionException.class);

        verify(fileService, never()).deleteFile(any());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void leaveGroupChat_regularParticipant_leavesSuccessfully() {

        Chat chat = groupChat(CHAT_ID, 2L, user(2L), currentUser, user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.leaveGroupChat(CHAT_ID);

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void leaveGroupChat_ownerWithOthersPresent_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L), user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageSource.getMessage(
                eq("chat.ownerCannotLeaveWhileOthersPresent"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("As the owner, you can't leave while other participants remain!");

        assertThatThrownBy(() -> chatService.leaveGroupChat(CHAT_ID))
                .isInstanceOf(BadRequestException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void leaveGroupChat_ownerAlone_leavesSuccessfully() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.leaveGroupChat(CHAT_ID);

        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants()).isEmpty();
    }

    @Test
    void blockGroupChat_success_savesReasonAndBlockedBy() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(blockedGroupRepository.existsByChat_IdAndUnblockedAtIsNull(CHAT_ID)).thenReturn(false);
        when(blockedGroupRepository.save(any(BlockedGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.blockGroupChat(CHAT_ID, "Spam");

        ArgumentCaptor<BlockedGroup> captor = ArgumentCaptor.forClass(BlockedGroup.class);
        verify(blockedGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getChat()).isEqualTo(chat);
        assertThat(captor.getValue().getBlockedBy()).isEqualTo(currentUser);
        assertThat(captor.getValue().getReason()).isEqualTo("Spam");
    }

    @Test
    void blockGroupChat_alreadyBlocked_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(blockedGroupRepository.existsByChat_IdAndUnblockedAtIsNull(CHAT_ID)).thenReturn(true);
        when(messageSource.getMessage(eq("chat.alreadyBlocked"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This group is already blocked!");

        assertThatThrownBy(() -> chatService.blockGroupChat(CHAT_ID, "Spam"))
                .isInstanceOf(BadRequestException.class);

        verify(blockedGroupRepository, never()).save(any());
    }

    @Test
    void unblockGroupChat_success_fillsUnblockedFields() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        BlockedGroup blockedGroup = new BlockedGroup();
        blockedGroup.setChat(chat);
        blockedGroup.setBlockedBy(user(2L));
        blockedGroup.setReason("Spam");

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(blockedGroupRepository.findByChat_IdAndUnblockedAtIsNull(CHAT_ID))
                .thenReturn(Optional.of(blockedGroup));
        when(blockedGroupRepository.save(any(BlockedGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.unblockGroupChat(CHAT_ID, "Resolved");

        ArgumentCaptor<BlockedGroup> captor = ArgumentCaptor.forClass(BlockedGroup.class);
        verify(blockedGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getUnblockedBy()).isEqualTo(currentUser);
        assertThat(captor.getValue().getUnblockedAt()).isNotNull();
        assertThat(captor.getValue().getUnblockReason()).isEqualTo("Resolved");
    }

    @Test
    void unblockGroupChat_notBlocked_throwsBadRequest() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(blockedGroupRepository.findByChat_IdAndUnblockedAtIsNull(CHAT_ID))
                .thenReturn(Optional.empty());
        when(messageSource.getMessage(eq("chat.notBlocked"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This group is not blocked!");

        assertThatThrownBy(() -> chatService.unblockGroupChat(CHAT_ID, "Resolved"))
                .isInstanceOf(BadRequestException.class);

        verify(blockedGroupRepository, never()).save(any());
    }

    @Test
    void addParticipants_blockedGroup_throwsForbidden() {

        Chat chat = groupChat(CHAT_ID, CURRENT_USER_ID, currentUser, user(2L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(blockedGroupRepository.existsByChat_IdAndUnblockedAtIsNull(CHAT_ID)).thenReturn(true);
        when(messageSource.getMessage(
                eq("chat.blockedBySupport"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This group has been blocked by support and can't be modified!");

        assertThatThrownBy(() ->
                chatService.addParticipants(CHAT_ID, new AddParticipantsRequestDto(Set.of(4L))))
                .isInstanceOf(ForbiddenActionException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void leaveGroupChat_blockedGroup_throwsForbidden() {

        Chat chat = groupChat(CHAT_ID, 2L, user(2L), currentUser, user(3L));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(blockedGroupRepository.existsByChat_IdAndUnblockedAtIsNull(CHAT_ID)).thenReturn(true);
        when(messageSource.getMessage(
                eq("chat.blockedBySupport"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This group has been blocked by support and can't be modified!");

        assertThatThrownBy(() -> chatService.leaveGroupChat(CHAT_ID))
                .isInstanceOf(ForbiddenActionException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void createPrivateChat_blockedPair_throwsBadRequest() {

        when(chatRepository.findPrivateChatBetweenUsers(CURRENT_USER_ID, 2L))
                .thenReturn(Optional.empty());
        when(blockedUserRepository.existsBlockBetween(CURRENT_USER_ID, 2L)).thenReturn(true);
        when(messageSource.getMessage(eq("user.chatBlocked"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("You can't message this user - one of you has blocked the other!");

        assertThatThrownBy(() -> chatService.createPrivateChat(2L))
                .isInstanceOf(BadRequestException.class);

        verify(chatRepository, never()).save(any());
    }

    private User user(Long id) {
        User result = new User();
        result.setId(id);
        return result;
    }

    private Chat groupChat(Long chatId, Long ownerId, User... participants) {
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setChatType(ChatType.GROUP);
        chat.setOwnerId(ownerId);
        chat.setName("Test Group");
        chat.getParticipants().addAll(Set.of(participants));
        return chat;
    }

    private Chat privateChat(Long chatId, Long ownerId, User... participants) {
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setChatType(ChatType.PRIVATE);
        chat.setOwnerId(ownerId);
        chat.getParticipants().addAll(Set.of(participants));
        return chat;
    }
}
