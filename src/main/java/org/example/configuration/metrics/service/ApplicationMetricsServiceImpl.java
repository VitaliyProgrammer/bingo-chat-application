package org.example.configuration.metrics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.example.configuration.metrics.model.MetricsNames;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationMetricsServiceImpl implements ApplicationMetricsService {

    private final Counter webSocketConnectCounter;
    private final Counter webSocketDisconnectCounter;
    private final Counter webSocketDeniedCounter;
    private final Counter messageSentCounter;
    private final Counter outboxProcessedCounter;
    private final Counter outboxFailedCounter;

    private final Counter webSocketErrorCounter;

    private final Timer outboxLatencyTimer;
    private final MeterRegistry meterRegistry;

    @Override
    public void incrementWebSocketConnect() {

        webSocketConnectCounter.increment();
    }

    @Override
    public void incrementWebSocketDisconnect() {

        webSocketDisconnectCounter.increment();
    }

    @Override
    public void incrementLoginFailed(String reason) {

        meterRegistry.counter(MetricsNames.LOGIN_FAILED, "reason", reason).increment();
    }

    @Override
    public void incrementJwtTokenInvalid(String reason) {

        meterRegistry.counter(MetricsNames.JWT_TOKEN_INVALID, "reason", reason).increment();
    }

    @Override
    public void incrementWebSocketDenied() {

        webSocketDeniedCounter.increment();
    }

    @Override
    public void incrementMessageSent() {

        messageSentCounter.increment();
    }

    @Override
    public void incrementOutboxProcessed() {

        outboxProcessedCounter.increment();
    }

    @Override
    public void incrementOutboxFailed() {

        outboxFailedCounter.increment();
    }

    @Override
    public void incrementWebSocketError() {

        webSocketErrorCounter.increment();
    }

    @Override
    public void incrementRateLimitExceeded(String endpoint) {

        meterRegistry.counter(MetricsNames.RATE_LIMIT_EXCEEDED, "endpoint", endpoint).increment();
    }

    @Override
    public Timer.Sample startOutboxTimer() {

        return Timer.start(meterRegistry);
    }

    @Override
    public void stopOutboxTimer(Timer.Sample sample) {

        if (sample != null) {
            sample.stop(outboxLatencyTimer);
        }
    }
}
