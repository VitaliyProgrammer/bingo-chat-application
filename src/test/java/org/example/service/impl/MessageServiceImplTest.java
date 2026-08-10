package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.example.dto.request.MessageRequestDto;
import org.example.dto.request.ReactionRequestDto;
import org.example.entity.Chat;
import org.example.entity.Message;
import org.example.entity.MessageReaction;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.configuration.outbox.factory.OutBoxEventFactory;
import org.example.configuration.outbox.repository.OutBoxEventRepository;
import org.example.exception.BadRequestException;
import org.example.exception.ForbiddenActionException;
import org.example.mapper.MessageMapper;
import org.example.repository.BlockedGroupRepository;
import org.example.repository.BlockedUserRepository;
import org.example.repository.ChatRepository;
import org.example.repository.MessageReactionRepository;
import org.example.repository.MessageRepository;
import org.example.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

/**
 * Unit tests for the group-chat reaction type cap (MAX_GROUP_REACTION_TYPES = 8).
 * All collaborators are mocked - no Spring context, no database. This only proves
 * the counting logic is correct; it CANNOT prove the {@code FOR UPDATE} lock actually
 * prevents a race between two concurrent requests - that needs a real DB
 * (see the integration test with Testcontainers).
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    private static final Long MESSAGE_ID = 100L;
    private static final Long CHAT_ID = 10L;
    private static final Long CURRENT_USER_ID = 1L;
    private static final String LIMIT_MESSAGE = "Reaction limit for this message reached!";

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @Mock
    private MessageSource messageSource;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private OutBoxEventRepository outBoxEventRepository;

    @Mock
    private OutBoxEventFactory outBoxEventFactory;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private BlockedGroupRepository blockedGroupRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User currentUser;
    private Chat groupChat;
    private Message message;

    @BeforeEach
    void setUp() {
        currentUser = user(CURRENT_USER_ID);
        groupChat = chat(CHAT_ID, ChatType.GROUP);
        message = message(MESSAGE_ID, groupChat);

        when(currentUserProvider.getAuthenticatedUser()).thenReturn(currentUser);
        lenient().when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
        when(chatRepository.existsByIdAndParticipants_Id(CHAT_ID, CURRENT_USER_ID)).thenReturn(true);
    }

    @Test
    void addReaction_newTypeUnderCap_isAllowed() {

        // 7 distinct types already exist, all from other users - room for one more.
        when(messageReactionRepository.findByMessageId(MESSAGE_ID))
                .thenReturn(reactionsFromOtherUsers(7));
        when(messageReactionRepository.findByMessageIdAndUserId(MESSAGE_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

        messageService.addReaction(MESSAGE_ID, new ReactionRequestDto("🎉"));

        verify(messageReactionRepository, times(1)).save(argThatSavedEmojiIs("🎉"));
    }

    @Test
    void addReaction_newTypeAtCap_throwsBadRequestExceptionWithResolvedMessage() {

        // Cap already reached by 8 other users - a 9th distinct type must be rejected.
        when(messageReactionRepository.findByMessageId(MESSAGE_ID))
                .thenReturn(reactionsFromOtherUsers(8));
        when(messageSource.getMessage(
                eq("reaction.limit.exceeded"), isNull(), anyString(), any(Locale.class)))
                .thenReturn(LIMIT_MESSAGE);

        assertThatThrownBy(() ->
                messageService.addReaction(MESSAGE_ID, new ReactionRequestDto("🆕")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LIMIT_MESSAGE);

        verify(messageReactionRepository, never()).save(any());
    }

    @Test
    void addReaction_joiningExistingTypeAtCap_isAllowedDespiteCap() {

        // Cap reached (8 types), but the caller picks one that ALREADY exists -
        // no new slot is consumed, so this must succeed even though we're at the cap.
        List<MessageReaction> existing = reactionsFromOtherUsers(8);
        String existingEmoji = existing.get(0).getEmoji();

        when(messageReactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(existing);
        when(messageReactionRepository.findByMessageIdAndUserId(MESSAGE_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

        messageService.addReaction(MESSAGE_ID, new ReactionRequestDto(existingEmoji));

        verify(messageReactionRepository, times(1)).save(argThatSavedEmojiIs(existingEmoji));
    }

    @Test
    void addReaction_switchingOwnReactionAtCap_isAllowedBecauseOwnSlotIsExcluded() {

        // 7 OTHER distinct types + the caller's own current reaction ("old") = 8 rows,
        // but only 7 count against the cap once the caller's own row is excluded -
        // so switching to a brand-new emoji must be allowed.
        List<MessageReaction> reactions = new java.util.ArrayList<>(reactionsFromOtherUsers(7));
        reactions.add(reaction(user(999L), "old"));
        // caller's own previous reaction, stored under CURRENT_USER_ID:
        reactions.set(reactions.size() - 1, reaction(currentUser, "old"));

        when(messageReactionRepository.findByMessageId(MESSAGE_ID)).thenReturn(reactions);
        when(messageReactionRepository.findByMessageIdAndUserId(MESSAGE_ID, CURRENT_USER_ID))
                .thenReturn(Optional.of(reactions.get(reactions.size() - 1)));

        messageService.addReaction(MESSAGE_ID, new ReactionRequestDto("new"));

        verify(messageReactionRepository, times(1)).save(argThatSavedEmojiIs("new"));
    }

    @Test
    void addReaction_privateChat_skipsCapEntirely() {

        Chat privateChat = chat(CHAT_ID, ChatType.PRIVATE);
        Message privateMessage = message(MESSAGE_ID, privateChat);
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(privateMessage));
        when(messageReactionRepository.findByMessageIdAndUserId(MESSAGE_ID, CURRENT_USER_ID))
                .thenReturn(Optional.empty());

        messageService.addReaction(MESSAGE_ID, new ReactionRequestDto("👍"));

        // The cap only exists to protect GROUP chats from unbounded reaction sprawl -
        // private chats have at most a couple of participants, so the lock (the
        // group-only side effect) must never fire here. findByMessageId still gets
        // called once, but from publishReactionEvent building the response - not
        // from the (skipped) cap check.
        verify(messageRepository, never()).lockMessageForUpdate(MESSAGE_ID);
    }

    @Test
    void sendMessage_blockedGroup_throwsForbidden() {

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(groupChat));
        when(blockedGroupRepository.existsByChat_IdAndUnblockedAtIsNull(CHAT_ID)).thenReturn(true);
        when(messageSource.getMessage(
                eq("chat.blockedBySupport"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("This group has been blocked by support and can't be modified!");

        MessageRequestDto request = new MessageRequestDto(CHAT_ID, "Hello", null);

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(ForbiddenActionException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_privateChatBlockedPair_throwsForbidden() {

        Chat privateChat = chat(CHAT_ID, ChatType.PRIVATE);
        User otherUser = user(2L);
        privateChat.getParticipants().add(currentUser);
        privateChat.getParticipants().add(otherUser);

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(privateChat));
        when(blockedUserRepository.existsBlockBetween(CURRENT_USER_ID, 2L)).thenReturn(true);
        when(messageSource.getMessage(
                eq("user.chatBlocked"), isNull(), anyString(), any(Locale.class)))
                .thenReturn("You can't message this user - one of you has blocked the other!");

        MessageRequestDto request = new MessageRequestDto(CHAT_ID, "Hello", null);

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(ForbiddenActionException.class);

        verify(messageRepository, never()).save(any());
    }

    private List<MessageReaction> reactionsFromOtherUsers(int distinctTypesCount) {

        List<MessageReaction> reactions = new java.util.ArrayList<>();
        for (int i = 0; i < distinctTypesCount; i++) {
            reactions.add(reaction(user(1000L + i), "emoji-" + i));
        }
        return reactions;
    }

    private MessageReaction argThatSavedEmojiIs(String expectedEmoji) {
        return org.mockito.ArgumentMatchers.argThat(saved -> saved.getEmoji().equals(expectedEmoji));
    }

    private User user(Long id) {
        User result = new User();
        result.setId(id);
        return result;
    }

    private Chat chat(Long id, ChatType type) {
        Chat result = new Chat();
        result.setId(id);
        result.setChatType(type);
        return result;
    }

    private Message message(Long id, Chat chat) {
        Message result = new Message();
        result.setId(id);
        result.setChat(chat);
        return result;
    }

    private MessageReaction reaction(User reactingUser, String emoji) {
        MessageReaction result = new MessageReaction();
        result.setMessage(message);
        result.setUser(reactingUser);
        result.setEmoji(emoji);
        return result;
    }
}
