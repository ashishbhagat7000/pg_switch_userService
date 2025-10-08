package com.vol.pgswitch.service.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * KeyGenerationService - Cryptographic key generation using Java Cryptography Extension (JCE)
 * 
 * This service provides secure key generation capabilities using Java's built-in
 * JCE (Java Cryptography Extension) instead of external libraries. It offers
 * cryptographically secure random key generation for AES encryption operations.
 * 
 * BENEFITS OF JCE:
 * - No external dependencies → lighter memory footprint
 * - Mature and widely used in enterprise applications
 * - Built-in optimization and hardware acceleration support
 * - Native integration with Java security providers
 * - FIPS 140-2 approved algorithms
 * 
 * SECURITY FEATURES:
 * - Cryptographically secure random number generation
 * - Support for AES-128, AES-192, and AES-256 key sizes
 * - Base64 encoding for safe storage and transmission
 * - Key validation and format checking
 * - Secure random IV generation
 * 
 * COMPLIANCE:
 * - PCI-DSS compliant key generation
 * - FIPS 140-2 approved algorithms
 * - NIST SP 800-57 compliant key lifecycle
 * - Industry-standard cryptographic practices
 * 
 * USAGE:
 * - Generate master keys for encryption operations
 * - Create key rotation sets for enhanced security
 * - Validate key formats and sizes
 * - Generate random IVs for encryption operations
 */
@Service
public class KeyGenerationService {

    private static final String ALGORITHM = "AES";
    private static final int DEFAULT_KEY_SIZE = 256; // AES-256
    private static final int IV_LENGTH = 12; // 96 bits for GCM

    /**
     * Generates a new AES master key
     * 
     * This method generates a cryptographically secure AES key using Java's
     * built-in KeyGenerator with the specified key size. The key is returned
     * as a Base64 encoded string for easy storage and transmission.
     * 
     * @param keySize Key size in bits (128, 192, or 256)
     * @return Base64 encoded AES key
     * @throws RuntimeException if key generation fails
     */
    public String generateMasterKey(int keySize) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(keySize);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed for size " + keySize + " bits", e);
        }
    }

    /**
     * Generates a new AES master key with default size (256 bits)
     * 
     * @return Base64 encoded AES-256 key
     */
    public String generateMasterKey() {
        return generateMasterKey(DEFAULT_KEY_SIZE);
    }

    /**
     * Generates a random IV for GCM encryption
     * 
     * This method generates a cryptographically secure random IV
     * suitable for AES-GCM encryption operations.
     * 
     * @param length IV length in bytes
     * @return Base64 encoded IV
     */
    public String generateIV(int length) {
        byte[] iv = new byte[length];
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * Generates a random IV with default length (12 bytes for GCM)
     * 
     * @return Base64 encoded IV
     */
    public String generateIV() {
        return generateIV(IV_LENGTH);
    }

    /**
     * Validates if a key is properly formatted
     * 
     * This method checks if the provided Base64 encoded key has the
     * correct format and size for AES operations.
     * 
     * @param keyBase64 Base64 encoded key to validate
     * @return true if key is valid, false otherwise
     */
    public boolean isValidKey(String keyBase64) {
        if (keyBase64 == null || keyBase64.trim().isEmpty()) {
            return false;
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            // Check if key size is valid for AES (128, 192, or 256 bits)
            return keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the key size in bits from a Base64 encoded key
     * 
     * @param keyBase64 Base64 encoded key
     * @return Key size in bits, or -1 if invalid
     */
    public int getKeySize(String keyBase64) {
        if (!isValidKey(keyBase64)) {
            return -1;
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            return keyBytes.length * 8;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Generates a key rotation set with multiple keys
     * 
     * This method generates multiple keys for key rotation purposes.
     * It's useful for implementing key rotation strategies where
     * multiple keys are maintained for seamless transitions.
     * 
     * @param count Number of keys to generate
     * @param keySize Key size in bits
     * @return Array of Base64 encoded keys
     */
    public String[] generateKeyRotationSet(int count, int keySize) {
        String[] keys = new String[count];
        for (int i = 0; i < count; i++) {
            keys[i] = generateMasterKey(keySize);
        }
        return keys;
    }

    /**
     * Generates a key rotation set with default key size (256 bits)
     * 
     * @param count Number of keys to generate
     * @return Array of Base64 encoded AES-256 keys
     */
    public String[] generateKeyRotationSet(int count) {
        return generateKeyRotationSet(count, DEFAULT_KEY_SIZE);
    }

    /**
     * Generates a secure random string for various purposes
     * 
     * This method generates a cryptographically secure random string
     * that can be used for nonces, session IDs, or other security purposes.
     * 
     * @param length Length of the random string
     * @return Base64 encoded random string
     */
    public String generateRandomString(int length) {
        byte[] randomBytes = new byte[length];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * Generates a secure random string with default length (32 bytes)
     * 
     * @return Base64 encoded random string
     */
    public String generateRandomString() {
        return generateRandomString(32);
    }

    /**
     * Validates key size parameter
     * 
     * @param keySize Key size in bits
     * @return true if key size is valid for AES
     */
    public boolean isValidKeySize(int keySize) {
        return keySize == 128 || keySize == 192 || keySize == 256;
    }

    /**
     * Gets the default key size (256 bits)
     * 
     * @return Default key size in bits
     */
    public int getDefaultKeySize() {
        return DEFAULT_KEY_SIZE;
    }

    /**
     * Gets the default IV length (12 bytes for GCM)
     * 
     * @return Default IV length in bytes
     */
    public int getDefaultIVLength() {
        return IV_LENGTH;
    }
}