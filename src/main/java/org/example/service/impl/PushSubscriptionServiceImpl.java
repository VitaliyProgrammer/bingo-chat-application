package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.PushSubscriptionRequestDto;
import org.example.entity.PushSubscription;
import org.example.entity.User;
import org.example.repository.PushSubscriptionRepository;
import org.example.security.CurrentUserProvider;
import org.example.service.PushSubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionServiceImpl implements PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    private final CurrentUserProvider currentUserProvider;

    @Value("${push.vapid.public-key}")
    private String publicKey;

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    @Override
    @Transactional
    public void subscribe(PushSubscriptionRequestDto request) {

        User currentUser = currentUserProvider.getAuthenticatedUser();

        PushSubscription subscription = pushSubscriptionRepository
                .findByEndpoint(request.endpoint())
                .orElseGet(PushSubscription::new);

        subscription.setUser(currentUser);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dhKey(request.keys().p256dh());
        subscription.setAuthKey(request.keys().auth());

        pushSubscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }
}
