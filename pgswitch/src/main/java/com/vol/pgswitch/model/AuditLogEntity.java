package com.vol.pgswitch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Audit log stored in the primary relational database (Postgres).
 * Converted from a Mongo document to a JPA entity so it can be
 * persisted via JPA/EntityManager in the same DB as other entities.
 */
@Data
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String actorId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Details are stored as a serialized JSON string to keep schema flexible
     * while remaining compatible with relational storage. Marked as LOB to
     * allow larger payloads.
     */
    @Lob
    @Column(name = "details", columnDefinition = "text")
    private String details;
}