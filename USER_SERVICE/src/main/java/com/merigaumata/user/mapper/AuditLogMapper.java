package com.merigaumata.user.mapper;

import com.merigaumata.user.entity.AuditLog;
import com.merigaumata.user.model.AuditLogResponse;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface AuditLogMapper {

    /**
     * Map AuditLog entity to AuditLogResponse DTO
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "action", target = "action")
    @Mapping(source = "entityType", target = "entityType")
    @Mapping(source = "entityId", target = "entityId")
    @Mapping(source = "details", target = "details")
    @Mapping(source = "timestamp", target = "timestamp")
    @Mapping(source = "success", target = "success")
    AuditLogResponse toResponse(AuditLog auditLog);

    /**
     * Map list of AuditLog entities to list of AuditLogResponse DTOs
     */
    List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs);

    /**
     * Map Page of AuditLog entities to Page of AuditLogResponse DTOs
     */
    default Page<AuditLogResponse> toResponsePage(Page<AuditLog> auditLogPage) {
        return auditLogPage.map(this::toResponse);
    }

    /**
     * Create AuditLog entity from parameters
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "entityType", source = "entityType")
    @Mapping(target = "entityId", source = "entityId")
    @Mapping(target = "details", source = "details")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "success", source = "success")
    @Mapping(target = "errorMessage", source = "errorMessage")
    AuditLog createAuditLog(
            String userId,
            String action,
            String entityType,
            String entityId,
            String details,
            String ipAddress,
            String userAgent,
            boolean success,
            String errorMessage
    );
}