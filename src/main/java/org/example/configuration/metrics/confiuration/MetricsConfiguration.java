package org.example.configuration.metrics.confiuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.configuration.metrics.model.MetricsNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    @Bean
    public Counter webSocketConnectCounter(MeterRegistry registry) {

        return Counter.builder(MetricsNames.WEBSOCKET_CONNECT).register(registry);
    }

    @Bean
    public Counter webSocketDisconnectCounter(MeterRegistry registry) {

        return Counter.builder(MetricsNames.WEBSOCKET_DISCONNECT).register(registry);
    }

    @Bean
    public Counter webSocketDeniedCounter(MeterRegistry registry) {

        return Counter.builder(MetricsNames.WEBSOCKET_DENIED).register(registry);
    }

    @Bean
    public Counter messageSentCounter(MeterRegistry registry) {

        return Counter.builder(MetricsNames.MESSAGE_SENT).register(registry);
    }

    @Bean
    public Counter outboxProcessedCounter(MeterRegistry registry) {

        return Counter.builder(MetricsNames.OUTBOX_PROCESSED).register(registry);
    }

    @Bean
    public Counter outboxFailedCounter(MeterRegistry registry) {

        return Counter.builder(MetricsNames.OUTBOX_FAILED).register(registry);
    }

    @Bean
    public Counter webSocketErrorCounter(MeterRegistry registry) {

        return Counter.builder(MetricsNames.WEB_SOCKET_ERROR).register(registry);
    }

    @Bean
    public Timer outboxLatencyTimer(MeterRegistry registry) {

        return Timer.builder(MetricsNames.OUTBOX_LATENCY)
                .publishPercentileHistogram()
                .register(registry);
    }
}
