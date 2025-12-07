package com.merigaumata.auth.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public void blacklistToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();

            if (expiration != null && expiration.after(Date.from(Instant.now()))) {
                String key = BLACKLIST_PREFIX + claims.getJWTID();
                long ttl = expiration.getTime() - System.currentTimeMillis();

                redisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.MILLISECONDS);
                log.info("Token blacklisted: {}", claims.getJWTID());
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            String key = BLACKLIST_PREFIX + claims.getJWTID();

            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check blacklist", e);
            return false;
        }
    }
}