package com.vol.pgswitch.controller;

import com.vol.pgswitch.service.crypto.KeyGenerationService;
import com.vol.pgswitch.service.crypto.KeyManagementService;
import com.vol.pgswitch.service.crypto.UserKeyService;
import com.vol.pgswitch.service.crypto.CryptoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;

/**
 * KeyManagementController - REST API for key management operations
 * 
 * This controller provides REST API endpoints for cryptographic key management
 * including key generation, rotation, validation, and retrieval. It's designed
 * for administrative operations and should be secured appropriately in production.
 * 
 * SECURITY WARNING:
 * - These endpoints should be secured with proper authentication/authorization
 * - Key generation endpoints should be restricted to authorized administrators
 * - Consider rate limiting for key generation operations
 * - Log all key management operations for audit trails
 * 
 * ENDPOINTS:
 * - POST /api/admin/keys/generate - Generate new master key
 * - POST /api/admin/keys/rotate - Rotate to new key
 * - GET /api/admin/keys/current - Get current key info
 * - GET /api/admin/keys/list - List all available keys
 * - POST /api/admin/keys/validate - Validate all keys
 * - DELETE /api/admin/keys/{keyId} - Remove specific key
 * 
 * COMPLIANCE:
 * - PCI-DSS compliant key management API
 * - NIST SP 800-57 compliant key lifecycle management
 * - Audit logging for compliance requirements
 */
@RestController
@RequestMapping("/api/admin/keys")
@Tag(name = "Key Management", description = "Cryptographic key generation and management operations")
public class KeyManagementController {

    private final KeyGenerationService keyGenerationService;
    private final KeyManagementService keyManagementService;
    private final UserKeyService userKeyService;
    private final CryptoService cryptoService;
    private final String masterKeyFromProperties;
    private final String apiVersion;

    public KeyManagementController(KeyGenerationService keyGenerationService,
                                   KeyManagementService keyManagementService,
                                   UserKeyService userKeyService,
                                   CryptoService cryptoService,
                                   @Value("${app.crypto.master-key-base64:}") String masterKeyFromProperties,
                                   @Value("${app.api.version:1.0.0}") String apiVersion) {
        this.keyGenerationService = keyGenerationService;
        this.keyManagementService = keyManagementService;
        this.userKeyService = userKeyService;
        this.cryptoService = cryptoService;
        this.masterKeyFromProperties = masterKeyFromProperties;
        this.apiVersion = apiVersion;
    }

