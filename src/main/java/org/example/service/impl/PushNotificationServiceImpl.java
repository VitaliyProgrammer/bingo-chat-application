package org.example.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.example.dto.response.MessageResponseDto;
import org.example.entity.Chat;
import org.example.entity.PushSubscription;
import org.example.entity.User;
import org.example.entity.type.ChatType;
import org.example.repository.ChatRepository;
import org.example.repository.PushSubscriptionRepository;
import org.example.repository.UserRepository;
import org.example.service.PushNotificationService;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final int BODY_MAX_LENGTH = 120;

    private static final int STATUS_NOT_FOUND = 404;

    private static final int STATUS_GONE = 410;

    private final PushSubscriptionRepository pushSubscriptionRepository;

    private final UserRepository userRepository;

    private final ChatRepository chatRepository;

    private final ObjectMapper objectMapper;

    private final Supplier<PushService> pushServiceSupplier;

    @Override
    public void sendToUser(Long userId, MessageResponseDto message) {

        List<PushSubscription> subscriptions =
                pushSubscriptionRepository.findAllByUser_Id(userId);

        if (subscriptions.isEmpty()) {
            return;
        }

        String payload = buildPayload(message);

        subscriptions.forEach(subscription -> send(subscription, payload));
    }

    private String buildPayload(MessageResponseDto message) {
        try {
            Map<String, Object> payload = Map.of(
                    "title", resolveTitle(message),
                    "body", truncateContent(message.content()),
                    "chatId", message.chatId()
            );
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to build push payload!", exception);
        }
    }

    private String resolveTitle(MessageResponseDto message) {

        String senderNickname = userRepository.findById(message.senderId())
                .map(User::getNickname)
                .orElse("Bingo Chat");

        return chatRepository.findById(message.chatId())
                .filter(chat -> chat.getChatType() == ChatType.GROUP)
                .map(Chat::getName)
                .map(chatName -> chatName + " · " + senderNickname)
                .orElse(senderNickname);
    }

    private String truncateContent(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > BODY_MAX_LENGTH
                ? content.substring(0, BODY_MAX_LENGTH) + "…"
                : content;
    }

    private void send(PushSubscription subscription, String payload) {
        try {
            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dhKey(),
                    subscription.getAuthKey(),
                    payload
            );

            HttpResponse response = pushServiceSupplier.get()
                    .send(notification, Encoding.AES128GCM);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == STATUS_NOT_FOUND || statusCode == STATUS_GONE) {
                pushSubscriptionRepository.delete(subscription);
                log.debug("Removed stale push subscription: id={}", subscription.getId());
            } else if (statusCode >= 300) {
                log.warn("Push delivery failed: subscriptionId={}, status={}, body={}",
                        subscription.getId(), statusCode, readBody(response));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Push delivery interrupted: subscriptionId={}", subscription.getId(),
                    exception);
        } catch (GeneralSecurityException | IOException | JoseException
                 | ExecutionException exception) {
            log.error("Push delivery error: subscriptionId={}", subscription.getId(), exception);
        }
    }

    private String readBody(HttpResponse response) {
        try {
            return response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
        } catch (IOException exception) {
            return "<unreadable>";
        }
    }
}
