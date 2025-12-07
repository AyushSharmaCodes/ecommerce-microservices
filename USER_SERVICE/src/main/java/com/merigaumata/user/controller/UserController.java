package com.merigaumata.user.controller;

import static com.merigaumata.user.util.CreatePageable.createPageable;

import com.merigaumata.user.api.UsersApi;
import com.merigaumata.user.mapper.UserMapper;
import com.merigaumata.user.model.*;
import com.merigaumata.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

    private final UserService userService;
    private final UserMapper userMapper;

    @Override
    public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
        UserResponse response = userService.createUser(createUserRequest);
        log.info("User created: {}", response.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteUser(String id) {
        userService.deleteUser(id);
        log.info("User deleted: {}", id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<PageOfUserResponse> getAllUsers(Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponse> users = userService.getAllUsers(pageable);
        PageOfUserResponse userResponse = userMapper.mapToUserResponse(users);
        return ResponseEntity.ok(userResponse);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<PageOfAuditLogResponse> getUserAuditLogs(String id, String requesterId, Integer page, Integer size, List<String> sort) {
        Pageable pageable = createPageable(page, size, sort);
        Page<AuditLogResponse> logs = userService.getUserAuditLogs(id, requesterId, pageable);
        PageOfAuditLogResponse response = userMapper.mapToPageResponse(logs);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UserResponse> getUserById(String id, String xUserId) {
        // Users can only access their own data unless they're admins
        UserResponse response = userService.getUserById(id, xUserId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserResponse> getUserByUsername(String username) {
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UserResponse> updateUser(String id, String requesterId, UpdateUserRequest updateUserRequest) {
        UserResponse response = userService.updateUser(id, updateUserRequest, requesterId);
        log.info("User updated: {}", id);
        return ResponseEntity.ok(response);
    }
}
