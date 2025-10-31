
package com.vol.pgswitch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vol.pgswitch.annotations.SupermerchantOnly;
import com.vol.pgswitch.dto.MerchantApplicationRequest;
import com.vol.pgswitch.dto.MerchantUpdateRequest;
import com.vol.pgswitch.dto.MerchantViewResponse;
import com.vol.pgswitch.dto.RelationTupleDto;
import com.vol.pgswitch.model.MerchantApplicationEntity;
import com.vol.pgswitch.service.MerchantApplicationService;
import com.vol.pgswitch.service.crypto.CryptoService;
import com.vol.pgswitch.service.keto.KetoService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * SuperAdminController - REST API for super admin merchant management operations
 * 
 * This controller provides comprehensive merchant management capabilities for super administrators
 * including viewing, updating, deleting, and exporting merchant data.
 * 
 * SECURITY: These endpoints should be secured with proper authentication and authorization
 * to ensure only super administrators can access them.
 */

@RestController
@RequestMapping("/api/super-admin/merchants")
@Tag(name = "Super Admin - Merchant Management", description = "Super administrator merchant management operations")
public class SuperMerchantController {
    private final KetoService ketoService;
    private final MerchantApplicationService merchantApplicationService;
    private final CryptoService cryptoService;

    public SuperMerchantController(KetoService ketoService, MerchantApplicationService merchantApplicationService,
                                   CryptoService cryptoService) {
        this.ketoService = ketoService;
        this.merchantApplicationService = merchantApplicationService;
        this.cryptoService = cryptoService;
    }

/**
     * Submit merchant application
     */

    @Operation(
            summary = "Submit Merchant Application",
            description = """
                    Submits a complete merchant application with KYC documents for RBI PA compliance.
                    
                    **Security Features:**
                    - Sensitive data encrypted with AES-256-GCM
                    - Files stored securely outside web root
                    - Comprehensive input validation
                    - Transactional data persistence
                    
                    **Required Fields:**
                    - Legal entity name, business type, contact information
                    - Authorized signatory details with government ID
                    - Bank account information for settlement
                    - Transaction profile and business details
                    
                    **File Upload:**
                    - Maximum 10MB per file, 25MB total
                    - Supported formats: PDF, JPG, PNG, DOC, DOCX
                    - Files stored with unique, secure naming
                    """,
            tags = {"Super Admin - Merchant Management"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Application submitted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "{\"applicationId\": \"507f1f77bcf86cd799439011\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or validation errors",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = "{\"error\": \"Validation failed\", \"details\": [\"Legal entity name is required\"]}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Server Error",
                                    value = "{\"error\": \"Internal server error\", \"message\": \"File storage failed\"}"
                            )
                    )
            )
    })
    @PostMapping(value = "/submit", consumes = {"multipart/form-data"})
    public ResponseEntity<com.vol.pgswitch.dto.ApiResponse<Map<String, Object>>> submit(
            @RequestPart("data") String requestJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {

        // 🔒 Step 1: Check authorization first
        ResponseEntity<?> auth = authorizeSupermerchant();
        if (auth != null) {
            // If unauthorized, return immediately (401, 403, 503)
            return (ResponseEntity<com.vol.pgswitch.dto.ApiResponse<Map<String, Object>>>) auth;
        }

        // 🔧 Step 2: Proceed with normal submit flow
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MerchantApplicationRequest request = objectMapper.readValue(requestJson, MerchantApplicationRequest.class);

            Long id = merchantApplicationService.submit(request, files);
            Map<String, Object> data = Map.of("applicationId", id);

            return ResponseEntity.ok(
                    new com.vol.pgswitch.dto.ApiResponse<>("success", "APPLICATION_SUBMITTED", "Application submitted.", data, defaultMeta())
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid submission payload", e);
        }
    }


/**
     * Get all merchants with pagination and filtering
     */
@Operation(summary = "Get all merchants", description = "Retrieve paginated list of all merchant applications")
@SupermerchantOnly
@GetMapping
public ResponseEntity<Map<String, Object>> getAllMerchants(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir,
        @RequestParam(required = false) String search) {

    /*// 🔒 Step 1: Check authorization first
    ResponseEntity<?> auth = authorizeSupermerchant();
    if (auth != null) {
        // If unauthorized, return immediately (401, 403, 503)
        return (ResponseEntity<Map<String, Object>>) auth;
    }*/


    // ✅ Step 2: Fetch paginated data
    Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);
    Page<MerchantApplicationEntity> merchants =
            merchantApplicationService.findAllMerchants(pageable, search);

    // ✅ Step 3: Map entities to DTOs
    List<MerchantViewResponse> merchantViews = merchants.getContent().stream()
            .map(this::convertToViewResponse)
            .collect(Collectors.toList());

    // ✅ Step 4: Build response
    Map<String, Object> response = new HashMap<>();
    response.put("merchants", merchantViews);
    response.put("currentPage", merchants.getNumber());
    response.put("totalItems", merchants.getTotalElements());
    response.put("totalPages", merchants.getTotalPages());
    response.put("hasNext", merchants.hasNext());
    response.put("hasPrevious", merchants.hasPrevious());

    return ResponseEntity.ok(response);
}


    /**
     * Get single merchant by ID
     */

    @Operation(summary = "Get merchant by ID", description = "Retrieve detailed information about a specific merchant")
    @SupermerchantOnly
    @GetMapping("/{id}")
    public ResponseEntity<MerchantViewResponse> getMerchantById(@PathVariable Long id) {
        MerchantApplicationEntity merchant = merchantApplicationService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        return ResponseEntity.ok(convertToViewResponse(merchant));
    }


