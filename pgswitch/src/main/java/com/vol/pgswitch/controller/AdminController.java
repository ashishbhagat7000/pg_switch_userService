package com.vol.pgswitch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vol.pgswitch.dto.MerchantApplicationRequest;
import com.vol.pgswitch.dto.MerchantUpdateRequest;
import com.vol.pgswitch.dto.MerchantViewResponse;
import com.vol.pgswitch.dto.RelationTupleDto;
import com.vol.pgswitch.model.MerchantApplicationEntity;
import com.vol.pgswitch.service.MerchantApplicationService;
import com.vol.pgswitch.service.crypto.CryptoService;
import com.vol.pgswitch.service.keto.KetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/merchants")
@Tag(name = "Admin - Merchant Management", description = "Administrator merchant management operations")
public class AdminController {
    private final KetoService ketoService;

    public AdminController(KetoService ketoService) {
        this.ketoService = ketoService;
    }


    @PostMapping("/create")
    public ResponseEntity<String> createRelation(@RequestBody RelationTupleDto dto) {
        return ketoService.createRelation(dto);
    }

    @GetMapping("/relations")
    public ResponseEntity<String> listRelations() {
        return ketoService.listRelations();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteRelation(@RequestBody RelationTupleDto dto) {
        return ketoService.deleteRelation(dto);
    }

    @PostMapping("/check")
    public ResponseEntity<String> checkPermission(@RequestBody RelationTupleDto dto) {
        return ketoService.checkPermission(dto);
    }

    /**
     * Bootstrap a demo role hierarchy and assign a role to a merchant_application object.
     * This is a helper that issues multiple admin API calls to create the relations.
     * Example behavior (for merchant app id=1):
     * - user:alice member of roles:supermerchant
     * - roles:supermerchant -> roles:merchant
     * - roles:merchant -> roles:submerchant
     * - roles:supermerchant has relation 'owner' on merchant_application:1
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<String> bootstrapDemo(@RequestParam(defaultValue = "1") Long merchantAppId) {
        try {
            // ✅ user:alice -> roles:supermerchant
            RelationTupleDto r = new RelationTupleDto();
            r.setNamespace("roles");
            r.setObject("supermerchant");
            r.setRelation("member");
            r.setSubjectId("user:alice");
            ketoService.createRelation(r);

            // ✅ roles:supermerchant -> roles:merchant (as subject_set)
            r = new RelationTupleDto();
            r.setNamespace("roles");
            r.setObject("merchant");
            r.setRelation("inherits");

            RelationTupleDto.SubjectSet subjectSet1 = new RelationTupleDto.SubjectSet();
            subjectSet1.setNamespace("roles");
            subjectSet1.setObject("supermerchant");
            subjectSet1.setRelation("member");
            r.setSubject_set(subjectSet1);
            ketoService.createRelation(r);

            // ✅ roles:merchant -> roles:submerchant
            r = new RelationTupleDto();
            r.setNamespace("roles");
            r.setObject("submerchant");
            r.setRelation("inherits");

            RelationTupleDto.SubjectSet subjectSet2 = new RelationTupleDto.SubjectSet();
            subjectSet2.setNamespace("roles");
            subjectSet2.setObject("merchant");
            subjectSet2.setRelation("member");
            r.setSubject_set(subjectSet2);
            ketoService.createRelation(r);

            // ✅ roles:supermerchant owner of merchant_application:{id}
            r = new RelationTupleDto();
            r.setNamespace("merchant_application");
            r.setObject(String.valueOf(merchantAppId));
            r.setRelation("owner");

            RelationTupleDto.SubjectSet subjectSet3 = new RelationTupleDto.SubjectSet();
            subjectSet3.setNamespace("roles");
            subjectSet3.setObject("supermerchant");
            subjectSet3.setRelation("member");
            r.setSubject_set(subjectSet3);
            ketoService.createRelation(r);

            return ResponseEntity.ok("✅ Bootstrap completed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Bootstrap failed: " + e.getMessage());
        }
    }

    /**
     * Authorize current principal as an Admin.
     * This mirrors the logic in SuperAdminController.authorizeSuperadmin()
     * but checks membership in the `roles:admin` subject_set.
     *
     * Returns null when authorized (so the security component treats it as allowed),
     * or a ResponseEntity with an appropriate status otherwise.
     */
    public ResponseEntity<?> authorizeAdmin() {
        try {
            // 1️⃣ Require authenticated principal
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String subjectId = authentication.getName();

            // 2️⃣ Extract groups from JWT or authorities
            List<String> groups = null;
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                groups = jwt.getClaimAsStringList("cognito:groups");
            }

            if (groups == null || groups.isEmpty()) {
                groups = authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
                        .map(String::toLowerCase)
                        .toList();
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    if (group == null || group.isBlank()) continue;

                    RelationTupleDto checkDto = new RelationTupleDto();
                    checkDto.setNamespace("roles");
                    checkDto.setObject("admin");
                    checkDto.setRelation("member");

                    RelationTupleDto.SubjectSet subjectSet = new RelationTupleDto.SubjectSet();
                    subjectSet.setNamespace("roles");
                    subjectSet.setObject(group.trim().toLowerCase());
                    subjectSet.setRelation("member");
                    checkDto.setSubject_set(subjectSet);

                    var resp = ketoService.checkPermission(checkDto);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        var node = mapper.readTree(resp.getBody());
                        if (node.has("allowed") && node.get("allowed").asBoolean()) {
                            return null; // Authorized
                        }
                    }
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error while authorizing via group check: " + e.getMessage());
        }

        // Fallback: check direct subject membership
        try {
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String subjectId = authentication.getName();

            RelationTupleDto checkDto = new RelationTupleDto();
            checkDto.setNamespace("roles");
            checkDto.setObject("admin");
            checkDto.setRelation("member");
            checkDto.setSubjectId("user:" + subjectId);

            var resp = ketoService.checkPermission(checkDto);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(resp.getBody());
            if (!node.has("allowed") || !node.get("allowed").asBoolean()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error while checking direct subject: " + e.getMessage());
        }

        return null; // authorized
    }

}
