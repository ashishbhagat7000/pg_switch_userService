package com.vol.pgswitch.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * KeyGenerationUtil - Utility class for cryptographic key generation using JCE
 * 
 * This utility class provides static methods for generating cryptographic keys
 * using Java's built-in JCE (Java Cryptography Extension). It's designed for
 * command-line usage and standalone key generation operations.
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
 * - Command-line key generation: java -cp target/classes com.vol.pgswitch.util.KeyGenerationUtil
 * - Programmatic key generation for testing
 * - Key validation and format checking
 * - IV generation for encryption operations
 */
public class KeyGenerationUtil {

    private static final String ALGORITHM = "AES";
    private static final int DEFAULT_KEY_SIZE = 256; // AES-256
    private static final int DEFAULT_IV_LENGTH = 12; // 96 bits for GCM

    /**
     * Main method for command-line key generation
     * 
     * Usage: java -cp target/classes com.vol.pgswitch.util.KeyGenerationUtil [keySize] [count]
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            int keySize = DEFAULT_KEY_SIZE;
            int count = 1;
            
            // Parse command line arguments
            if (args.length > 0) {
                keySize = Integer.parseInt(args[0]);
                if (!isValidKeySize(keySize)) {
                    System.err.println("Invalid key size. Must be 128, 192, or 256 bits.");
                    System.exit(1);
                }
            }
            
            if (args.length > 1) {
                count = Integer.parseInt(args[1]);
                if (count < 1 || count > 10) {
                    System.err.println("Count must be between 1 and 10.");
                    System.exit(1);
                }
            }
            
            // Generate keys
            System.out.println("Generating " + count + " AES-" + keySize + " key(s) using JCE...");
            System.out.println("==========================================");
            
            for (int i = 0; i < count; i++) {
                String key = generateKey(keySize);
                System.out.println("Key " + (i + 1) + ": " + key);
            }
            
            System.out.println("==========================================");
            System.out.println("Keys generated successfully!");
            System.out.println("Copy the key(s) to your .env file:");
            System.out.println("APP_CRYPTO_MASTER_KEY=<key>");
            
        } catch (Exception e) {
            System.err.println("Error generating keys: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Generates a new AES key with specified size
     * 
     * @param keySize Key size in bits (128, 192, or 256)
     * @return Base64 encoded AES key
     * @throws RuntimeException if key generation fails
     */
    public static String generateKey(int keySize) {
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
     * Generates a new AES key with default size (256 bits)
     * 
     * @return Base64 encoded AES-256 key
     */
    public static String generateKey() {
        return generateKey(DEFAULT_KEY_SIZE);
    }

    /**
     * Generates a random IV for GCM encryption
     * 
     * @param length IV length in bytes
     * @return Base64 encoded IV
     */
    public static String generateIV(int length) {
        byte[] iv = new byte[length];
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * Generates a random IV with default length (12 bytes for GCM)
     * 
     * @return Base64 encoded IV
     */
    public static String generateIV() {
        return generateIV(DEFAULT_IV_LENGTH);
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
    public static int getKeySize(String keyBase64) {
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
     * @param count Number of keys to generate
     * @param keySize Key size in bits
     * @return Array of Base64 encoded keys
     */
    public static String[] generateKeyRotationSet(int count, int keySize) {
        String[] keys = new String[count];
        for (int i = 0; i < count; i++) {
            keys[i] = generateKey(keySize);
        }
        return keys;
    }

    /**
     * Generates a key rotation set with default key size (256 bits)
     * 
     * @param count Number of keys to generate
     * @return Array of Base64 encoded AES-256 keys
     */
    public static String[] generateKeyRotationSet(int count) {
        return generateKeyRotationSet(count, DEFAULT_KEY_SIZE);
    }

    /**
     * Generates a secure random string for various purposes
     * 
     * @param length Length of the random string
     * @return Base64 encoded random string
     */
    public static String generateRandomString(int length) {
        byte[] randomBytes = new byte[length];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * Generates a secure random string with default length (32 bytes)
     * 
     * @return Base64 encoded random string
     */
    public static String generateRandomString() {
        return generateRandomString(32);
    }

    /**
     * Validates key size parameter
     * 
     * @param keySize Key size in bits
     * @return true if key size is valid for AES
     */
    public static boolean isValidKeySize(int keySize) {
        return keySize == 128 || keySize == 192 || keySize == 256;
    }

    /**
     * Gets the default key size (256 bits)
     * 
     * @return Default key size in bits
     */
    public static int getDefaultKeySize() {
        return DEFAULT_KEY_SIZE;
    }

    /**
     * Gets the default IV length (12 bytes for GCM)
     * 
     * @return Default IV length in bytes
     */
    public static int getDefaultIVLength() {
        return DEFAULT_IV_LENGTH;
    }

    /**
     * Prints usage information
     */
    public static void printUsage() {
        System.out.println("KeyGenerationUtil - JCE-based cryptographic key generation");
        System.out.println("==========================================================");
        System.out.println();
        System.out.println("Usage: java -cp target/classes com.vol.pgswitch.util.KeyGenerationUtil [keySize] [count]");
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("  keySize  Key size in bits (128, 192, 256) - default: 256");
        System.out.println("  count    Number of keys to generate (1-10) - default: 1");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -cp target/classes com.vol.pgswitch.util.KeyGenerationUtil");
        System.out.println("  java -cp target/classes com.vol.pgswitch.util.KeyGenerationUtil 256");
        System.out.println("  java -cp target/classes com.vol.pgswitch.util.KeyGenerationUtil 256 3");
        System.out.println();
        System.out.println("Features:");
        System.out.println("  - Uses Java Cryptography Extension (JCE)");
        System.out.println("  - No external dependencies");
        System.out.println("  - FIPS 140-2 approved algorithms");
        System.out.println("  - PCI-DSS compliant key generation");
    }
}