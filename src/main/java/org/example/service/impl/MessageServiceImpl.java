package org.example.service.impl;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.configuration.outbox.factory.OutBoxEventFactory;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.dto.request.MessageAckRequestDto;
import org.example.dto.request.MessageReminderRequestDto;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.entity.status.MessageStatus;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageReminderEvent;
import org.example.event.MessageSentEvent;
import org.example.exception.ChatNotFoundException;
import org.example.exception.ForbiddenActionException;
import org.example.exception.MessageNotFoundException;
import org.example.mapper.MessageMapper;
import org.example.repository.ChatRepository;
import org.example.repository.MessageRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.MessageService;
import org.example.service.RedisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
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

    private final RedisTemplate<String, Object> redisTemplate;

    private final OutBoxEventRepository outBoxEventRepository;

    private final OutBoxEventFactory outBoxEventFactory;

    private final ApplicationMetricsService metricsService;

    @Override
    @Transactional
    public void openChat(Long chatId) {

        User senderUser = currentUserProvider.getAuthenticatedUser();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));

        validateUserInChat(chat, senderUser);

        log.info("Opening chat: chatId={}, chatId={}", chatId, senderUser.getId());

        redisService.resetUnReadMessages(senderUser.getId(), chatId);

        markChatAsRead(chatId);
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(MessageRequestDto request) {

        User senderUser = currentUserProvider.getAuthenticatedUser();

        return sendMessageInternal(request, senderUser);
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(MessageRequestDto request, Principal principal) {

        log.debug("SEND MESSAGE CALLED chatId={}, thread={}, principal={}",
                request.chatId(), Thread.currentThread().getName(), principal.getName());

        User senderUser = currentUserProvider.getAuthenticatedUser(principal);

        return sendMessageInternal(request, senderUser);
    }

    @Override
    @Transactional
    public MessageResponseDto editMessage(Long messageId, String newContent) {

        User user = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateMessageOwner(message, user);

        message.setContent(newContent);

        message.setEditedAt(LocalDateTime.now());

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

        log.info("Message delivered: messageId={}, chatId={}", messageId, currentUser.getId());

        outBoxEventRepository.save(outBoxEventFactory.messageDelivered(
                new MessageDeliveredEvent(message.getChat().getId(), message.getId(),
                        currentUser.getId())
        ));

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

        log.info("Message read: messageId={}, chatId={}", messageId, currentUser.getId());

        outBoxEventRepository.save(outBoxEventFactory.messageRead(
                new MessageReadEvent(message.getChat().getId(), currentUser.getId())));

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public void markChatAsRead(Long chatId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        int allMessagesAsRead =
                messageRepository.markAllMessagesInChatAsRead(chatId, currentUser.getId());

        if (allMessagesAsRead > 0) {

            log.info("Bulk read: chatId={}, chatId={}, count={}",
                    chatId, currentUser.getId(), allMessagesAsRead);

            redisService.resetUnReadMessages(currentUser.getId(), chatId);

            outBoxEventRepository.save(outBoxEventFactory.messageRead(
                    new MessageReadEvent(chatId, currentUser.getId())));
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage = messageRepository.findAllByChatId(chatId, pageable);

        return messageMapper.toPageDto(messagePage);
    }

    @Override
    @Transactional
    public void acknowledge(MessageAckRequestDto request) {

        String acknowledgeKey = "ack:" + request.messageId();

        redisService.setValue(acknowledgeKey, "1", Duration.ofMinutes(10));

        redisTemplate.opsForSet().remove("pending:messages", request.messageId().toString());

        log.info("ACK received: messageId={}, chatId={}", request.messageId(), request.chatId());

        markAsDeliveredInternal(request.messageId());
    }

    @Override
    @Transactional
    public MessageResponseDto pinMessage(Long messageId) {

        Message message = getMessageOrThrow(messageId);

        validateMessageOwner(message, currentUserProvider.getAuthenticatedUser());

        message.setIsPinned(true);

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public MessageResponseDto setReminder(MessageReminderRequestDto request) {

        Message message = getMessageOrThrow(request.messageId());

        validateMessageOwner(message, currentUserProvider.getAuthenticatedUser());

        message.setReminderAt(request.reminderAt());

        outBoxEventRepository.save(outBoxEventFactory.messageReminded(new MessageReminderEvent(
                message.getId(), currentUserProvider.getAuthenticatedUser().getId(),
                message.getChat().getId(), message.getContent(), request.reminderAt())));

        return messageMapper.toDto(message);
    }

    private MessageResponseDto sendMessageInternal(MessageRequestDto request, User senderUser) {

        // Step 1: Lock chat row FIRST (blocks other concurrent requests)
        chatRepository.lockChatForUpdate(request.chatId());

        Chat chat = chatRepository.findById(request.chatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));

        validateUserInChat(chat, senderUser);

        // Step 2: Generate sequence atomically WITHIN locked transaction
        Long sequence = chatRepository.getNextMessageSequence(chat.getId());
        chat.setLastMessageSequence(sequence);

        log.debug("Generated sequence for chatId={}: sequence={}", chat.getId(), sequence);

        Message message = new Message();
        message.setChat(chat);
        message.setSender(senderUser);
        message.setContent(request.content());
        message.setStatus(MessageStatus.SENT);
        message.setSequence(sequence);

        replyToMessage(request, message, chat);

        Message savedMessage = messageRepository.save(message);

        chat.setLastMessageText(savedMessage.getContent());
        chat.setLastActivityTime(LocalDateTime.now());
        // lastMessageSequence is already set via chat.setLastMessageSequence(sequence) earlier

        chatRepository.save(chat);

        metricsService.incrementMessageSent();

        log.info("Message sent: messageId={}, chatId={}, senderId={}, sequence={}",
                savedMessage.getId(), chat.getId(), senderUser.getId(), sequence);

        redisTemplate.opsForSet().add("pending:messages", savedMessage.getId().toString());

        chat.getParticipants().forEach(user -> {
            if (!user.getId().equals(senderUser.getId())) {
                redisService.incrementUnreadMessages(user.getId(), chat.getId());
            }
        });

        MessageResponseDto response = messageMapper.toDto(savedMessage);

        final Map<Long, Boolean> participantsOnline = chat.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toMap(userId -> userId, redisService::isUserOnline));

        outBoxEventRepository.save(outBoxEventFactory.messageSent(
                new MessageSentEvent(chat.getId(), response, participantsOnline)
        ));

        return response;
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
            throw new ForbiddenActionException("Sender can`t change message OutboxEventStatus!");
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

        log.warn("Invalid message status transition: messageId={}, from={}, to={}",
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

    private void markAsDeliveredInternal(Long messageId) {

        Message message = getMessageOrThrow(messageId);

        if (message.getStatus() != MessageStatus.SENT) {
            return;
        }

        message.setStatus(MessageStatus.DELIVERED);

        outBoxEventRepository.save(outBoxEventFactory.messageDelivered(
                        new MessageDeliveredEvent(
                                message.getChat().getId(),
                                message.getId(),
                                message.getSender().getId()
                        )
                )
        );
    }

}
