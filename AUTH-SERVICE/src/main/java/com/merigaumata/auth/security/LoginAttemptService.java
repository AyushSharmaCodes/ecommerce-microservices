package com.merigaumata.auth.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginAttemptService {

    @Value("${security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${security.lockout-duration:900}") // 15 minutes
    private int lockoutDuration;

    private final LoadingCache<String, Integer> attemptsCache;

    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void loginSucceeded(String key) {
        attemptsCache.invalidate(key);
    }

    public void loginFailed(String key) {
        int attempts = 0;
        try {
            attempts = attemptsCache.get(key);
        } catch (ExecutionException e) {
            attempts = 0;
        }
        attempts++;
        attemptsCache.put(key, attempts);

        if (attempts >= maxAttempts) {
            log.warn("Account locked due to too many failed attempts: {}", key);
        }
    }

    public boolean isBlocked(String key) {
        try {
            return attemptsCache.get(key) >= maxAttempts;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public int getAttemptsRemaining(String key) {
        try {
            return Math.max(0, maxAttempts - attemptsCache.get(key));
        } catch (ExecutionException e) {
            return maxAttempts;
        }
    }
}
