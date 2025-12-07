package com.merigaumata.user.service;

import com.merigaumata.user.entity.User;
import com.merigaumata.user.exception.DuplicateResourceException;
import com.merigaumata.user.exception.ResourceNotFoundException;
import com.merigaumata.user.exception.ValidationException;
import com.merigaumata.user.mapper.AuditLogMapper;
import com.merigaumata.user.mapper.UserMapper;
import com.merigaumata.user.model.*;
import com.merigaumata.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;

import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditLogMapper auditLogMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Map DTO to Entity using MapStruct
        User user = userMapper.toEntity(request);
        user = userRepository.save(user);

        auditService.logAction(
                user.getId(),
                "USER_CREATED",
                "User",
                user.getId(),
                "User account created",
                null,
                null,
                true,
                null
        );

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String id, String requesterId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Authorization check
        if (!id.equals(requesterId) && !isAdmin(requesterId)) {
            throw new ForbiddenException("You do not have permission to access this resource");
        }

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userMapper.toResponsePage(userPage);
    }

    @Transactional
    public UserResponse updateUser(String id, UpdateUserRequest request, String requesterId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Authorization check
        if (!id.equals(requesterId) && !isAdmin(requesterId)) {
            throw new ForbiddenException("You do not have permission to modify this resource");
        }

        // Check email uniqueness if email is being changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
        }

        // Update entity using MapStruct (only non-null fields)
        userMapper.updateEntityFromRequest(request, user);
        user = userRepository.save(user);

        auditService.logAction(
                user.getId(),
                "USER_UPDATED",
                "User",
                user.getId(),
                "User profile updated",
                null,
                null,
                true,
                null
        );

        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        auditService.logAction(
                user.getId(),
                "USER_DELETED",
                "User",
                user.getId(),
                "User account deleted",
                null,
                null,
                true,
                null
        );

        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(String id, ChangePasswordRequest request, String requesterId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Authorization check
        if (!id.equals(requesterId)) {
            throw new ForbiddenException("You can only change your own password");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            auditService.logAction(
                    user.getId(),
                    "PASSWORD_CHANGE_FAILED",
                    "User",
                    user.getId(),
                    "Invalid current password",
                    null,
                    null,
                    false,
                    "Invalid current password"
            );
            throw new ValidationException("Current password is incorrect", null);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditService.logAction(
                user.getId(),
                "PASSWORD_CHANGED",
                "User",
                user.getId(),
                "Password changed successfully",
                null,
                null,
                true,
                null
        );
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getUserAuditLogs(String userId, String requesterId, Pageable pageable) {
        // Authorization check
        if (!userId.equals(requesterId) && !isAdmin(requesterId)) {
            throw new ForbiddenException("You do not have permission to view these audit logs");
        }

        return auditService.getUserAuditLogs(userId, pageable);
    }

    @Transactional
    public void updateLastLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(Instant.now());
            userRepository.save(user);
        });
    }

    private boolean isAdmin(String userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().contains("ROLE_ADMIN"))
                .orElse(false);
    }
}