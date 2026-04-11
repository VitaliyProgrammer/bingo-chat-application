package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.response.MessagePageResponseDto;
import org.example.dto.response.MessageResponseDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.User;
import org.example.entity.status.MessageStatus;
import org.example.exception.ChatNotFoundException;
import org.example.exception.ForbiddenActionException;
import org.example.exception.MessageNotFoundException;
import org.example.mapper.MessageMapper;
import org.example.repository.ChatRepository;
import org.example.repository.MessageRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    private final ChatRepository chatRepository;

    private final CurrentUserProvider currentUserProvider;

    private final MessageMapper messageMapper;

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

        return messageMapper.toDto(savedMessage);
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
        validateReceiver(message, currentUser);
        validateStatus(message, MessageStatus.SENT);

        message.setStatus(MessageStatus.DELIVERED);

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public MessageResponseDto markAsRead(Long messageId) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        Message message = getMessageOrThrow(messageId);

        validateUserInChat(message.getChat(), currentUser);
        validateReceiver(message, currentUser);
        validateStatus(message, MessageStatus.DELIVERED);

        message.setStatus(MessageStatus.READ);

        return messageMapper.toDto(message);
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

        if (!chat.getParticipants().contains(currentUser)) {
            throw new ForbiddenActionException("No access to this chat!");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage = messageRepository.findAllByChatId(chatId, pageable);

        return messageMapper.toPageDto(messagePage);
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

    private void validateReceiver(Message message, User user) {

        if (!message.getSender().getId().equals(user.getId())) {
            throw new ForbiddenActionException("Sender can`t change message status!");
        }
    }

    private void validateStatus(Message message, MessageStatus expectedStatus) {

        if (message.getStatus() != expectedStatus) {
            throw new IllegalStateException("Invalid status transition!");
        }
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
