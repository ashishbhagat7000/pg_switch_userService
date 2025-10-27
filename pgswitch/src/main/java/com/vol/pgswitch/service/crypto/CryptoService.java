package com.vol.pgswitch.service.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.core.SdkBytes;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CryptoService - AES-GCM encryption service using Java Cryptography Extension (JCE)
 * 
 * This service provides AES-256-GCM encryption and decryption using Java's built-in
 * JCE (Java Cryptography Extension) instead of external libraries like Bouncy Castle.
 * This approach offers better performance, reduced memory footprint, and leverages
 * Java's mature cryptographic infrastructure.
 * 
 * BENEFITS OF JCE:
 * - No external dependencies → lighter memory footprint
 * - Mature and widely used in enterprise applications
 * - Built-in optimization and hardware acceleration support
 * - Works perfectly with Spring Boot for field-level encryption
 * - Native integration with Java security providers
 * 
 * SECURITY FEATURES:
 * - AES-256-GCM encryption (256-bit key, 96-bit IV)
 * - Authenticated encryption with additional data (AEAD)
 * - Cryptographically secure random number generation
 * - Base64 encoding for safe storage and transmission
 * - Protection against padding oracle attacks
 * 
 * ENCRYPTED FIELDS:
 * - PAN numbers (businessPanEncrypted)
 * - GSTIN numbers (gstinEncrypted) 
 * - CIN numbers (cinEncrypted)
 * - Government ID numbers (signatoryGovtIdNumberEncrypted)
 * - Bank account numbers (accountNumberEncrypted)
 * 
 * COMPLIANCE:
 * - PCI-DSS compliant encryption standards
 * - FIPS 140-2 approved algorithms
 * - NIST SP 800-38D compliant GCM mode
 * - Industry-standard key management practices
 * 
 * USAGE:
 * - Inject this service into other components
 * - Call encrypt() for sensitive data before database storage
 * - Call decrypt() when retrieving sensitive data for processing
 * - Master key should be rotated regularly (recommended: every 90 days)
 */
