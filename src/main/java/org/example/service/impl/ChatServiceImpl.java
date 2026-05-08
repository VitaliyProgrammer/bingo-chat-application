package org.example.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.response.ChatListItemResponseDto;
import org.example.dto.response.ChatResponseDto;
import org.example.entity.Chat;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.exception.UserNotFoundException;
import org.example.mapper.ChatListMapper;
import org.example.mapper.ChatMapper;
import org.example.repository.ChatRepository;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.example.security.audit.SecurityAuditService;
import org.example.service.ChatService;
import org.example.service.RedisService;
import org.example.service.presence.PresenceTimeFormatter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ChatMapper chatMapper;

    private final ChatListMapper chatListMapper;

    private final PresenceTimeFormatter timeFormatter;

    private final MessageSource messageSource;

    private final RedisService redisService;

    private final SecurityAuditService securityAuditService;

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
                .findPrivateChatBetweenUsers(senderUser.getId(), receiverId);

        Locale locale = currentLocale();

        if (existingChat.isPresent()) {
            log.info("Chat already exists between users: senderId={}, receiverId={}",
                    senderId, receiverId);

            return chatMapper.toDto(existingChat.get(), timeFormatter, locale);
        }

        User receiverUser = userRepository.findById(receiverId)
                .orElseThrow(() -> {
                    log.error("Receiver user not found: chatId={}", receiverId);
                    return new UserNotFoundException("User not found!");
                });

        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);
        chat.getParticipants().add(senderUser);
        chat.getParticipants().add(receiverUser);

        Chat savedChat = chatRepository.save(chat);

        log.info("Private chat created successfully: chatId={}, senderId={}, receiverId={}",
                savedChat.getId(), senderId, receiverId);

        return chatMapper.toDto(savedChat, timeFormatter, locale);
    }

    @Override
    public List<ChatResponseDto> getMyChats() {

        User currentUser = currentUserProvider.getAuthenticatedUser();
        Long userId = currentUser.getId();

        log.debug("Fetching chats for chatId={}", userId);

        Locale locale = currentLocale();

        List<ChatResponseDto> result = chatRepository.findAllChatsByUserId(userId).stream()
                .map(chat -> chatMapper.toDto(chat, timeFormatter, locale))
                .toList();

        log.debug("Fetched {} chats for chatId={}", result.size(), userId);

        return result;
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

        Optional<Chat> existingChat = chatRepository.findSelfChat(currentUser.getId());

        if (existingChat.isPresent()) {
            return chatMapper.toDto(existingChat.get(), timeFormatter, currentLocale());
        }

        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);

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
}