    /**
     * Generates a new master key
     * 
     * @param request Request containing key size (optional)
     * @return Generated key information
     */
    @Operation(
            summary = "Generate Master Key",
            description = """
                    Generates a new AES master key for encryption operations.
                    
                    **Security Features:**
                    - Uses cryptographically secure random number generation
                    - Supports AES-128, AES-192, and AES-256 key sizes
                    - Returns Base64 encoded key for easy storage
                    - Validates generated key before returning
                    
                    **Usage:**
                    - Default key size is 256 bits (AES-256)
                    - Key is immediately validated for correctness
                    - Generated key should be stored securely
                    """,
            tags = {"Key Management"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Key generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "keyId": "generated-key-1704067200000",
                                              "key": "XyJI9BzseORZdq8q86WPWm5ag9JQpm7LeCTMMlCPQbA=",
                                              "keySize": 256,
                                              "isValid": true,
                                              "generatedAt": 1704067200000
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateKey(
            @Parameter(
                    description = "Key generation request (optional)",
                    example = "{\"keySize\": 256}"
            )
            @RequestBody(required = false) Map<String, Object> request) {
        int keySize = 256; // Default to AES-256
        
        if (request != null && request.containsKey("keySize")) {
            keySize = (Integer) request.get("keySize");
        }
        
        String newKey = keyGenerationService.generateMasterKey(keySize);
        String keyId = "generated-key-" + System.currentTimeMillis();
        
        Map<String, Object> response = new HashMap<>();
        response.put("keyId", keyId);
        response.put("key", newKey);
        response.put("keySize", keySize);
        response.put("isValid", keyGenerationService.isValidKey(newKey));
        response.put("generatedAt", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Generates a key rotation set
     * 
     * @param request Request containing count and key size
     * @return Generated key rotation set
     */
    @PostMapping("/generate-rotation-set")
    public ResponseEntity<Map<String, Object>> generateKeyRotationSet(@RequestBody Map<String, Object> request) {
        int count = (Integer) request.getOrDefault("count", 3);
        int keySize = (Integer) request.getOrDefault("keySize", 256);
        
        String[] keyIds = keyManagementService.generateKeyRotationSet(count);
        
        Map<String, Object> response = new HashMap<>();
        response.put("keyIds", keyIds);
        response.put("count", count);
        response.put("keySize", keySize);
        response.put("generatedAt", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Rotates to a new key
     * 
     * @param request Request containing new key ID
     * @return Rotation result
     */
    @PostMapping("/rotate")
    public ResponseEntity<Map<String, Object>> rotateKey(@RequestBody(required = false) Map<String, String> request) {
        try {
            String newKeyId = request != null ? request.get("newKeyId") : null;
            String providedNewKeyBase64 = request != null ? request.get("newKeyBase64") : null;
            String providedPreviousKeyBase64 = request != null ? request.get("previousKeyBase64") : null;
            String previousKeyId = keyManagementService.getCurrentKeyId();

            boolean kms = cryptoService.isKmsEnabled();
            boolean generatedInline = false;

            int rewrapped;
            if (kms) {
                if (newKeyId == null || newKeyId.trim().isEmpty()) {
                    newKeyId = "kms-key-" + System.currentTimeMillis();
                    keyManagementService.rotateToNewKey(newKeyId);
                    generatedInline = true;
                } else {
                    boolean success = keyManagementService.rotateToExistingKey(newKeyId);
                    if (!success) {
                        throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Key not found: " + newKeyId);
                    }
                }
                rewrapped = userKeyService.rewrapAllUserKeysWithKMS();
            } else {
                if (newKeyId == null || newKeyId.trim().isEmpty()) {
                    String newKeyBase64Generated = keyGenerationService.generateMasterKey(256);
                    newKeyId = "generated-key-" + System.currentTimeMillis();
                    keyManagementService.rotateToNewKey(newKeyId);
                    providedNewKeyBase64 = newKeyBase64Generated;
                    generatedInline = true;
                } else {
                    boolean success = keyManagementService.rotateToExistingKey(newKeyId);
                    if (!success) {
                        throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Key not found: " + newKeyId);
                    }
                }
                String newMasterKeyBase64;
                if (providedNewKeyBase64 != null && !providedNewKeyBase64.trim().isEmpty()) {
                    newMasterKeyBase64 = providedNewKeyBase64;
                } else {
                    Map<String, Object> keyInfo = keyManagementService.getKeyInfo(newKeyId);
                    Object keyObj = keyInfo != null ? keyInfo.get("key") : null;
                    if (keyObj == null) {
                        throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Key material not found for keyId: " + newKeyId);
                    }
                    newMasterKeyBase64 = String.valueOf(keyObj);
                }
                String previousMasterKeyBase64;
                if (providedPreviousKeyBase64 != null && !providedPreviousKeyBase64.trim().isEmpty()) {
                    previousMasterKeyBase64 = providedPreviousKeyBase64;
                } else {
                    Map<String, Object> prevInfo = keyManagementService.getKeyInfo(previousKeyId);
                    Object prevKeyObj = prevInfo != null ? prevInfo.get("key") : null;
                    if (prevKeyObj == null) {
                        if (masterKeyFromProperties != null && !masterKeyFromProperties.trim().isEmpty()) {
                            previousMasterKeyBase64 = masterKeyFromProperties;
                        } else {
                            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Previous key material not found for keyId: " + previousKeyId);
                        }
                    } else {
                        previousMasterKeyBase64 = String.valueOf(prevKeyObj);
                    }
                }
                rewrapped = userKeyService.rewrapAllUserKeys(previousMasterKeyBase64, newMasterKeyBase64);
            }

            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("newKeyId", newKeyId);
            data.put("previousKeyId", previousKeyId);
            data.put("rotatedAt", System.currentTimeMillis());
            data.put("rewrappedUserKeys", rewrapped);
            data.put("generated", generatedInline);
            data.put("kmsManaged", kms);

            Map<String, Object> meta = new HashMap<>();
            meta.put("requestId", UUID.randomUUID().toString());
            meta.put("apiVersion", apiVersion);
            meta.put("timestamp", java.time.Instant.now().toString());

            response.put("status", "success");
            response.put("code", "KEY_ROTATED");
            response.put("message", "Master key rotated and user keys rewrapped.");
            response.put("data", data);
            response.put("meta", meta);

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Gets current key information
     * 
     * @return Current key information
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentKey() {
        String currentKeyId = keyManagementService.getCurrentKeyId();
        Map<String, Object> keyInfo = keyManagementService.getKeyInfo(currentKeyId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("currentKeyId", currentKeyId);
        response.put("keyInfo", keyInfo);
        response.put("keyCount", keyManagementService.getKeyCount());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all available keys
     * 
     * @return List of all keys with their information
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listKeys() {
        String[] keyIds = keyManagementService.getAvailableKeyIds();
        Map<String, Map<String, Object>> keyInfos = new HashMap<>();
        
        for (String keyId : keyIds) {
            keyInfos.put(keyId, keyManagementService.getKeyInfo(keyId));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("keys", keyInfos);
        response.put("totalCount", keyIds.length);
        response.put("currentKeyId", keyManagementService.getCurrentKeyId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validates all keys in the system
     * 
     * @return Validation results for all keys
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateKeys() {
        Map<String, Boolean> validationResults = keyManagementService.validateAllKeys();
        
        Map<String, Object> response = new HashMap<>();
        response.put("validationResults", validationResults);
        response.put("totalKeys", validationResults.size());
        response.put("validKeys", validationResults.values().stream().mapToInt(b -> b ? 1 : 0).sum());
        response.put("invalidKeys", validationResults.values().stream().mapToInt(b -> b ? 0 : 1).sum());
        response.put("validatedAt", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Removes a specific key
     * 
     * @param keyId Key ID to remove
     * @return Removal result
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Map<String, Object>> removeKey(@PathVariable String keyId) {
        boolean removed = keyManagementService.removeKey(keyId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("keyId", keyId);
        response.put("removed", removed);
        response.put("message", removed ? "Key removed successfully" : "Key not found or is current key");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Generates a new IV for testing purposes
     * 
     * @param request Request containing IV length
     * @return Generated IV
     */
    @PostMapping("/generate-iv")
    public ResponseEntity<Map<String, Object>> generateIV(@RequestBody(required = false) Map<String, Object> request) {
        int length = 12; // Default to 96-bit IV for GCM
        
        if (request != null && request.containsKey("length")) {
            length = (Integer) request.get("length");
        }
        
        String iv = keyGenerationService.generateIV(length);
        
        Map<String, Object> response = new HashMap<>();
        response.put("iv", iv);
        response.put("length", length);
        response.put("generatedAt", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validates a specific key
     * 
     * @param request Request containing key to validate
     * @return Validation result
     */
    @PostMapping("/validate-key")
    public ResponseEntity<Map<String, Object>> validateKey(@RequestBody Map<String, String> request) {
        String key = request.get("key");
        
        Map<String, Object> response = new HashMap<>();
        response.put("isValid", keyGenerationService.isValidKey(key));
        response.put("keySize", keyGenerationService.getKeySize(key));
        response.put("validatedAt", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
