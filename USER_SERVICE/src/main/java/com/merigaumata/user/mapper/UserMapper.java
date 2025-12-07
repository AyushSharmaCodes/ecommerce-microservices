package com.merigaumata.user.mapper;

import com.merigaumata.user.entity.User;
import com.merigaumata.user.model.*;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface UserMapper {

    /**
     * Map CreateUserRequest to User entity
     * Excludes: id, createdAt, updatedAt (auto-generated)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "accountLocked", constant = "false")
    @Mapping(target = "failedLoginAttempts", constant = "0")
    User toEntity(CreateUserRequest request);

    /**
     * Map User entity to UserResponse DTO
     * Excludes: password (sensitive)
     */
    UserResponse toResponse(User user);

    /**
     * Map list of User entities to list of UserResponse DTOs
     */
    List<UserResponse> toResponseList(List<User> users);

    /**
     * Map Page of User entities to Page of UserResponse DTOs
     */
    default Page<UserResponse> toResponsePage(Page<User> userPage) {
        return userPage.map(this::toResponse);
    }

    /**
     * Update User entity from UpdateUserRequest
     * Only updates non-null fields from the request
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true) // Username cannot be changed
    @Mapping(target = "password", ignore = true) // Password has separate endpoint
    @Mapping(target = "roles", ignore = true) // Roles have separate management
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "accountLocked", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);

    /**
     * Partial mapping for internal use (includes password)
     * Used by Auth Service for authentication
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "enabled", source = "enabled")
    UserAuthInfo toAuthInfo(User user);

    @Mapping(target = "totalElements", source = "totalElements")
    @Mapping(target = "numberOfElements", source = "numberOfElements")
    PageOfAuditLogResponse mapToPageResponse(Page<AuditLogResponse> page);

    @Mapping(target = "totalElements", source = "totalElements")
    @Mapping(target = "numberOfElements", source = "numberOfElements")
    PageOfUserResponse mapToUserResponse(Page<UserResponse> page);
}
