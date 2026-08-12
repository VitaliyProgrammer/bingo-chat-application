package org.example.service;

import org.example.dto.response.MessageResponseDto;

public interface PushNotificationService {

    void sendToUser(Long userId, MessageResponseDto message);
}
