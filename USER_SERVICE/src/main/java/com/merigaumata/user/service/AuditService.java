package com.merigaumata.user.service;

import com.merigaumata.user.entity.AuditLog;
import com.merigaumata.user.mapper.AuditLogMapper;
import com.merigaumata.user.model.AuditLogResponse;
import com.merigaumata.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Async
    @Transactional
    public void logAction(String userId, String action, String entityType, String entityId,
                          String details, String ipAddress, String userAgent,
                          boolean success, String errorMessage) {
        try {
            AuditLog auditLog = auditLogMapper.createAuditLog(
                    userId, action, entityType, entityId, details,
                    ipAddress, userAgent, success, errorMessage
            );

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} - {}", action, userId);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getUserAuditLogs(String userId, Pageable pageable) {
        Page<AuditLog> auditLogPage = auditLogRepository.findByUserId(userId, pageable);
        return auditLogMapper.toResponsePage(auditLogPage);
    }
}