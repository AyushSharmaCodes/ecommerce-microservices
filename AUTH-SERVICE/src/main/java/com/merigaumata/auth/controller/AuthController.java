package com.merigaumata.auth.controller;

import com.merigaumata.auth.api.AuthenticationApi;
import com.merigaumata.auth.model.*;
import com.merigaumata.auth.security.LoginAttemptService;
import com.merigaumata.auth.service.AuthService;
import com.merigaumata.auth.service.RefreshTokenService;
import com.merigaumata.user.model.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    @Override
    public ResponseEntity<TokenIntrospectionResponse> introspect(TokenIntrospectionRequest tokenIntrospectionRequest) {
        TokenIntrospectionResponse response = authService.introspectToken(tokenIntrospectionRequest.getToken());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String xForwardedFor) {
        String clientIp = xForwardedFor != null ? xForwardedFor : "unknown";
        String username = loginRequest.getUsername();
        LoginResponse response = authService.authenticate(loginRequest);
        loginAttemptService.loginSucceeded(username);
        log.info("Successful login for user: {} from IP: {}", username, clientIp);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> logout(String authorization, LogoutRequest logoutRequest) {
        String accessToken = authorization.substring(7); // Remove "Bearer "
        authService.logout(accessToken, logoutRequest.getRefreshToken());
        log.info("User logged out successfully");
        return ResponseEntity.ok().body(new MessageResponse("Logged out successfully"));
    }


    @Override
    public ResponseEntity<LoginResponse> refresh(RefreshRequest refreshRequest) {
        LoginResponse response = refreshTokenService.refreshAccessToken(refreshRequest.getRefreshToken());
        log.info("Token refreshed successfully");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> register(RegisterRequest registerRequest) {
        UserResponse userResponse = authService.register(registerRequest);
        if (Objects.nonNull(userResponse)) {
            log.info("New user registered: {}", userResponse.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("User registered successfully"));
        }
        log.info("User registration Failed: {}", registerRequest.getUsername());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("User registration Failed"));
    }


//
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
//                                   @RequestHeader(value = "X-Forwarded-For", required = false) String ip) {
//        String clientIp = ip != null ? ip : "unknown";
//        String username = request.getUsername();
//
//        // Check if account is locked
//        if (loginAttemptService.isBlocked(username)) {
//            log.warn("Login attempt for locked account: {}", username);
//            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//                    .body(ErrorResponse.of("Account temporarily locked due to too many failed attempts"));
//        }
//
//        try {
//            LoginResponse response = authService.authenticate(request);
//            loginAttemptService.loginSucceeded(username);
//            log.info("Successful login for user: {} from IP: {}", username, clientIp);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            loginAttemptService.loginFailed(username);
//            int attemptsRemaining = loginAttemptService.getAttemptsRemaining(username);
//            log.warn("Failed login attempt for user: {} from IP: {}. Attempts remaining: {}",
//                    username, clientIp, attemptsRemaining);
//
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ErrorResponse.of("Invalid credentials. Attempts remaining: " + attemptsRemaining));
//        }
//    }
//
//    @PostMapping("/refresh")
//    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
//        try {
//            LoginResponse response = refreshTokenService.refreshAccessToken(request.getRefreshToken());
//            log.info("Token refreshed successfully");
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Token refresh failed: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ErrorResponse.of("Invalid or expired refresh token"));
//        }
//    }
//
//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest request,
//                                    @RequestHeader("Authorization") String authHeader) {
//        try {
//            String accessToken = authHeader.substring(7); // Remove "Bearer "
//            authService.logout(accessToken, request.getRefreshToken());
//            log.info("User logged out successfully");
//            return ResponseEntity.ok().body(new MessageResponse("Logged out successfully"));
//        } catch (Exception e) {
//            log.error("Logout failed: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(ErrorResponse.of("Logout failed"));
//        }
//    }
//
//    @PostMapping("/register")
//    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
//        try {
//            authService.register(request);
//            log.info("New user registered: {}", request.getUsername());
//            return ResponseEntity.status(HttpStatus.CREATED)
//                    .body(new MessageResponse("User registered successfully"));
//        } catch (Exception e) {
//            log.error("Registration failed: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(ErrorResponse.of(e.getMessage()));
//        }
//    }
//
//    @PostMapping("/introspect")
//    public ResponseEntity<TokenIntrospectionResponse> introspect(@Valid @RequestBody TokenIntrospectionRequest request) {
//        TokenIntrospectionResponse response = authService.introspectToken(request.getToken());
//        return ResponseEntity.ok(response);
//    }

}