package org.example.controller.redis;

import lombok.RequiredArgsConstructor;
import org.example.service.redis.RedisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/redis-test")
public class RedisTestController {

    private final RedisService redisService;

    @PostMapping
    public void save(@RequestParam String key, @RequestParam String value) {

        redisService.setValue(key, value);
    }

    @GetMapping
    public String get(@RequestParam String key) {

        return redisService.getValue(key);
    }
}
