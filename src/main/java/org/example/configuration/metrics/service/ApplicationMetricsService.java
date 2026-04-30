package org.example.configuration.metrics.service;

import io.micrometer.core.instrument.Timer;

public interface ApplicationMetricsService {

    void incrementWebSocketConnect();

    void incrementWebSocketDisconnect();

    void incrementLoginFailed(String reason);

    void incrementJwtTokenInvalid(String reason);

    void incrementWebSocketDenied();

    void incrementMessageSent();

    void incrementOutboxProcessed();

    void incrementOutboxFailed();

    void incrementWebSocketError();

    Timer.Sample startOutboxTimer();

    void stopOutboxTimer(Timer.Sample sample);
}
