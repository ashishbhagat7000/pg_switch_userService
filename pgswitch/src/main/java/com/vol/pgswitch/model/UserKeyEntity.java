package com.vol.pgswitch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * UserKeyEntity - Stores an application-level encryption key per user.
 * The key material is stored encrypted-at-rest using the platform master key.
 */
@Entity
@Table(name = "user_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Changed from String to Long for relational database best practices

    // Indexed and unique field for efficient lookups by user ID
    @Column(name = "user_id", nullable = false, unique = true, length = 255)
    private String userId;

    // Base64 key encrypted using CryptoService (AES-GCM) with master key
    // Length is set high to accommodate encrypted, base64-encoded key material
    @Column(name = "key_encrypted", nullable = false, length = 512)
    private String keyEncrypted;

    @Column(name = "key_size_bits", nullable = false)
    private int keySizeBits;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "rotated_at")
    private Long rotatedAt;

    // Stores previous decrypted user key (Base64) for audit/backup per request
    // Stored as CLOB/TEXT if supported by dialect, or just long VARCHAR.
    @Column(name = "old_plain_user_key_base64", length = 1024)
    private String oldPlainUserKeyBase64;

    // Timestamp when the key was last rewrapped under a new master key
    @Column(name = "last_rewrapped_at")
    private Long lastRewrappedAt;

    // Version number for key rotation tracking
    @Column(name = "key_version", nullable = false)
    private Integer keyVersion;

    // Previous key version (for audit trail)
    @Column(name = "previous_key_version")
    private Integer previousKeyVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getKeyEncrypted() { return keyEncrypted; }
    public void setKeyEncrypted(String keyEncrypted) { this.keyEncrypted = keyEncrypted; }
    public int getKeySizeBits() { return keySizeBits; }
    public void setKeySizeBits(int keySizeBits) { this.keySizeBits = keySizeBits; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public Long getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(Long rotatedAt) { this.rotatedAt = rotatedAt; }
    public String getOldPlainUserKeyBase64() { return oldPlainUserKeyBase64; }
    public void setOldPlainUserKeyBase64(String oldPlainUserKeyBase64) { this.oldPlainUserKeyBase64 = oldPlainUserKeyBase64; }
    public Long getLastRewrappedAt() { return lastRewrappedAt; }
    public void setLastRewrappedAt(Long lastRewrappedAt) { this.lastRewrappedAt = lastRewrappedAt; }
    public Integer getKeyVersion() { return keyVersion; }
    public void setKeyVersion(Integer keyVersion) { this.keyVersion = keyVersion; }
    public Integer getPreviousKeyVersion() { return previousKeyVersion; }
    public void setPreviousKeyVersion(Integer previousKeyVersion) { this.previousKeyVersion = previousKeyVersion; }
}


