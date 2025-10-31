package com.vol.pgswitch.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vol.pgswitch.annotations.SuperadminOnly;
import com.vol.pgswitch.annotations.SupermerchantOnly;
import com.vol.pgswitch.dto.BulkRelationTuplesRequest;
import com.vol.pgswitch.dto.UserCreateRequest;
import com.vol.pgswitch.dto.*;
import com.vol.pgswitch.model.MerchantApplicationEntity;
import java.time.LocalDateTime;

import com.vol.pgswitch.service.MerchantApplicationService;
import com.vol.pgswitch.service.crypto.CryptoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;
import com.vol.pgswitch.dto.UserDto;
import com.vol.pgswitch.dto.UserUpdateRequest;
import com.vol.pgswitch.dto.RelationTupleDto;
import com.vol.pgswitch.service.keto.KetoService;
import com.vol.pgswitch.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/superadmin")
@Tag(name = "Super Admin - User & Permissions Management", description = "Super Administrator operations for user management and Keto API permissions")
public class SuperAdminController {

    private final KetoService ketoService;
    private final UserService userService;

        private final MerchantApplicationService merchantApplicationService;
        private final CryptoService cryptoService;

        public SuperAdminController(KetoService ketoService, 
                                  UserService userService,
                                  MerchantApplicationService merchantApplicationService,
                                  CryptoService cryptoService) {
        this.ketoService = ketoService;
            this.userService = userService;
            this.merchantApplicationService = merchantApplicationService;
            this.cryptoService = cryptoService;
    }

