package com.merigaumata.auth.controller;

import com.merigaumata.auth.api.AuthenticationApi;
import com.merigaumata.auth.model.LoginRequest;
import com.merigaumata.auth.model.LoginResponse;
import com.merigaumata.auth.model.RefreshTokenRequest;
import com.merigaumata.auth.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor(onConstructor_ = @Autowired)
public class AuthController implements AuthenticationApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> logout(String authorization) {
        // TODO: Implement logout logic
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<LoginResponse> refresh(RefreshTokenRequest refreshTokenRequest) {
        // TODO: Implement refresh token logic
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
