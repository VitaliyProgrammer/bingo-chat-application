package org.example.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.dto.response.UnreadMessagesResponseDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.entity.status.MessageStatus;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageSentEvent;
import org.example.exception.ChatNotFoundException;
import org.example.exception.ForbiddenActionException;
import org.example.exception.MessageNotFoundException;
import org.example.mapper.MessageMapper;
import org.example.repository.ChatRepository;
import org.example.repository.MessageRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.MessageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    private final ChatRepository chatRepository;

    private final CurrentUserProvider currentUserProvider;

    private final MessageMapper messageMapper;

    private final RedisService redisService;
    private final SimpMessagingTemplate messagingTemplate;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void openChat(Long chatId) {

        User senderUser = currentUserProvider.getAuthenticatedUser();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));

        validateUserInChat(chat, senderUser);

        log.info("Opening chat: chatId={}, userId={}", chatId, senderUser.getId());

        redisService.resetUnReadMessages(senderUser.getId(), chatId);

        markChatAsRead(chatId);
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(MessageRequestDto request) {

        User senderUser = currentUserProvider.getAuthenticatedUser();

        Chat chat = chatRepository.findById(request.chatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));

        validateUserInChat(chat, senderUser);

        Message message = new Message();
        message.setChat(chat);
        message.setSender(senderUser);
        message.setContent(request.content());
        message.setStatus(MessageStatus.SENT);

        replyToMessage(request, message, chat);

        Message savedMessage = messageRepository.save(message);

        chat.setLastMessageText(savedMessage.getContent());
        chat.setLastActivityTime(LocalDateTime.now());

        log.info("Message sent: messageId={}, chatId={}, senderId={}",
                savedMessage.getId(), chat.getId(), senderUser.getId());

        chat.getParticipants().forEach(user -> {
            if (!user.getId().equals(senderUser.getId())) {
                redisService.incrementUnreadMessages(user.getId(), chat.getId());
            }
        });

        MessageResponseDto response = messageMapper.toDto(savedMessage);

        eventPublisher.publishEvent(new MessageSentEvent(chat.getId(), response));

        return response;
    }

    @Override
    @Transactional
    public MessageResponseDto editMessage(Long messageId, String newContent) {

        User user = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateMessageOwner(message, user);

        message.setContent(newContent);

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public MessageResponseDto markAsDelivered(Long messageId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateUserInChat(message.getChat(), currentUser);
        validateNotSender(message, currentUser);

        if (message.getStatus() == MessageStatus.DELIVERED
                || message.getStatus() == MessageStatus.READ) {

            log.debug("Skip duplicate DELIVERED event: messageId={}", messageId);
            return messageMapper.toDto(message);
        }

        validateStatusTransition(message, MessageStatus.DELIVERED);
        message.setStatus(MessageStatus.DELIVERED);

        log.info("Message delivered: messageId={}, userId={}", messageId, currentUser.getId());

        eventPublisher.publishEvent(new MessageDeliveredEvent(
                message.getChat().getId(), message.getId(), currentUser.getId())
        );

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public MessageResponseDto markAsRead(Long messageId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateUserInChat(message.getChat(), currentUser);
        validateNotSender(message, currentUser);

        if (message.getStatus() == MessageStatus.READ) {
            log.debug("Skip duplicate READ event: messageId={}", messageId);
            return messageMapper.toDto(message);
        }

        validateStatusTransition(message, MessageStatus.READ);
        message.setStatus(MessageStatus.READ);

        log.info("Message read: messageId={}, userId={}", messageId, currentUser.getId());

        eventPublisher.publishEvent(new MessageReadEvent(
                message.getChat().getId(), currentUser.getId())
        );

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public void markChatAsRead(Long chatId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        int allMessagesAsRead =
                messageRepository.markAllMessagesInChatAsRead(chatId, currentUser.getId());

        if (allMessagesAsRead > 0) {

            log.info("Bulk read: chatId={}, userId={}, count={}",
                    chatId, currentUser.getId(), allMessagesAsRead);

            redisService.resetUnReadMessages(currentUser.getId(), chatId);

            eventPublisher.publishEvent(new MessageReadEvent(chatId, currentUser.getId()));
        }
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId) {

        User user = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateMessageOwner(message, user);

        messageRepository.delete(message);
    }

    @Override
    public MessagePageResponseDto getChatMessages(Long chatId, int page, int size) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));

        validateUserInChat(chat, currentUser);

        if (!chat.getParticipants().contains(currentUser)) {
            throw new ForbiddenActionException("No access to this chat!");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage = messageRepository.findAllByChatId(chatId, pageable);

        return messageMapper.toPageDto(messagePage);
    }

    @Override
    public void handleUnreadAndNotifyMessages(MessageResponseDto response) {

        Chat chat = chatRepository.findById(response.chatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));

        chat.getParticipants().forEach(user -> {

            Long userId = user.getId();

            if (userId.equals(response.senderId())) {
                return;
            }

            int unreadMessages = redisService.getUnreadMessages(userId, response.chatId());

            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/unread",
                    new UnreadMessagesResponseDto(response.chatId(), unreadMessages));
        });
    }

    private Message getMessageOrThrow(Long messageId) {

        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found!"));
    }

    private void validateUserInChat(Chat chat, User user) {

        if (!chat.getParticipants().contains(user)) {
            throw new ForbiddenActionException("You are not a participant of this chat!");
        }
    }

    private void validateMessageOwner(Message message, User user) {

        if (!message.getSender().getId().equals(user.getId())) {
            throw new ForbiddenActionException("You can modify only your message!");
        }
    }

    private void validateNotSender(Message message, User user) {

        if (message.getSender().getId().equals(user.getId())) {
            throw new ForbiddenActionException("Sender can`t change message status!");
        }
    }

    private void validateStatusTransition(Message message, MessageStatus expectedStatus) {

        MessageStatus currentStatus = message.getStatus();

        if (currentStatus == MessageStatus.SENT && expectedStatus == MessageStatus.DELIVERED) {
            return;
        }
        if (currentStatus == MessageStatus.DELIVERED && expectedStatus == MessageStatus.READ) {
            return;
        }

        log.warn("Invalid status transition: messageId={}, from={}, to={}",
                message.getId(), currentStatus, expectedStatus);

        throw new IllegalStateException("Invalid status transition: " + currentStatus
                + " -> " + expectedStatus);
    }

    private void validateSameChat(Message parentMessage, Chat chat) {

        if (!parentMessage.getChat().getId().equals(chat.getId())) {
            throw new IllegalStateException("Reply message must be in the same chat!");
        }
    }

    private void replyToMessage(MessageRequestDto request, Message message, Chat chat) {

        if (request.replyToMessageId() == null) {
            return;
        }

        Message parentMessage = messageRepository.findById(request.replyToMessageId())
                .orElseThrow(() -> new MessageNotFoundException(
                        "The message for reply not found!"));

        validateSameChat(parentMessage, chat);

        message.setReplyTo(parentMessage);
    }
}
