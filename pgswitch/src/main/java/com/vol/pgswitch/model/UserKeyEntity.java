package com.vol.pgswitch.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * UserKeyEntity - Stores an application-level encryption key per user.
 * The key material is stored encrypted-at-rest using the platform master key.
 */
@Document(collection = "user_keys")
public class UserKeyEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    // Base64 key encrypted using CryptoService (AES-GCM) with master key
    @Field("key_encrypted")
    private String keyEncrypted;

    @Field("key_size_bits")
    private int keySizeBits;

    @Field("created_at")
    private long createdAt;

    @Field("rotated_at")
    private Long rotatedAt;

    // Stores previous decrypted user key (Base64) for audit/backup per request
    @Field("old_plain_user_key_base64")
    private String oldPlainUserKeyBase64;

    // Timestamp when the key was last rewrapped under a new master key
    @Field("last_rewrapped_at")
    private Long lastRewrappedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
}


