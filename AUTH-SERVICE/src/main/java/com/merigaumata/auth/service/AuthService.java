package com.merigaumata.auth.service;


import com.merigaumata.auth.entity.Role;
import com.merigaumata.auth.model.LoginRequest;
import com.merigaumata.auth.model.LoginResponse;
import com.merigaumata.auth.repository.RoleRepository;
import com.merigaumata.user.api.InternalUserApiApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for authentication operations
 * Handles user registration, login, and token validation
 */
@Service
@Transactional
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AuthService {

    private final RoleRepository roleRepository;

    private final InternalUserApiApi userService;

    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        // Call user service to authenticate user
        var userResponse = userService.authenticateUser(loginRequest);
        // Generate tokens (access and refresh)
        String accessToken = "generated-access-token"; // Replace with actual token generation logic
        String refreshToken = "generated-refresh-token"; // Replace with actual token generation logic

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600) // Token expiry time in seconds
                .build();
    }

    /**
     * Initialize default roles if they don't exist
     */
    @Transactional
    public void initializeRoles() {
        if (!roleRepository.existsByName("ROLE_USER")) {
            roleRepository.save(new Role("ROLE_USER"));
        }
        if (!roleRepository.existsByName("ROLE_ADMIN")) {
            roleRepository.save(new Role("ROLE_ADMIN"));
        }
    }
}
