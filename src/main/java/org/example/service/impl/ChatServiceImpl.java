package org.example.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.AddParticipantsRequestDto;
import org.example.dto.request.GroupChatRequestDto;
import org.example.dto.response.ChatListItemResponseDto;
import org.example.dto.response.ChatResponseDto;
import org.example.entity.BlockedGroup;
import org.example.entity.Chat;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.exception.BadRequestException;
import org.example.exception.ChatNotFoundException;
import org.example.exception.ForbiddenActionException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.ChatListMapper;
import org.example.mapper.ChatMapper;
import org.example.repository.BlockedGroupRepository;
import org.example.repository.BlockedUserRepository;
import org.example.repository.ChatRepository;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.example.security.audit.SecurityAuditService;
import org.example.service.ChatService;
import org.example.service.FileService;
import org.example.service.RedisService;
import org.example.service.presence.PresenceTimeFormatter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    // A group needs to be visibly different from a private chat (2 people) -
    // creator + at least 2 others, so 3+ people total.
    private static final int MIN_OTHER_PARTICIPANTS = 2;

    private static final String GROUP_AVATARS_DIR = "avatars/groups";

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ChatMapper chatMapper;

    private final ChatListMapper chatListMapper;

    private final PresenceTimeFormatter timeFormatter;

    private final MessageSource messageSource;

    private final RedisService redisService;

    private final SecurityAuditService securityAuditService;

    private final FileService fileService;

    private final BlockedUserRepository blockedUserRepository;

    private final BlockedGroupRepository blockedGroupRepository;

    @Override
    @Transactional
    public ChatResponseDto createPrivateChat(Long receiverId) {

        User senderUser = currentUserProvider.getAuthenticatedUser();
        Long senderId = senderUser.getId();

        log.info("Creating private chat: senderId={}, receiverId={}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            securityAuditService.forbiddenChatAccess(senderId, receiverId);

            log.warn("User tried to create chat with himself: chatId={}", senderId);
            throw new IllegalStateException("Can`t create chat with yourself!");
        }

        Optional<Chat> existingChat = chatRepository
                .findPrivateChatBetweenUsers(senderId, receiverId);

        Locale locale = currentLocale();

        if (existingChat.isPresent()) {
            log.info("Chat already exists between users: senderId={}, receiverId={}",
                    senderId, receiverId);

            return chatMapper.toDto(existingChat.get(), timeFormatter, locale);
        }

        if (blockedUserRepository.existsBlockBetween(senderId, receiverId)) {
            throw new BadRequestException(messageSource.getMessage(
                    "user.chatBlocked", null,
                    "You can't message this user - one of you has blocked the other!", locale));
        }

        senderUser = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender user not found!"));

        final User receiverUser = userRepository.findById(receiverId)
                .orElseThrow(() -> {
                    log.error("Receiver user not found: chatId={}", receiverId);
                    return new UserNotFoundException("User not found!");
                });

        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);
        chat.setOwnerId(senderId);
        chat.setLastActivityTime(LocalDateTime.now());
        chat.getParticipants().add(senderUser);
        chat.getParticipants().add(receiverUser);

        Chat savedChat = chatRepository.save(chat);

        log.info("Private chat created successfully: chatId={}, senderId={}, receiverId={}",
                savedChat.getId(), senderId, receiverId);

        return chatMapper.toDto(savedChat, timeFormatter, locale);
    }

    @Override
    @Transactional
    public ChatResponseDto createGroupChat(GroupChatRequestDto request) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Long currentUserId = currentUser.getId();

        Set<Long> otherParticipantIds = new HashSet<>(request.participantIds());
        otherParticipantIds.remove(currentUserId);

        Locale locale = currentLocale();

        if (otherParticipantIds.size() < MIN_OTHER_PARTICIPANTS) {
            throw new BadRequestException(messageSource.getMessage(
                    "groupChat.participants.tooFew", null,
                    "A group needs at least 2 other participants besides you!", locale));
        }

        List<User> otherParticipants = userRepository.findAllById(otherParticipantIds);

        if (otherParticipants.size() != otherParticipantIds.size()) {
            log.warn("Group chat creation failed: some participants not found, ownerId={}",
                    currentUserId);
            throw new UserNotFoundException("One or more participants not found!");
        }

        Chat chat = new Chat();
        chat.setChatType(ChatType.GROUP);
        chat.setName(request.name());
        chat.setOwnerId(currentUserId);
        chat.setPrivateGroup(request.isPrivate() == null || request.isPrivate());
        chat.getParticipants().add(currentUser);
        chat.getParticipants().addAll(otherParticipants);

        Chat savedChat = chatRepository.save(chat);

        log.info("Group chat created: chatId={}, ownerId={}, participants={}",
                savedChat.getId(), currentUserId, chat.getParticipants().size());

        return chatMapper.toDto(savedChat, timeFormatter, locale);
    }

    @Override
    @Transactional
    public ChatResponseDto addParticipants(Long chatId, AddParticipantsRequestDto request) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentLocale();

        Chat chat = getChatOrThrow(chatId);
        requireNotBlocked(chat, locale);
        requireGroupOwnedByCurrentUser(chat, currentUser.getId(), locale);

        Set<Long> newParticipantIds = request.participantIds();

        boolean alreadyMember = chat.getParticipants().stream()
                .map(User::getId)
                .anyMatch(newParticipantIds::contains);

        if (alreadyMember) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.participantAlreadyMember", null,
                    "One or more users are already in this group!", locale));
        }

        List<User> newParticipants = userRepository.findAllById(newParticipantIds);

        if (newParticipants.size() != newParticipantIds.size()) {
            log.warn("Add participants failed: some users not found, chatId={}", chatId);
            throw new UserNotFoundException("One or more participants not found!");
        }

        chat.getParticipants().addAll(newParticipants);
        Chat savedChat = chatRepository.save(chat);

        log.info("Participants added: chatId={}, addedCount={}, byUserId={}",
                chatId, newParticipants.size(), currentUser.getId());

        return chatMapper.toDto(savedChat, timeFormatter, locale);
    }

    @Override
    @Transactional
    public ChatResponseDto removeParticipant(Long chatId, Long userId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentLocale();

        Chat chat = getChatOrThrow(chatId);
        requireNotBlocked(chat, locale);
        requireGroupOwnedByCurrentUser(chat, currentUser.getId(), locale);

        if (userId.equals(chat.getOwnerId())) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.ownerCannotBeRemoved", null,
                    "Owner can't be removed - transfer ownership or leave the group instead!",
                    locale));
        }

        boolean removed = chat.getParticipants().removeIf(user -> user.getId().equals(userId));

        if (!removed) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.participantNotMember", null,
                    "That user is not a member of this group!", locale));
        }

        Chat savedChat = chatRepository.save(chat);

        log.info("Participant removed: chatId={}, removedUserId={}, byUserId={}",
                chatId, userId, currentUser.getId());

        return chatMapper.toDto(savedChat, timeFormatter, locale);
    }

    @Override
    @Transactional
    public ChatResponseDto transferOwnership(Long chatId, Long newOwnerId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentLocale();

        Chat chat = getChatOrThrow(chatId);
        requireNotBlocked(chat, locale);
        requireGroupOwnedByCurrentUser(chat, currentUser.getId(), locale);

        if (newOwnerId.equals(chat.getOwnerId())) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.transferTarget.alreadyOwner", null,
                    "This user is already the owner!", locale));
        }

        boolean isMember = chat.getParticipants().stream()
                .anyMatch(user -> user.getId().equals(newOwnerId));

        if (!isMember) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.transferTarget.notMember", null,
                    "Ownership can only be transferred to a current group member!", locale));
        }

        chat.setOwnerId(newOwnerId);
        Chat savedChat = chatRepository.save(chat);

        log.info("Group ownership transferred: chatId={}, oldOwnerId={}, newOwnerId={}",
                chatId, currentUser.getId(), newOwnerId);

        return chatMapper.toDto(savedChat, timeFormatter, locale);
    }

    @Override
    @Transactional
    public String updateGroupAvatar(Long chatId, MultipartFile file) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentLocale();

        Chat chat = getChatOrThrow(chatId);
        requireNotBlocked(chat, locale);
        requireGroupOwnedByCurrentUser(chat, currentUser.getId(), locale);

        if (chat.getAvatarUrl() != null) {
            fileService.deleteFile(chat.getAvatarUrl());
        }

        String filePath = fileService.saveFile(GROUP_AVATARS_DIR, file);
        chat.setAvatarUrl(filePath);
        chatRepository.save(chat);

        log.info("Group avatar updated: chatId={}, byUserId={}", chatId, currentUser.getId());

        return filePath;
    }

    @Override
    @Transactional
    public void deleteGroupAvatar(Long chatId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentLocale();

        Chat chat = getChatOrThrow(chatId);
        requireNotBlocked(chat, locale);
        requireGroupOwnedByCurrentUser(chat, currentUser.getId(), locale);

        if (chat.getAvatarUrl() != null) {
            fileService.deleteFile(chat.getAvatarUrl());
            chat.setAvatarUrl(null);
            chatRepository.save(chat);
        }

        log.info("Group avatar deleted: chatId={}, byUserId={}", chatId, currentUser.getId());
    }

    @Override
    @Transactional
    public void blockGroupChat(Long chatId, String reason) {

        Locale locale = currentLocale();

        Chat chat = getChatOrThrow(chatId);
        requireGroupChat(chat, locale);

        if (blockedGroupRepository.existsByChat_IdAndUnblockedAtIsNull(chatId)) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.alreadyBlocked", null,
                    "This group is already blocked!", locale));
        }

        User currentUser = currentUserProvider.getAuthenticatedUser();
        BlockedGroup blockedGroup = new BlockedGroup();
        blockedGroup.setChat(chat);
        blockedGroup.setBlockedBy(currentUser);
        blockedGroup.setReason(reason);
        blockedGroupRepository.save(blockedGroup);

        log.info("Group chat blocked by support: chatId={}, byUserId={}",
                chatId, currentUser.getId());
    }

    @Override
    @Transactional
    public void unblockGroupChat(Long chatId, String reason) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Locale locale = currentLocale();

        requireGroupChat(getChatOrThrow(chatId), locale);

        BlockedGroup blockedGroup = blockedGroupRepository.findByChat_IdAndUnblockedAtIsNull(chatId)
                .orElseThrow(() -> new BadRequestException(messageSource.getMessage(
                        "chat.notBlocked", null,
                        "This group is not blocked!", locale)));

        blockedGroup.setUnblockedBy(currentUser);
        blockedGroup.setUnblockedAt(LocalDateTime.now());
        blockedGroup.setUnblockReason(reason);
        blockedGroupRepository.save(blockedGroup);

        log.info("Group chat unblocked by support: chatId={}, byUserId={}",
                chatId, currentUser.getId());
    }

    @Override
    @Transactional
    public void leaveGroupChat(Long chatId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Long currentUserId = currentUser.getId();
        Locale locale = currentLocale();

        Chat chat = getChatOrThrow(chatId);
        requireGroupChat(chat, locale);
        requireNotBlocked(chat, locale);

        if (currentUserId.equals(chat.getOwnerId()) && chat.getParticipants().size() > 1) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.ownerCannotLeaveWhileOthersPresent", null,
                    "As the owner, you can't leave while other participants remain!", locale));
        }

        chat.getParticipants().removeIf(user -> user.getId().equals(currentUserId));
        chatRepository.save(chat);

        log.info("User left group chat: chatId={}, userId={}", chatId, currentUserId);
    }

    @Override
    public List<ChatListItemResponseDto> getMyChatList() {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Long userId = currentUser.getId();

        log.debug("Building chat list for chatId={}", userId);

        Locale locale = currentLocale();

        List<ChatListItemResponseDto> result = chatRepository.findAllChatsByUserId(userId).stream()
                .map(chat -> mapToChatListItem(chat, currentUser.getId(), locale))
                .sorted(chatComparator())
                .toList();

        log.debug("Chat list built: chatId={}, size={}", userId, result.size());

        return result;
    }

    private ChatListItemResponseDto mapToChatListItem(Chat chat, Long currentUserId,
                                                      Locale locale) {

        int unreadCount = redisService.getUnreadMessages(currentUserId, chat.getId());

        String lastMessage = Optional.ofNullable(chat.getLastMessageText()).orElse("");

        if (chat.getChatType() == ChatType.SELF) {

            return new ChatListItemResponseDto(
                    chat.getId(),
                    currentUserId,
                    "Saved Messages",
                    "/images/system/saved-messages.png",
                    lastMessage,
                    unreadCount,
                    "Personal notes",
                    false,
                    true,
                    null,
                    chat.getLastActivityTime()
            );
        }

        if (chat.getChatType() == ChatType.GROUP) {

            // A group has no single "companion" - fields that only make sense for a
            // 1-on-1 chat (companionId, online presence) are left empty; avatarUrl
            // comes from the group's own uploaded picture, if any.
            return new ChatListItemResponseDto(
                    chat.getId(),
                    null,
                    chat.getName(),
                    chat.getAvatarUrl(),
                    lastMessage,
                    unreadCount,
                    null,
                    false,
                    false,
                    chat.isPrivateGroup(),
                    chat.getLastActivityTime()
            );
        }

        User companion = chat.getParticipants().stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Companion not found in chat: chatId={}, currentUserId={}",
                            chat.getId(), currentUserId);
                    return new IllegalStateException("Companion not found!");
                });

        Long companionId = companion.getId();

        boolean isOnline = redisService.isUserOnline(companionId);

        String presenceStatus = getPresenceUser(companionId, isOnline, locale);

        log.debug("ChatListItem: chatId={}, companionId={}, unread={}, online={}",
                chat.getId(), companionId, unreadCount, isOnline);

        return chatListMapper.toDto(chat, companion, lastMessage,
                unreadCount, presenceStatus, isOnline);
    }

    @Override
    @Transactional
    public ChatResponseDto createSelfChat() {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Optional<Chat> existingChat =
                chatRepository.findByOwnerIdAndChatType(currentUser.getId(), ChatType.SELF);

        if (existingChat.isPresent()) {
            return chatMapper.toDto(existingChat.get(), timeFormatter, currentLocale());
        }

        Chat chat = new Chat();
        chat.setChatType(ChatType.SELF);
        chat.setOwnerId(currentUser.getId());

        chat.getParticipants().add(currentUser);

        Chat savedChat = chatRepository.save(chat);

        return chatMapper.toDto(savedChat, timeFormatter, currentLocale());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatResponseDto getSelfChat() {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Locale locale = currentLocale();

        Chat selfChat = chatRepository
                .findByOwnerIdAndChatType(currentUser.getId(), ChatType.SELF)
                .orElseThrow(() -> {
                    log.error("SELF chat not found for userId={}", currentUser.getId());

                    return new IllegalStateException("SELF chat not found!");
                });

        log.debug("SELF chat fetched: chatId={}, userId={}", selfChat.getId(), currentUser.getId());

        return chatMapper.toDto(selfChat, timeFormatter, locale);
    }

    private Comparator<ChatListItemResponseDto> chatComparator() {

        return Comparator
                .comparing(ChatListItemResponseDto::selfChat, Comparator.reverseOrder())
                .thenComparing(ChatListItemResponseDto::isOnline, Comparator.reverseOrder())
                .thenComparing(ChatListItemResponseDto::lastActivityTime,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String getPresenceUser(Long userId, boolean isOnline, Locale locale) {

        if (isOnline) {

            return messageSource.getMessage("user.online", null, "Online",
                    locale);
        }

        Long lastSeen = redisService.getLastSeen(userId);

        if (lastSeen == null) {
            log.debug("User offline without lastSeen: chatId={}", userId);
            return messageSource.getMessage("user.offline", null, locale);
        }

        return timeFormatter.formatLastSeen(lastSeen, locale);
    }

    private Locale currentLocale() {
        return LocaleContextHolder.getLocale();
    }

    private Chat getChatOrThrow(Long chatId) {

        return chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));
    }

    private void requireGroupChat(Chat chat, Locale locale) {

        if (chat.getChatType() != ChatType.GROUP) {
            throw new BadRequestException(messageSource.getMessage(
                    "chat.notGroup", null,
                    "Only group chats support participant management!", locale));
        }
    }

    private void requireGroupOwnedByCurrentUser(Chat chat, Long currentUserId, Locale locale) {

        requireGroupChat(chat, locale);

        if (!chat.getOwnerId().equals(currentUserId)) {
            throw new ForbiddenActionException("Only the group owner can manage participants!");
        }
    }

    private void requireNotBlocked(Chat chat, Locale locale) {

        if (blockedGroupRepository.existsByChat_IdAndUnblockedAtIsNull(chat.getId())) {
            throw new ForbiddenActionException(messageSource.getMessage(
                    "chat.blockedBySupport", null,
                    "This group has been blocked by support and can't be modified!", locale));
        }
    }
}
