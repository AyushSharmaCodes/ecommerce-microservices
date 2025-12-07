package com.merigaumata.auth.service;

import com.merigaumata.auth.entity.RefreshToken;
import com.merigaumata.auth.model.LoginResponse;
import com.merigaumata.auth.repository.RefreshTokenRepository;
import com.merigaumata.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createRefreshToken(String userId, String token) {
        // Hash the refresh token before storing
        String hashedToken = passwordEncoder.encode(token);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hashedToken);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenValidity()));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public LoginResponse refreshAccessToken(String refreshToken) {
        // Find all non-revoked tokens for verification
        List<RefreshToken> tokens = refreshTokenRepository.findByRevokedFalse();

        RefreshToken validToken = null;
        for (RefreshToken token : tokens) {
            if (passwordEncoder.matches(refreshToken, token.getTokenHash())) {
                validToken = token;
                break;
            }
        }

        if (validToken == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (validToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        // Rotate refresh token (one-time use)
        revokeRefreshToken(validToken.getId());

        String userId = validToken.getUserId();

        // Generate new tokens
        List<String> roles = List.of("ROLE_USER"); // Fetch from user service in production
        List<String> scopes = List.of("read", "write");

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, roles, scopes, "api-gateway");
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Store new refresh token
        createRefreshToken(userId, newRefreshToken);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .userId(userId)
                .roles(roles)
                .scopes(scopes)
                .build();
    }

    @Transactional
    public void revokeRefreshToken(String tokenOrId) {
        refreshTokenRepository.findById(tokenOrId)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        });
        refreshTokenRepository.saveAll(tokens);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Cleaned up expired refresh tokens");
    }
}