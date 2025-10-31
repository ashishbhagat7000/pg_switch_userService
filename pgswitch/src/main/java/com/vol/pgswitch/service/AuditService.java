package com.vol.pgswitch.service;

import jakarta.persistence.EntityManager;
import com.vol.pgswitch.model.AuditLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void logUserCreated(Long userId, String username) {
        logAuditEvent("USER_CREATED", Map.of(
            "userId", userId,
            "username", username
        ));
    }

    @Transactional
    public void logUserUpdated(Long userId, String username) {
        logAuditEvent("USER_UPDATED", Map.of(
            "userId", userId,
            "username", username
        ));
    }

    @Transactional
    public void logUserDeleted(Long userId, String username) {
        logAuditEvent("USER_DELETED", Map.of(
            "userId", userId,
            "username", username
        ));
    }

    private void logAuditEvent(String eventType, Map<String, Object> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorId = auth != null ? auth.getName() : "system";

        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setEventType(eventType);
        auditLog.setActorId(actorId);
        auditLog.setTimestamp(LocalDateTime.now());

        try {
            String json = objectMapper.writeValueAsString(details != null ? details : Map.of());
            auditLog.setDetails(json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details: {}", e.getMessage(), e);
            auditLog.setDetails("{}");
        }

        entityManager.persist(auditLog);
        log.info("Audit log: {} by {} - {}", eventType, actorId, details);
    }
}