/**
     * Get fully decrypted merchant by ID (for super admin diagnostics)
     * mode=master|user. If user, provide userId to use user's key.
     */

    @Operation(summary = "Get decrypted merchant by ID", description = "Return decrypted fields using master or user key")
    @SupermerchantOnly
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
     * Update merchant details
     */

    @Operation(summary = "Update merchant", description = "Update merchant application details")
    @SupermerchantOnly
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMerchant(
            @PathVariable Long id,
            @Valid @RequestBody MerchantUpdateRequest updateRequest) {
        try {
            MerchantApplicationEntity updatedMerchant = merchantApplicationService.updateMerchant(id, updateRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Merchant updated successfully");
            response.put("merchantId", updatedMerchant.getId());
            response.put("updatedAt", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }


/**
     * Delete merchant application immediately
     */

    @Operation(summary = "Delete merchant", description = "Delete merchant application and associated KYC documents")
    @SupermerchantOnly
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
    @SupermerchantOnly
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
    @SupermerchantOnly
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
    @SupermerchantOnly
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
    @SupermerchantOnly
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


/**
     * Convert entity to view response with masked sensitive data
     */

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
        List<MerchantViewResponse.KycDocumentView> kycDocs = entity.getKycDocuments().stream()
            .map(doc -> {
                MerchantViewResponse.KycDocumentView view = new MerchantViewResponse.KycDocumentView();
                view.setId(doc.getId());
                view.setDocumentType(doc.getDocumentType());
                view.setOriginalFilename(doc.getOriginalFilename());
                view.setSizeBytes(doc.getSizeBytes());
                view.setContentType(doc.getContentType());
                view.setDownloadUrl("/api/super-admin/merchants/" + entity.getId() + "/kyc-documents/" + doc.getId() + "/download");
                return view;
            })
            .collect(Collectors.toList());
        response.setKycDocuments(kycDocs);
        response.setCreatedAt(entity.getCreatedAt());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : "UNKNOWN");
        return response;
    }


/**
     * Mask sensitive data showing only last few characters
     */

    private String maskSensitiveData(String data, int visibleChars) {
        if (data == null || data.length() <= visibleChars) {
            return data;
        }
        return "*".repeat(data.length() - visibleChars) + data.substring(data.length() - visibleChars);
    }

    // Helper: returns null when authorized, or a ResponseEntity to return to caller (401/403/503)

    public ResponseEntity<?> authorizeSupermerchant() {
        // 1️⃣ Require authenticated principal
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String subjectId = authentication.getName();

        try {
            // 2️⃣ Extract groups from Cognito JWT or Spring Authorities
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

            ObjectMapper mapper = new ObjectMapper();

            // 3️⃣ Check each group (subject_set)
            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    if (group == null || group.isBlank()) continue;

                    RelationTupleDto checkDto = new RelationTupleDto();
                    checkDto.setNamespace("roles");
                    checkDto.setObject("supermerchant");
                    checkDto.setRelation("member");

                    // ✅ Use nested SubjectSet object
                    RelationTupleDto.SubjectSet subjectSet = new RelationTupleDto.SubjectSet();
                    subjectSet.setNamespace("roles");
                    subjectSet.setObject(group.trim().toLowerCase());
                    subjectSet.setRelation("member");
                    checkDto.setSubject_set(subjectSet);

                    var resp = ketoService.checkPermission(checkDto);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        var node = mapper.readTree(resp.getBody());
                        if (node.has("allowed") && node.get("allowed").asBoolean()) {
                            return null; // ✅ Authorized
                        }
                    }
                }
                // ❌ None of the groups authorized
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error while authorizing via group check: " + e.getMessage());
        }

        // 4️⃣ Fallback: Check by subject_id (direct user membership)
        try {
            RelationTupleDto checkDto = new RelationTupleDto();
            checkDto.setNamespace("roles");
            checkDto.setObject("supermerchant");
            checkDto.setRelation("member");
            checkDto.setSubjectId("user:" + subjectId);

            var resp = ketoService.checkPermission(checkDto);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            ObjectMapper mapper = new ObjectMapper();
            var node = mapper.readTree(resp.getBody());
            if (!node.has("allowed") || !node.get("allowed").asBoolean()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error while checking direct subject: " + e.getMessage());
        }

        // ✅ Authorized successfully
        return null;
    }


    private Map<String, Object> defaultMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestId", java.util.UUID.randomUUID().toString());
        meta.put("timestamp", java.time.Instant.now().toString());
        return meta;
    }
}
