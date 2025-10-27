package com.vol.pgswitch.controller;

import com.vol.pgswitch.model.UserKeyEntity;
import com.vol.pgswitch.service.crypto.UserKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user-keys")
public class UserKeyController {

    private final UserKeyService userKeyService;
    private final String activeMasterKey;

    public UserKeyController(UserKeyService userKeyService,
                             @Value("${app.crypto.master-key-base64:}") String activeMasterKey) {
        this.userKeyService = userKeyService;
        this.activeMasterKey = activeMasterKey;
    }

    @PostMapping("/{userId}/generate")
    public ResponseEntity<Map<String, Object>> generate(@PathVariable String userId,
                                                        @RequestParam(name = "keySize", required = false) Integer keySize) {
        try {
            int size = keySize != null ? keySize : 256;
            String key = userKeyService.generateAndStoreUserKey(userId, size);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "key", key,
                    "keySize", size
            ));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserKeyEntity> metadata(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(userKeyService.getUserKeyMetadata(userId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{userId}/rotate")
    public ResponseEntity<Map<String, Object>> rotate(@PathVariable String userId,
                                                      @RequestParam(name = "keySize", required = false) Integer keySize) {
        try {
            int size = keySize != null ? keySize : 256;
            String key = userKeyService.generateAndStoreUserKey(userId, size);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "key", key,
                    "keySize", size,
                    "rotated", true
            ));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String userId) {
        try {
            userKeyService.deleteUserKey(userId);
            return ResponseEntity.ok(Map.of("userId", userId, "deleted", true));
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{userId}/plaintext")
    public ResponseEntity<Map<String, Object>> getPlaintext(@PathVariable String userId) {
        try {
            String key = userKeyService.getUserKeyPlaintext(userId);
            return ResponseEntity.ok(Map.of("userId", userId, "key", key));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // Deprecated: rewrap moved to KeyManagementController.rotate
}