    // User Management Endpoints

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Returns a paginated list of all users. Requires SuperAdmin role.")
    public ResponseEntity<Page<UserDto>> listUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "username") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        PageRequest pageRequest = PageRequest.of(
            page, size, Sort.by(direction, sortBy));
        
        return ResponseEntity.ok(userService.findAllUsers(pageRequest));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a single user by their ID. Requires SuperAdmin role.")
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @PostMapping("/users")
    @Operation(summary = "Create new user", description = "Creates a new user with specified roles. Requires SuperAdmin role.")
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user's details. Requires SuperAdmin role.")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user", description = "Soft deletes a user by ID. Requires SuperAdmin role.")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Keto Relations Management

    @PostMapping("/relations")
    @Operation(summary = "Create relation tuple", description = "Creates a new Keto relation tuple. Requires SuperAdmin role.")
    public ResponseEntity<String> createRelation(@RequestBody RelationTupleDto dto) {
        return ketoService.createRelation(dto);
    }

    @GetMapping("/relations")
    @Operation(summary = "List relations", description = "Lists all Keto relation tuples. Requires SuperAdmin role.")
    public ResponseEntity<String> listRelations() {
        return ketoService.listRelations();
    }

    @DeleteMapping("/relations")
    @Operation(summary = "Delete relation tuple", description = "Deletes a Keto relation tuple. Requires SuperAdmin role.")
    public ResponseEntity<String> deleteRelation(@RequestBody RelationTupleDto dto) {
        return ketoService.deleteRelation(dto);
    }

    @PostMapping("/check")
    @Operation(summary = "Check permission", description = "Checks if a permission is granted. Requires SuperAdmin role.")
    public ResponseEntity<String> checkPermission(@RequestBody RelationTupleDto dto) {
        return ketoService.checkPermission(dto);
    }

    @PostMapping("/relations/bulk-seed")
    @Operation(
        summary = "Bulk import relation tuples", 
        description = "Creates multiple relation tuples in a single operation. Used for bootstrapping or restoring permissions. Requires SuperAdmin role."
    )
    public ResponseEntity<Map<String, Object>> bulkSeedRelations(
            @Valid @RequestBody BulkRelationTuplesRequest request) {
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        for (RelationTupleDto tuple : request.getTuples()) {
            try {
                ResponseEntity<String> response = ketoService.createRelation(tuple);
                if (response.getStatusCode().is2xxSuccessful()) {
                    successCount++;
                } else {
                    failureCount++;
                    errors.add("Failed to create tuple: " + tuple + ", Status: " + response.getStatusCode());
                }
            } catch (Exception e) {
                failureCount++;
                errors.add("Error creating tuple: " + tuple + ", Error: " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalProcessed", request.getTuples().size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        HttpStatus status = failureCount == 0 ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/relations/export")
    @Operation(
        summary = "Export all relation tuples", 
        description = "Retrieves all current relation tuples. Useful for backup or migration. Requires SuperAdmin role."
    )
    public ResponseEntity<BulkRelationTuplesRequest> exportRelations() {
        ResponseEntity<String> response = ketoService.listRelations();
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        
        try {
            // Parse the JSON response from Keto
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            List<RelationTupleDto> tuples = new ArrayList<>();
            
            if (root.isArray()) {
                for (JsonNode node : root) {
                    RelationTupleDto tuple = mapper.treeToValue(node, RelationTupleDto.class);
                    tuples.add(tuple);
                }
            }

            BulkRelationTuplesRequest result = new BulkRelationTuplesRequest();
            result.setTuples(tuples);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error parsing relation tuples: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Merchant Application Management

    @GetMapping("/merchant-applications")
    @SuperadminOnly
    @Operation(summary = "List all merchant applications", 
               description = "Returns a paginated list of all merchant applications. Requires SuperAdmin role.")
    public ResponseEntity<Map<String, Object>> getAllMerchantApplications(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<MerchantApplicationEntity> merchants = merchantApplicationService.findAllMerchants(pageable, search);

        List<MerchantViewResponse> merchantViews = merchants.getContent().stream()
                .map(this::convertToViewResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("applications", merchantViews);
        response.put("currentPage", merchants.getNumber());
        response.put("totalItems", merchants.getTotalElements());
        response.put("totalPages", merchants.getTotalPages());
        response.put("hasNext", merchants.hasNext());
        response.put("hasPrevious", merchants.hasPrevious());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchant-applications/{id}")
    @SuperadminOnly
    @Operation(summary = "Get merchant application by ID", 
               description = "Returns detailed information about a merchant application, including decrypted data. Requires SuperAdmin role.")
    public ResponseEntity<MerchantViewResponse> getMerchantApplicationById(
            @Parameter(description = "Application ID") @PathVariable Long id) {
        MerchantApplicationEntity merchant = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Merchant application not found with ID: " + id));
        return ResponseEntity.ok(convertToViewResponse(merchant));
    }

    /**
     * Get fully decrypted merchant by ID (for super admin diagnostics)
     * mode=master|user. If user, provide userId to use user's key.
     */

    @Operation(summary = "Get decrypted merchant by ID", description = "Return decrypted fields using master or user key")
    @SuperadminOnly
    @GetMapping("/{id}/decrypted")
    public ResponseEntity<Map<String, Object>> getMerchantByIdDecrypted(
            @PathVariable Long id,
            @RequestParam(defaultValue = "master") String mode,
            @RequestParam(required = false) String userId) {
        MerchantApplicationEntity entity = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        String businessPan = cryptoService.decrypt(entity.getBusinessPanEncrypted());
        String gstin = cryptoService.decrypt(entity.getGstinEncrypted());
        String cin = cryptoService.decrypt(entity.getCinEncrypted());
        String govtId = cryptoService.decrypt(entity.getSignatoryGovtIdNumberEncrypted());
        String accountNumber = cryptoService.decrypt(entity.getAccountNumberEncrypted());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", entity.getId());
        out.put("legalEntityName", entity.getLegalEntityName());
        out.put("status", entity.getStatus() != null ? entity.getStatus().name() : "UNKNOWN");
        out.put("createdAt", entity.getCreatedAt());
        out.put("businessPan", businessPan);
        out.put("gstin", gstin);
        out.put("cin", cin);
        out.put("signatoryGovtIdNumber", govtId);
        out.put("accountNumber", accountNumber);
        return ResponseEntity.ok(out);
    }





    /**
     * Delete merchant application immediately
     */

    @Operation(summary = "Delete merchant", description = "Delete merchant application and associated KYC documents")
    @SuperadminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable Long id) {
        try {
            merchantApplicationService.deleteMerchant(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Merchant deleted successfully");
            response.put("merchantId", id);
            response.put("deletedAt", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }


    /**
     * Get merchant KYC documents
     */

    @Operation(summary = "Get merchant KYC documents", description = "Retrieve all KYC documents for a merchant")
    @SuperadminOnly
    @GetMapping("/{id}/kyc-documents")
    public ResponseEntity<Map<String, Object>> getMerchantKycDocuments(@PathVariable Long id) {
        MerchantApplicationEntity merchant = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        List<MerchantViewResponse.KycDocumentView> kycDocs = merchant.getKycDocuments().stream()
                .map(doc -> {
                    MerchantViewResponse.KycDocumentView view = new MerchantViewResponse.KycDocumentView();
                    view.setId(doc.getId());
                    view.setDocumentType(doc.getDocumentType());
                    view.setOriginalFilename(doc.getOriginalFilename());
                    view.setSizeBytes(doc.getSizeBytes());
                    view.setContentType(doc.getContentType());
                    view.setDownloadUrl("/api/super-admin/merchants/" + id + "/kyc-documents/" + doc.getId() + "/download");
                    return view;
                })
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("merchantId", id);
        response.put("merchantName", merchant.getLegalEntityName());
        response.put("kycDocuments", kycDocs);
        response.put("totalDocuments", kycDocs.size());
        return ResponseEntity.ok(response);
    }


    /**
     * Download KYC document
     */

    @Operation(summary = "Download KYC document", description = "Download a specific KYC document")
    @SuperadminOnly
    @GetMapping("/{merchantId}/kyc-documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadKycDocument(
            @PathVariable String merchantId,
            @PathVariable String documentId) {
        String content = "KYC Document content for merchant: " + merchantId + ", document: " + documentId;
        byte[] contentBytes = content.getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "kyc-document-" + documentId + ".pdf");
        return new ResponseEntity<>(contentBytes, headers, HttpStatus.OK);
    }


    /**
     * Export merchants to CSV
     */

    @Operation(summary = "Export merchants to CSV", description = "Export merchant list to CSV format")
    @SuperadminOnly
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportMerchantsToCsv(
            @RequestParam(required = false) String search) {
        List<MerchantApplicationEntity> merchants = merchantApplicationService.findAllMerchantsForExport(search);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Legal Entity Name,Business Type,Contact Email,City,State,Status,Created At\n");
        for (MerchantApplicationEntity merchant : merchants) {
            csv.append(merchant.getId()).append(",")
                    .append(merchant.getLegalEntityName()).append(",")
                    .append(merchant.getBusinessType()).append(",")
                    .append(merchant.getContactEmail()).append(",")
                    .append(merchant.getCity()).append(",")
                    .append(merchant.getState()).append(",")
                    .append(merchant.getStatus() != null ? merchant.getStatus().name() : "UNKNOWN").append(",")
                    .append(merchant.getCreatedAt() != null ? new java.util.Date(merchant.getCreatedAt()) : "N/A")
                    .append("\n");
        }
        byte[] csvBytes = csv.toString().getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "merchants-export-" + System.currentTimeMillis() + ".csv");
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }


    /**
     * Export merchants to PDF
     */

    @Operation(summary = "Export merchants to PDF", description = "Export merchant list to PDF format")
    @SuperadminOnly
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportMerchantsToPdf(
            @RequestParam(required = false) String search) {
        String pdfContent = "PDF Export of Merchants\n\nSearch: " + (search != null ? search : "All") + "\n\n";
        pdfContent += "This would contain the actual PDF content with merchant data.\n";
        pdfContent += "Generated at: " + new Date();
        byte[] pdfBytes = pdfContent.getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "merchants-export-" + System.currentTimeMillis() + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }


    @PostMapping("/merchant-applications/{id}/approve")
    @SuperadminOnly
    @Operation(summary = "Approve merchant application", 
               description = "Approves a merchant application and triggers necessary notifications. Requires SuperAdmin role.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveMerchantApplication(
            @Parameter(description = "Application ID") @PathVariable Long id) {
        try {
            MerchantApplicationEntity merchant = merchantApplicationService.approveMerchant(id);
            
            Map<String, Object> data = new HashMap<>();
            data.put("applicationId", merchant.getId());
            data.put("status", merchant.getStatus());
            data.put("approvedAt", merchant.getUpdatedAt());
            data.put("legalEntityName", merchant.getLegalEntityName());
            
            return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "APPLICATION_APPROVED",
                "Merchant application approved successfully.",
                data,
                defaultMeta()
            ));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/merchant-applications/{id}/reject")
    @SuperadminOnly
    @Operation(summary = "Reject merchant application", 
               description = "Rejects a merchant application with a reason and triggers notifications. Requires SuperAdmin role.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectMerchantApplication(
            @Parameter(description = "Application ID") @PathVariable Long id,
            @Parameter(description = "Rejection reason") @RequestParam String reason) {
        try {
            MerchantApplicationEntity merchant = merchantApplicationService.rejectMerchant(id, reason);
            
            Map<String, Object> data = new HashMap<>();
            data.put("applicationId", merchant.getId());
            data.put("status", merchant.getStatus());
            data.put("rejectedAt", merchant.getUpdatedAt());
            data.put("reason", reason);
            data.put("legalEntityName", merchant.getLegalEntityName());
            
            return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "APPLICATION_REJECTED",
                "Merchant application rejected.",
                data,
                defaultMeta()
            ));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping("/merchant-applications/{id}")
    @SuperadminOnly
    @Operation(summary = "Delete merchant application", 
               description = "Permanently deletes a merchant application and all associated data. Requires SuperAdmin role.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteMerchantApplication(
            @Parameter(description = "Application ID") @PathVariable Long id) {
        try {
            MerchantApplicationEntity merchant = merchantApplicationService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Merchant application not found with ID: " + id));

            String legalEntityName = merchant.getLegalEntityName();
            merchantApplicationService.deleteMerchant(id);
            
            Map<String, Object> data = new HashMap<>();
            data.put("applicationId", id);
            data.put("deletedAt", LocalDateTime.now());
            data.put("legalEntityName", legalEntityName);
            
            return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "APPLICATION_DELETED",
                "Merchant application deleted successfully.",
                data,
                defaultMeta()
            ));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private MerchantViewResponse convertToViewResponse(MerchantApplicationEntity entity) {
        MerchantViewResponse response = new MerchantViewResponse();
        response.setId(entity.getId());
        response.setLegalEntityName(entity.getLegalEntityName());
        response.setBrandName(entity.getBrandName());
        response.setBusinessType(entity.getBusinessType());
        response.setBusinessCategory(entity.getBusinessCategory());
        response.setIncorporationDate(entity.getIncorporationDate());
        response.setBusinessPan(maskSensitiveData(cryptoService.decrypt(entity.getBusinessPanEncrypted()), 4));
        response.setGstin(maskSensitiveData(cryptoService.decrypt(entity.getGstinEncrypted()), 4));
        response.setCin(maskSensitiveData(cryptoService.decrypt(entity.getCinEncrypted()), 4));
        response.setAddressLine1(entity.getAddressLine1());
        response.setAddressLine2(entity.getAddressLine2());
        response.setCity(entity.getCity());
        response.setState(entity.getState());
        response.setCountry(entity.getCountry());
        response.setPincode(entity.getPincode());
        response.setContactNumber(entity.getContactNumber());
        response.setContactEmail(entity.getContactEmail());
        response.setWebsiteUrl(entity.getWebsiteUrl());
        response.setSignatoryFullName(entity.getSignatoryFullName());
        response.setSignatoryDob(entity.getSignatoryDob());
        response.setSignatoryDesignation(entity.getSignatoryDesignation());
        response.setSignatoryMobile(entity.getSignatoryMobile());
        response.setSignatoryEmail(entity.getSignatoryEmail());
        response.setSignatoryGovtIdType(entity.getSignatoryGovtIdType());
        response.setSignatoryGovtIdNumber(maskSensitiveData(cryptoService.decrypt(entity.getSignatoryGovtIdNumberEncrypted()), 4));
        response.setAccountHolderName(entity.getAccountHolderName());
        response.setBankName(entity.getBankName());
        response.setBranchName(entity.getBranchName());
        response.setAccountNumber(maskSensitiveData(cryptoService.decrypt(entity.getAccountNumberEncrypted()), 4));
        response.setIfscCode(entity.getIfscCode());
        response.setAccountType(entity.getAccountType());
        response.setGoodsOrServices(entity.getGoodsOrServices());
        response.setAverageTicketSizeInr(entity.getAverageTicketSizeInr());
        response.setExpectedMonthlyVolumeInr(entity.getExpectedMonthlyVolumeInr());
        response.setExpectedAnnualTurnoverInr(entity.getExpectedAnnualTurnoverInr());
        response.setRefundPolicyUrl(entity.getRefundPolicyUrl());
        response.setRiskCategory(entity.getRiskCategory());
        response.setCreatedAt(entity.getCreatedAt());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : "UNKNOWN");
        
        // Add KYC documents if available
        if (entity.getKycDocuments() != null) {
            List<MerchantViewResponse.KycDocumentView> kycDocs = entity.getKycDocuments().stream()
                .map(doc -> {
                    MerchantViewResponse.KycDocumentView view = new MerchantViewResponse.KycDocumentView();
                    view.setId(doc.getId());
                    view.setDocumentType(doc.getDocumentType());
                    view.setOriginalFilename(doc.getOriginalFilename());
                    view.setSizeBytes(doc.getSizeBytes());
                    view.setContentType(doc.getContentType());
                    view.setDownloadUrl("/api/superadmin/merchant-applications/" + entity.getId() + 
                                     "/kyc-documents/" + doc.getId() + "/download");
                    return view;
                })
                .collect(Collectors.toList());
            response.setKycDocuments(kycDocs);
        }
        
        return response;
    }

    private String maskSensitiveData(String data, int visibleChars) {
        if (data == null || data.length() <= visibleChars) {
            return data;
        }
        return "*".repeat(data.length() - visibleChars) + data.substring(data.length() - visibleChars);
    }

    private Map<String, Object> defaultMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestId", UUID.randomUUID().toString());
        meta.put("timestamp", LocalDateTime.now());
        return meta;
    }

    public ResponseEntity<?> authorizeSuperadmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String subjectId = authentication.getName();

        try {
            List<String> groups = null;
            if (authentication.getPrincipal() instanceof Jwt jwt) {
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

            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    if (group == null || group.isBlank()) continue;

                    RelationTupleDto checkDto = new RelationTupleDto();
                    checkDto.setNamespace("roles");
                    checkDto.setObject("superadmin"); 
                    checkDto.setRelation("member");

                    RelationTupleDto.SubjectSet subjectSet = new RelationTupleDto.SubjectSet();
                    subjectSet.setNamespace("roles");
                    subjectSet.setObject(group.trim().toLowerCase());
                    subjectSet.setRelation("member");
                    checkDto.setSubject_set(subjectSet);

                    var resp = ketoService.checkPermission(checkDto);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.getBody());
                        if (node.has("allowed") && node.get("allowed").asBoolean()) {
                            return null;
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

        try {
            RelationTupleDto checkDto = new RelationTupleDto();
            checkDto.setNamespace("roles");
            checkDto.setObject("superadmin");
            checkDto.setRelation("member");
            checkDto.setSubjectId("user:" + subjectId);

            var resp = ketoService.checkPermission(checkDto);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.getBody());
            if (!node.has("allowed") || !node.get("allowed").asBoolean()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error while checking direct subject: " + e.getMessage());
        }

        return null;
    }



//assign permission to any service through this single api to perticular user

    @PostMapping("/access/assign/{namespace}")
    @SuperadminOnly
    @Operation(summary = "Assign access to all merchants for a given service namespace")
    public ResponseEntity<Map<String, Object>> assignAccessToAllMerchants(
            @PathVariable String namespace) {

        List<MerchantApplicationEntity> merchants = merchantApplicationService.findAllMerchants();
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        for (MerchantApplicationEntity merchant : merchants) {
            try {
                RelationTupleDto tuple = new RelationTupleDto();
                tuple.setNamespace(namespace); // dynamically use wallet, pg, bbps, etc.
                tuple.setObject("merchant:" + merchant.getId());
                tuple.setRelation("use");

                RelationTupleDto.SubjectSet subjectSet = new RelationTupleDto.SubjectSet();
                subjectSet.setNamespace("roles");
                subjectSet.setObject("merchant");
                subjectSet.setRelation("member");
                tuple.setSubject_set(subjectSet);

                ResponseEntity<String> response = ketoService.createRelation(tuple);

                if (response.getStatusCode().is2xxSuccessful()) {
                    successCount++;
                } else {
                    failureCount++;
                    errors.add("Merchant " + merchant.getId() + " failed: " + response.getStatusCode());
                }

            } catch (Exception e) {
                failureCount++;
                errors.add("Merchant " + merchant.getId() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("namespace", namespace);
        result.put("totalMerchants", merchants.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("errors", errors);
        result.put("timestamp", System.currentTimeMillis());

        HttpStatus status = failureCount == 0 ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(result);
    }


}
