package com.merigaumata.auth.service;

import com.merigaumata.auth.entity.Role;
import com.merigaumata.auth.mapper.AuthMapper;
import com.merigaumata.auth.model.LoginRequest;
import com.merigaumata.auth.model.LoginResponse;
import com.merigaumata.auth.model.RegisterRequest;
import com.merigaumata.auth.model.TokenIntrospectionResponse;
import com.merigaumata.auth.security.JwtTokenProvider;
import com.merigaumata.auth.security.PasswordPolicy;
import com.merigaumata.user.api.UsersApi;
import com.merigaumata.user.model.UserResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    private final UsersApi usersApi;
    private final AuthMapper authMapper;

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String userId = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        List<String> scopes = List.of("read", "write"); // Define based on your business logic

        String accessToken = jwtTokenProvider.generateAccessToken(userId, roles, scopes, "api-gateway");
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Store refresh token
        refreshTokenService.createRefreshToken(userId, refreshToken);

        return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .userId(userId)
                .roles(roles)
                .scopes(scopes)
                .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Validate password policy
        PasswordPolicy.ValidationResult validationResult = passwordPolicy.validate(request.getPassword());
        if (!validationResult.isValid()) {
            throw new RuntimeException(validationResult.getMessage());
        }
        // Call UserService to create user
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        return usersApi.createUser(authMapper.maptoCreateUserRequest(request)).block();

    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Blacklist access token
        tokenBlacklistService.blacklistToken(accessToken);
        // Revoke refresh token
        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    public TokenIntrospectionResponse introspectToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check if token is blacklisted
            if (tokenBlacklistService.isBlacklisted(token)) {
                return new TokenIntrospectionResponse(false, null, null, null, null, null);
            }

            // Check expiration
            if (claims.getExpirationTime().before(new java.util.Date())) {
                return new TokenIntrospectionResponse(false, null, null, null, null, null);
            }

            return TokenIntrospectionResponse.builder()
                    .active(true)
                    .subject(claims.getSubject())
                    .issuer(claims.getIssuer())
                    .expiration(claims.getExpirationTime().getTime() / 1000)
                    .roles(claims.getStringListClaim("roles"))
                    .scopes(claims.getStringListClaim("scopes"))
                    .build();
        } catch (Exception e) {
            log.error("Token introspection failed", e);
            return new TokenIntrospectionResponse(false, null, null, null, null, null);
        }
    }
}