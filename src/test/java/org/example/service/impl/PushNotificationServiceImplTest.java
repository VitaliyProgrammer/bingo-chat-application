package org.example.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Security;
import java.util.List;
import java.util.Optional;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.example.dto.response.MessageResponseDto;
import org.example.entity.Chat;
import org.example.entity.PushSubscription;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.repository.ChatRepository;
import org.example.repository.PushSubscriptionRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

    private static final Long RECIPIENT_ID = 1L;
    private static final Long SENDER_ID = 2L;
    private static final Long CHAT_ID = 10L;

    @BeforeAll
    static void registerBouncyCastle() {
        // Notification's constructor parses EC keys via a "BC" KeyFactory lookup;
        // in production PushConfiguration registers it once at startup.
        Security.addProvider(new BouncyCastleProvider());
    }

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PushService pushService;

    @InjectMocks
    private PushNotificationServiceImpl pushNotificationService;

    @Test
    void sendToUser_noSubscriptions_doesNotCallPushService() throws Exception {

        when(pushSubscriptionRepository.findAllByUser_Id(RECIPIENT_ID))
                .thenReturn(List.of());

        pushNotificationService.sendToUser(RECIPIENT_ID, message());

        verify(pushService, never()).send(any(Notification.class), any(Encoding.class));
    }

    @Test
    void sendToUser_subscriptionActive_keepsSubscriptionOnSuccess() throws Exception {

        PushSubscription subscription = subscription();
        when(pushSubscriptionRepository.findAllByUser_Id(RECIPIENT_ID))
                .thenReturn(List.of(subscription));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender()));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"title\":\"John\"}");
        HttpResponse response = httpResponse(200);
        when(pushService.send(any(Notification.class), any(Encoding.class))).thenReturn(response);

        pushNotificationService.sendToUser(RECIPIENT_ID, message());

        verify(pushService, times(1)).send(any(Notification.class), any(Encoding.class));
        verify(pushSubscriptionRepository, never()).delete(any(PushSubscription.class));
    }

    @Test
    void sendToUser_subscriptionGone_deletesStaleSubscription() throws Exception {

        PushSubscription subscription = subscription();
        when(pushSubscriptionRepository.findAllByUser_Id(RECIPIENT_ID))
                .thenReturn(List.of(subscription));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender()));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"title\":\"John\"}");
        HttpResponse response = httpResponse(410);
        when(pushService.send(any(Notification.class), any(Encoding.class))).thenReturn(response);

        pushNotificationService.sendToUser(RECIPIENT_ID, message());

        verify(pushSubscriptionRepository).delete(subscription);
    }

    @Test
    void sendToUser_groupChat_usesChatNameInTitle() throws Exception {

        PushSubscription subscription = subscription();
        when(pushSubscriptionRepository.findAllByUser_Id(RECIPIENT_ID))
                .thenReturn(List.of(subscription));
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender()));
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(groupChat()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"title\":\"Team · John\"}");
        HttpResponse response = httpResponse(201);
        when(pushService.send(any(Notification.class), any(Encoding.class))).thenReturn(response);

        pushNotificationService.sendToUser(RECIPIENT_ID, message());

        verify(objectMapper).writeValueAsString(any());
    }

    private MessageResponseDto message() {
        return new MessageResponseDto(
                100L, SENDER_ID, false, CHAT_ID, "Hello there!",
                null, null, null, false, null, null, null
        );
    }

    private PushSubscription subscription() {
        // Valid base64url-encoded uncompressed EC (P-256) point + 16-byte auth secret,
        // required because Notification's constructor parses them as real crypto keys.
        PushSubscription subscription = new PushSubscription();
        subscription.setId(5L);
        subscription.setEndpoint("https://push.example.com/endpoint");
        subscription.setP256dhKey(
                "BNCKHLf7HsAaDn4DY6iRohZWhxKKFzu6R_cEmKLiK158"
                        + "VeRQOrKlGFBXZRXJtrUJIu_17kTkLK0MoSlePdkOWlU");
        subscription.setAuthKey("MXgYP2BOdd8G3ziy_6w76A");
        return subscription;
    }

    private User sender() {
        User user = new User();
        user.setId(SENDER_ID);
        user.setNickname("John");
        return user;
    }

    private Chat groupChat() {
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setChatType(ChatType.GROUP);
        chat.setName("Team");
        return chat;
    }

    private HttpResponse httpResponse(int statusCode) {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(statusCode);

        HttpResponse response = mock(HttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        return response;
    }
}