@Service
public class CryptoService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 256; // 256 bits

    private final String masterKeyBase64;
    private final String kmsKeyId;
    private final KmsClient kmsClient;

    /**
     * Constructor - Initializes crypto service with master key or KMS
     * 
     * @param masterKeyBase64 Base64 encoded master key from environment (fallback)
     * @param kmsKeyId AWS KMS key ID (primary)
     * @param kmsClient AWS KMS client
     */
    public CryptoService(@Value("${app.crypto.master-key-base64:}") String masterKeyBase64,
                        @Value("${app.crypto.kms-key-id:}") String kmsKeyId,
                        @org.springframework.beans.factory.annotation.Autowired(required = false) KmsClient kmsClient) {
        this.masterKeyBase64 = masterKeyBase64;
        this.kmsKeyId = kmsKeyId;
        this.kmsClient = kmsClient;
        validateConfiguration();
    }

    /**
     * Encrypts sensitive data using AES-256-GCM
     * 
     * This method encrypts plaintext data using AES-256-GCM encryption with
     * authenticated encryption. The IV is randomly generated for each encryption
     * operation to ensure uniqueness and security.
     * 
     * @param plaintext The sensitive data to encrypt
     * @return Base64 encoded encrypted data (IV + ciphertext + auth tag)
     * @throws RuntimeException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return plaintext;
        }

        try {
            if (kmsKeyId != null && !kmsKeyId.trim().isEmpty() && kmsClient != null) {
                // Use AWS KMS for encryption
                return encryptWithKMS(plaintext);
            } else {
                // Fallback to local master key
                return encryptWithLocalKey(plaintext);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts encrypted data using AES-256-GCM
     * 
     * This method decrypts Base64 encoded encrypted data back to plaintext.
     * It extracts the IV from the encrypted data and uses it for decryption
     * with authentication verification.
     * 
     * @param encryptedData Base64 encoded encrypted data
     * @return Decrypted plaintext data
     * @throws RuntimeException if decryption fails or authentication fails
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            return encryptedData;
        }

        try {
            if (kmsKeyId != null && !kmsKeyId.trim().isEmpty() && kmsClient != null) {
                // Use AWS KMS for decryption
                return decryptWithKMS(encryptedData);
            } else {
                // Fallback to local master key
                return decryptWithLocalKey(encryptedData);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generates a cryptographically secure random IV
     * 
     * @return Random IV bytes
     */
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Gets the master key from Base64 encoded string
     * 
     * @return SecretKey for AES operations
     * @throws RuntimeException if key is invalid
     */
    private SecretKey getMasterKey() {
        if (masterKeyBase64 == null || masterKeyBase64.trim().isEmpty()) {
            throw new RuntimeException("Master key not configured. Set APP_CRYPTO_MASTER_KEY environment variable.");
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Invalid master key format", e);
        }
    }

    /**
     * Encrypts plaintext using a provided Base64-encoded AES-256 key
     */
    public String encryptWithKey(String plaintext, String keyBase64) {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return plaintext;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            byte[] iv = generateIV();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Encryption with provided key failed", e);
        }
    }

    /**
     * Decrypts ciphertext using a provided Base64-encoded AES-256 key
     */
    public String decryptWithKey(String encryptedData, String keyBase64) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            return encryptedData;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedBytes.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption with provided key failed", e);
        }
    }

    /**
     * Validates the master key configuration
     * 
     * @throws RuntimeException if key is not properly configured
     */
    /**
     * Encrypt using AWS KMS
     */
    private String encryptWithKMS(String plaintext) {
        try {
            EncryptRequest encryptRequest = EncryptRequest.builder()
                    .keyId(kmsKeyId)
                    .plaintext(SdkBytes.fromString(plaintext, StandardCharsets.UTF_8))
                    .build();
            
            EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);
            return Base64.getEncoder().encodeToString(encryptResponse.ciphertextBlob().asByteArray());
        } catch (Exception e) {
            throw new RuntimeException("KMS encryption failed", e);
        }
    }

    /**
     * Decrypt using AWS KMS
     */
    private String decryptWithKMS(String encryptedData) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            DecryptRequest decryptRequest = DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteArray(encryptedBytes))
                    .build();
            
            DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);
            return decryptResponse.plaintext().asString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("KMS decryption failed", e);
        }
    }

    /**
     * Encrypt using local master key (fallback)
     */
    private String encryptWithLocalKey(String plaintext) {
        try {
            SecretKey secretKey = getMasterKey();
            byte[] iv = generateIV();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Local encryption failed", e);
        }
    }

    /**
     * Decrypt using local master key (fallback)
     */
    private String decryptWithLocalKey(String encryptedData) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedBytes.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            SecretKey secretKey = getMasterKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Local decryption failed", e);
        }
    }

    private void validateConfiguration() {
        if (masterKeyBase64 == null || masterKeyBase64.trim().isEmpty()) {
            if (kmsKeyId == null || kmsKeyId.trim().isEmpty()) {
                throw new RuntimeException("Neither master key nor KMS key configured. Set app.crypto.master-key-base64 or app.crypto.kms-key-id.");
            }
        }
        
        if (masterKeyBase64 != null && !masterKeyBase64.trim().isEmpty()) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
                if (keyBytes.length != KEY_LENGTH / 8) {
                    throw new RuntimeException("Master key must be " + KEY_LENGTH + " bits (" + (KEY_LENGTH / 8) + " bytes)");
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid master key format: " + e.getMessage(), e);
            }
        }
        
        // Validate KMS configuration if KMS is enabled
        if (kmsKeyId != null && !kmsKeyId.trim().isEmpty() && kmsClient == null) {
            throw new RuntimeException("KMS key ID configured but KmsClient not available. Check AWS credentials and region configuration.");
        }
    }

    /**
     * Generates a new AES-256 key for testing or key rotation
     * 
     * @return Base64 encoded AES-256 key
     */
    public static String generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Validates if a key is properly formatted
     * 
     * @param keyBase64 Base64 encoded key to validate
     * @return true if key is valid, false otherwise
     */
    public static boolean isValidKey(String keyBase64) {
        if (keyBase64 == null || keyBase64.trim().isEmpty()) {
            return false;
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            return keyBytes.length == KEY_LENGTH / 8;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isKmsEnabled() {
        return kmsKeyId != null && !kmsKeyId.trim().isEmpty() && kmsClient != null;
    }
}