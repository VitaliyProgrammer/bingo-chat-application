package org.example.service.impl;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.example.dto.request.ReactionRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.dto.response.ReactionSummaryDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.MessageReaction;
import org.example.entity.User;
import org.example.entity.status.MessageStatus;
import org.example.entity.type.ChatType;
import org.example.event.MessageDeliveredEvent;
import org.example.event.MessageEditedEvent;
import org.example.event.MessagePinnedEvent;
import org.example.event.MessageReactedEvent;
import org.example.event.MessageReadEvent;
import org.example.event.MessageRemindedEvent;
import org.example.event.MessageSentEvent;
import org.example.exception.BadRequestException;
import org.example.exception.ChatNotFoundException;
import org.example.exception.ForbiddenActionException;
import org.example.exception.MessageNotFoundException;
import org.example.mapper.MessageMapper;
import org.example.repository.ChatRepository;
import org.example.repository.MessageReactionRepository;
import org.example.repository.MessageRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.MessageService;
import org.example.service.RedisService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

    // GROUP chats only: any emoji is a valid reaction TYPE, but a single message can
    // only ever accumulate this many DISTINCT types (mirrors Telegram's reaction bar
    // under high-view posts) - past the cap, new reactors must join an existing type
    // rather than opening a new slot. Private/self chats have at most a couple of
    // participants, so this scale problem can't occur there and stay unrestricted.
    private static final int MAX_GROUP_REACTION_TYPES = 8;

    private final MessageRepository messageRepository;

    private final ChatRepository chatRepository;

    private final CurrentUserProvider currentUserProvider;

    private final MessageMapper messageMapper;

    private final RedisService redisService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OutBoxEventRepository outBoxEventRepository;

    private final OutBoxEventFactory outBoxEventFactory;

    private final ApplicationMetricsService metricsService;

    private final MessageReactionRepository messageReactionRepository;

    private final MessageSource messageSource;

    @Override
    @Transactional
    public void openChat(Long chatId) {

        User senderUser = currentUserProvider.getAuthenticatedUser();

        validateUserInChat(chatId, senderUser.getId());

        log.info("Opening chat: chatId={}, userId={}", chatId, senderUser.getId());

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

        MessageResponseDto response = messageMapper.toDto(message);

        outBoxEventRepository.save(outBoxEventFactory.messageEdited(
                new MessageEditedEvent(message.getChat().getId(), response)
        ));

        return response;
    }

    @Override
    @Transactional
    public MessageResponseDto markAsDelivered(Long messageId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateUserInChat(message.getChat().getId(), currentUser.getId());
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

        validateUserInChat(message.getChat().getId(), currentUser.getId());
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

        validateUserInChat(chatId, currentUser.getId());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage = messageRepository.findAllByChatId(chatId, pageable);

        List<Long> messageIds = messagePage.getContent().stream().map(Message::getId).toList();

        Map<Long, List<MessageReaction>> reactionsByMessageId = messageReactionRepository
                .findByMessageIdIn(messageIds).stream()
                .collect(Collectors.groupingBy(reaction -> reaction.getMessage().getId()));

        List<MessageResponseDto> messages = messagePage.getContent().stream()
                .map(message -> messageMapper.toDto(message,
                        buildReactionSummaries(reactionsByMessageId
                                .getOrDefault(message.getId(), List.of()))))
                .toList();

        return new MessagePageResponseDto(messages, messagePage.getNumber(), messagePage.getSize(),
                messagePage.getTotalElements(), messagePage.getTotalPages());
    }

    @Override
    @Transactional
    public void acknowledge(MessageAckRequestDto request, Principal principal) {

        User currentUser = currentUserProvider.getAuthenticatedUser(principal);

        Message message = getMessageOrThrow(request.messageId());

        validateUserInChat(message.getChat().getId(), currentUser.getId());
        validateNotSender(message, currentUser);

        String acknowledgeKey = "ack:" + request.messageId();

        redisService.setValue(acknowledgeKey, "1", Duration.ofMinutes(10));

        redisTemplate.opsForSet().remove("pending:messages", request.messageId().toString());

        log.info("ACK received: messageId={}, chatId={}, userId={}",
                message.getId(), message.getChat().getId(), currentUser.getId());

        markAsDeliveredInternal(message);
    }

    @Override
    @Transactional
    public MessageResponseDto pinMessage(Long messageId) {

        Message message = getMessageOrThrow(messageId);

        // Everyone in chat should be able to pin? Or only sender?
        // Usually, in group chats any participant can pin, but here we have private chats.
        Long currentUserId = currentUserProvider.getAuthenticatedUser().getId();
        validateUserInChat(message.getChat().getId(), currentUserId);

        message.setIsPinned(true);

        MessageResponseDto response = messageMapper.toDto(message);

        outBoxEventRepository.save(outBoxEventFactory.messagePinned(
                new MessagePinnedEvent(message.getChat().getId(), response)
        ));

        return response;
    }

    @Override
    @Transactional
    public MessageResponseDto setReminder(Long messageId, MessageReminderRequestDto request) {

        Message message = getMessageOrThrow(messageId);

        validateMessageOwner(message, currentUserProvider.getAuthenticatedUser());

        message.setReminderAt(request.reminderAt());

        outBoxEventRepository.save(outBoxEventFactory.messageReminded(new MessageRemindedEvent(
                message.getId(), currentUserProvider.getAuthenticatedUser().getId(),
                message.getChat().getId(), message.getContent(), request.reminderAt())));

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public MessageResponseDto addReaction(Long messageId, ReactionRequestDto request) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateUserInChat(message.getChat().getId(), currentUser.getId());

        if (message.getChat().getChatType() == ChatType.GROUP) {
            // Lock the message row FIRST so a concurrent addReaction on the same message
            // can't read the same pre-cap state and both slip past MAX_GROUP_REACTION_TYPES.
            // The second transaction blocks here until the first commits, then re-reads
            // fresh state - same technique as the chat-row lock in sendMessageInternal.
            messageRepository.lockMessageForUpdate(messageId);
            validateGroupReactionSlot(messageId, currentUser.getId(), request.emoji());
        }

        MessageReaction reaction = messageReactionRepository
                .findByMessageIdAndUserId(messageId, currentUser.getId())
                .orElseGet(MessageReaction::new);

        reaction.setMessage(message);
        reaction.setUser(currentUser);
        reaction.setEmoji(request.emoji());

        messageReactionRepository.save(reaction);

        log.info("Reaction added: messageId={}, userId={}, emoji={}",
                messageId, currentUser.getId(), request.emoji());

        return publishReactionEvent(message);
    }

    @Override
    @Transactional
    public MessageResponseDto removeReaction(Long messageId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateUserInChat(message.getChat().getId(), currentUser.getId());

        messageReactionRepository.deleteByMessageIdAndUserId(messageId, currentUser.getId());

        log.info("Reaction removed: messageId={}, userId={}", messageId, currentUser.getId());

        return publishReactionEvent(message);
    }

    private MessageResponseDto publishReactionEvent(Message message) {

        List<ReactionSummaryDto> reactions =
                buildReactionSummaries(messageReactionRepository.findByMessageId(message.getId()));

        MessageResponseDto response = messageMapper.toDto(message, reactions);

        outBoxEventRepository.save(outBoxEventFactory.messageReacted(
                new MessageReactedEvent(message.getChat().getId(), response)
        ));

        return response;
    }

    private List<ReactionSummaryDto> buildReactionSummaries(List<MessageReaction> reactions) {

        Map<String, List<Long>> userIdsByEmoji = reactions.stream()
                .collect(Collectors.groupingBy(
                        MessageReaction::getEmoji,
                        LinkedHashMap::new,
                        Collectors.mapping(reaction -> reaction.getUser().getId(),
                                Collectors.toList())
                ));

        return userIdsByEmoji.entrySet().stream()
                .map(entry -> new ReactionSummaryDto(entry.getKey(), entry.getValue().size(),
                        entry.getValue()))
                .toList();
    }

    private MessageResponseDto sendMessageInternal(MessageRequestDto request, User senderUser) {

        validateUserInChat(request.chatId(), senderUser.getId());

        // Step 1: Lock chat row FIRST (blocks other concurrent requests)
        chatRepository.lockChatForUpdate(request.chatId());

        Chat chat = chatRepository.findById(request.chatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat not found!"));

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

    private void validateUserInChat(Long chatId, Long userId) {

        if (!chatRepository.existsByIdAndParticipants_Id(chatId, userId)) {
            throw new ForbiddenActionException("You are not a participant of this chat!");
        }
    }

    private void validateGroupReactionSlot(Long messageId, Long currentUserId, String emoji) {

        List<MessageReaction> existingReactions =
                messageReactionRepository.findByMessageId(messageId);

        boolean emojiAlreadyUsedOnMessage = existingReactions.stream()
                .anyMatch(reaction -> reaction.getEmoji().equals(emoji));

        if (emojiAlreadyUsedOnMessage) {
            return;
        }

        // Brand-new type for this message - only counts against the cap if it would
        // add a slot. The caller's own current reaction (if any) is excluded, since
        // switching away from it frees that slot rather than consuming a new one.
        long distinctTypesFromOthers = existingReactions.stream()
                .filter(reaction -> !reaction.getUser().getId().equals(currentUserId))
                .map(MessageReaction::getEmoji)
                .distinct()
                .count();

        if (distinctTypesFromOthers >= MAX_GROUP_REACTION_TYPES) {
            throw new BadRequestException(messageSource.getMessage(
                    "reaction.limit.exceeded", null,
                    "Reaction limit for this message reached - join one of the "
                            + "existing reactions!",
                    LocaleContextHolder.getLocale()));
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

    private void markAsDeliveredInternal(Message message) {

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
