package org.example.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.ChatResponseDto;
import org.example.entity.Chat;
import org.example.entity.User;
import org.example.entity.status.ChatType;
import org.example.exception.UserNotFoundException;
import org.example.mapper.ChatMapper;
import org.example.repository.ChatRepository;
import org.example.repository.UserRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.ChatService;
import org.example.service.presence.PresenceTimeFormatter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ChatMapper chatMapper;

    private final PresenceTimeFormatter timeFormatter;

    @Override
    @Transactional
    public ChatResponseDto createPrivateChat(Long userId) {

        User senderUser = currentUserProvider.getAuthenticatedUser();

        if (senderUser.getId().equals(userId)) {
            throw new IllegalStateException("Can`t create chat with yourself!");
        }

        Optional<Chat> existingChat = chatRepository
                .findPrivateChatBetweenUsers(senderUser.getId(), userId);

        Locale locale = LocaleContextHolder.getLocale();

        if (existingChat.isPresent()) {
            return chatMapper.toDto(existingChat.get(), timeFormatter, locale);
        }

        User receiverUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);
        chat.getParticipants().add(senderUser);
        chat.getParticipants().add(receiverUser);

        Chat savedChat = chatRepository.save(chat);

        return chatMapper.toDto(savedChat, timeFormatter, locale);
    }

    @Override
    public List<ChatResponseDto> getMyChats() {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Locale locale = LocaleContextHolder.getLocale();

        return chatRepository.findAllChatsByUserId(currentUser.getId()).stream()
                .map(chat -> chatMapper.toDto(chat, timeFormatter, locale))
                .toList();
    }
}
