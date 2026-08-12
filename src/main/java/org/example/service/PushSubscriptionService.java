package org.example.service;

import org.example.dto.request.PushSubscriptionRequestDto;

public interface PushSubscriptionService {

    String getPublicKey();

    void subscribe(PushSubscriptionRequestDto request);

    void unsubscribe(String endpoint);
}
