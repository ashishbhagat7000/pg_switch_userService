package com.vol.pgswitch.service.crypto;

import com.vol.pgswitch.model.UserKeyEntity;
import com.vol.pgswitch.repository.UserKeyRepository;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class UserKeyService {

    private final UserKeyRepository userKeyRepository;
    private final KeyGenerationService keyGenerationService;
    private final CryptoService cryptoService;

    public UserKeyService(UserKeyRepository userKeyRepository,
                          KeyGenerationService keyGenerationService,
                          CryptoService cryptoService) {
        this.userKeyRepository = userKeyRepository;
        this.keyGenerationService = keyGenerationService;
        this.cryptoService = cryptoService;
    }

    public String generateAndStoreUserKey(String userId, int keySizeBits) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is required");
        if (!keyGenerationService.isValidKeySize(keySizeBits)) keySizeBits = keyGenerationService.getDefaultKeySize();

        String rawKeyBase64 = keyGenerationService.generateMasterKey(keySizeBits);
        String encryptedKey = cryptoService.encrypt(rawKeyBase64);

        UserKeyEntity entity = userKeyRepository.findByUserId(userId).orElse(new UserKeyEntity());
        entity.setUserId(userId);
        entity.setKeyEncrypted(encryptedKey);
        entity.setKeySizeBits(keySizeBits);
        long now = System.currentTimeMillis();
        if (entity.getCreatedAt() == 0) {
            entity.setCreatedAt(now);
            entity.setKeyVersion(1); // Initial version
        } else {
            entity.setRotatedAt(now);
            entity.setPreviousKeyVersion(entity.getKeyVersion());
            entity.setKeyVersion((entity.getKeyVersion() != null ? entity.getKeyVersion() : 1) + 1);
        }

        userKeyRepository.save(entity);
        return rawKeyBase64; // return plaintext once to caller; not stored
    }

    public String getUserKeyPlaintext(String userId) {
        UserKeyEntity entity = userKeyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("userId not found"));
        String decrypted = cryptoService.decrypt(entity.getKeyEncrypted());
        // optionally validate format
        if (!keyGenerationService.isValidKey(decrypted)) {
            throw new IllegalStateException("Stored user key is invalid");
        }
        return decrypted;
    }

    public void deleteUserKey(String userId) {
        userKeyRepository.deleteByUserId(userId);
    }

    public UserKeyEntity getUserKeyMetadata(String userId) {
        return userKeyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("userId not found"));
    }

    /**
     * Rewrap all user keys from previous master key to new master key.
     * Stores previous plaintext user key. Returns number of keys rewrapped.
     */
    public int rewrapAllUserKeys(String previousMasterKeyBase64, String newMasterKeyBase64) {
        var all = userKeyRepository.findAll();
        int count = 0;
        for (UserKeyEntity e : all) {
            // 1. Decrypt current user key using PREVIOUS master key material
            String plaintextUserKey = cryptoService.decryptWithKey(e.getKeyEncrypted(), previousMasterKeyBase64);
            
            // 2. Store old plaintext as requested (audit backup)
            e.setOldPlainUserKeyBase64(plaintextUserKey);
            
            // 3. Re-encrypt same plaintext under new master
            String rewrapped = cryptoService.encryptWithKey(plaintextUserKey, newMasterKeyBase64);
            e.setKeyEncrypted(rewrapped);
            
            // 4. Update version tracking
            e.setPreviousKeyVersion(e.getKeyVersion());
            e.setKeyVersion((e.getKeyVersion() != null ? e.getKeyVersion() : 1) + 1);
            
            // 5. Mark when this was done
            e.setLastRewrappedAt(System.currentTimeMillis());
            
            userKeyRepository.save(e);
            count++;
        }
        return count;
    }

    /**
     * Rewrap all user keys using KMS (AWS handles master key rotation automatically).
     * For KMS-based systems, we just re-encrypt user keys with current KMS key.
     */
    public int rewrapAllUserKeysWithKMS() {
        var all = userKeyRepository.findAll();
        int count = 0;
        for (UserKeyEntity e : all) {
            // 1. Decrypt current user key using current KMS key
            String plaintextUserKey = cryptoService.decrypt(e.getKeyEncrypted());
            
            // 2. Store old plaintext as requested (audit backup)
            e.setOldPlainUserKeyBase64(plaintextUserKey);
            
            // 3. Re-encrypt same plaintext with current KMS key (AWS handles rotation)
            String rewrapped = cryptoService.encrypt(plaintextUserKey);
            e.setKeyEncrypted(rewrapped);
            
            // 4. Update version tracking
            e.setPreviousKeyVersion(e.getKeyVersion());
            e.setKeyVersion((e.getKeyVersion() != null ? e.getKeyVersion() : 1) + 1);
            
            // 5. Mark when this was done
            e.setLastRewrappedAt(System.currentTimeMillis());
            
            userKeyRepository.save(e);
            count++;
        }
        return count;
    }
}


