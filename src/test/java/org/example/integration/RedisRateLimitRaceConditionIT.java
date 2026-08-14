package org.example.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.example.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

class RedisRateLimitRaceConditionIT extends AbstractIntegrationTest {

    private static final int THREAD_COUNT = 30;
    private static final int MAX_REQUESTS = 10;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void isAllowed_firstCall_setsExpiryInTheSameAtomicStep() {
        String key = "rate-limit-it:" + UUID.randomUUID();

        boolean allowed = redisService.isAllowed(key, MAX_REQUESTS, Duration.ofSeconds(30));

        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        assertThat(allowed).isTrue();
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isGreaterThan(0L).isLessThanOrEqualTo(30L);
    }

    @Test
    void isAllowed_concurrentRequests_neverAllowsMoreThanTheLimit() throws Exception {
        String key = "rate-limit-it:" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger allowedCount = new AtomicInteger();

        List<Future<?>> futures = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> executor.submit(() -> {
                    awaitUninterruptibly(startGate);
                    if (redisService.isAllowed(key, MAX_REQUESTS, Duration.ofSeconds(30))) {
                        allowedCount.incrementAndGet();
                    }
                }))
                .collect(Collectors.toList());

        startGate.countDown();

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertThat(allowedCount.get()).isEqualTo(MAX_REQUESTS);
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
