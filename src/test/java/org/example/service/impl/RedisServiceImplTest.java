package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

    private static final String KEY = "rate-limit:login:127.0.0.1";
    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private RedisServiceImpl redisService;

    @Test
    void isAllowed_underLimit_returnsTrue() {
        redisService = new RedisServiceImpl(redisTemplate);
        stubScriptResult(3L);

        boolean allowed = redisService.isAllowed(KEY, MAX_REQUESTS, WINDOW);

        assertThat(allowed).isTrue();
    }

    @Test
    void isAllowed_exactlyAtLimit_returnsTrue() {
        redisService = new RedisServiceImpl(redisTemplate);
        stubScriptResult((long) MAX_REQUESTS);

        boolean allowed = redisService.isAllowed(KEY, MAX_REQUESTS, WINDOW);

        assertThat(allowed).isTrue();
    }

    @Test
    void isAllowed_overLimit_returnsFalse() {
        redisService = new RedisServiceImpl(redisTemplate);
        stubScriptResult(MAX_REQUESTS + 1L);

        boolean allowed = redisService.isAllowed(KEY, MAX_REQUESTS, WINDOW);

        assertThat(allowed).isFalse();
    }

    @Test
    void isAllowed_incrementAndExpire_executedAsSingleAtomicScript() {
        redisService = new RedisServiceImpl(redisTemplate);
        stubScriptResult(1L);

        redisService.isAllowed(KEY, MAX_REQUESTS, WINDOW);

        verifyScriptCalledOnceWithKeyAndWindow();
    }

    private void stubScriptResult(Long result) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(result);
    }

    private void verifyScriptCalledOnceWithKeyAndWindow() {
        verify(redisTemplate)
                .execute(any(RedisScript.class), eq(List.of(KEY)), eq(String.valueOf(WINDOW.getSeconds())));
    }
}
