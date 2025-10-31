package com.vol.pgswitch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vol.pgswitch.dto.*;
import com.vol.pgswitch.model.MerchantApplicationEntity;
import com.vol.pgswitch.service.MerchantApplicationService;
import com.vol.pgswitch.service.crypto.CryptoService;
import com.vol.pgswitch.service.keto.KetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@RestController
@RequestMapping("/api/super-merchant/merchants")
public class MerchantController {

    private final KetoService ketoService;

    private final MerchantApplicationService merchantApplicationService;
    private final CryptoService cryptoService;

    public MerchantController(KetoService ketoService, MerchantApplicationService merchantApplicationService,
                              CryptoService cryptoService) {

        this.ketoService = ketoService;
        this.merchantApplicationService = merchantApplicationService;
        this.cryptoService = cryptoService;
    }
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(
            @RequestPart("data") String requestJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {

        // 🔒 Step 1: Check authorization first
        ResponseEntity<?> auth = authorizeSupermerchant();
        if (auth != null) {
            // If unauthorized, return immediately (401, 403, 503)
            return (ResponseEntity<ApiResponse<Map<String, Object>>>) auth;
        }

        // 🔧 Step 2: Proceed with normal submit flow
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MerchantApplicationRequest request = objectMapper.readValue(requestJson, MerchantApplicationRequest.class);

            Long id = merchantApplicationService.submit(request, files);
            Map<String, Object> data = Map.of("applicationId", id);

            return ResponseEntity.ok(
                    new ApiResponse<>("success", "APPLICATION_SUBMITTED", "Application submitted.", data, defaultMeta())
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid submission payload", e);
        }
    }


    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<MerchantApplicationEntity> merchants = merchantApplicationService.findAllMerchants(pageable, search);
        List<MerchantViewResponse> merchantViews = merchants.getContent().stream()
                .map(this::toView)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("merchants", merchantViews);
        data.put("currentPage", merchants.getNumber());
        data.put("totalItems", merchants.getTotalElements());
        data.put("totalPages", merchants.getTotalPages());
        data.put("hasNext", merchants.hasNext());
        data.put("hasPrevious", merchants.hasPrevious());
        return ResponseEntity.ok(new ApiResponse<>("success", "MERCHANT_LIST", "Merchants fetched.", data, defaultMeta()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantViewResponse>> get(@PathVariable Long id) {
        MerchantApplicationEntity m = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        return ResponseEntity.ok(new ApiResponse<>("success", "MERCHANT_DETAILS", "Merchant fetched.", toView(m), defaultMeta()));
    }

    @GetMapping("/{id}/decrypted")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDecrypted(
            @PathVariable Long id,
            @RequestParam(defaultValue = "master") String mode,
            @RequestParam(required = false) String userId) {
        MerchantApplicationEntity e = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", e.getId());
        out.put("legalEntityName", e.getLegalEntityName());
        out.put("status", e.getStatus() != null ? e.getStatus().name() : "UNKNOWN");
        out.put("createdAt", e.getCreatedAt());
        out.put("businessPan", cryptoService.decrypt(e.getBusinessPanEncrypted()));
        out.put("gstin", cryptoService.decrypt(e.getGstinEncrypted()));
        out.put("cin", cryptoService.decrypt(e.getCinEncrypted()));
        out.put("signatoryGovtIdNumber", cryptoService.decrypt(e.getSignatoryGovtIdNumberEncrypted()));
        out.put("accountNumber", cryptoService.decrypt(e.getAccountNumberEncrypted()));
        return ResponseEntity.ok(new ApiResponse<>("success", "MERCHANT_DECRYPTED", "Merchant decrypted.", out, defaultMeta()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable Long id,
                                                                   @RequestBody MerchantUpdateRequest updateRequest) {
        try {
            MerchantApplicationEntity updated = merchantApplicationService.updateMerchant(id, updateRequest);
            Map<String, Object> data = new HashMap<>();
            data.put("merchantId", updated.getId());
            data.put("updatedAt", System.currentTimeMillis());
            return ResponseEntity.ok(new ApiResponse<>("success", "MERCHANT_UPDATED", "Merchant updated.", data, defaultMeta()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Long id) {
        try {
            merchantApplicationService.deleteMerchant(id);
            Map<String, Object> data = new HashMap<>();
            data.put("merchantId", id);
            data.put("deletedAt", System.currentTimeMillis());
            return ResponseEntity.ok(new ApiResponse<>("success", "MERCHANT_DELETED", "Merchant deleted.", data, defaultMeta()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping("/{id}/kyc-documents")
    public ResponseEntity<ApiResponse<Map<String, Object>>> kyc(@PathVariable Long id) {
        MerchantApplicationEntity merchant = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        List<MerchantViewResponse.KycDocumentView> kycDocs = merchant.getKycDocuments().stream()
                .map(doc -> {
                    MerchantViewResponse.KycDocumentView view = new MerchantViewResponse.KycDocumentView();
                    view.setId(doc.getId());
                    view.setDocumentType(doc.getDocumentType());
                    view.setOriginalFilename(doc.getOriginalFilename());
                    view.setSizeBytes(doc.getSizeBytes());
                    view.setContentType(doc.getContentType());
                    view.setDownloadUrl("/api/super-merchant/merchants/" + id + "/kyc-documents/" + doc.getId() + "/download");
                    return view;
                })
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("merchantId", id);
        data.put("merchantName", merchant.getLegalEntityName());
        data.put("kycDocuments", kycDocs);
        data.put("totalDocuments", kycDocs.size());
        return ResponseEntity.ok(new ApiResponse<>("success", "MERCHANT_KYC", "Merchant KYC fetched.", data, defaultMeta()));
    }

    @GetMapping("/{merchantId}/kyc-documents/{documentId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String merchantId, @PathVariable String documentId) {
        byte[] contentBytes = ("KYC Document content for merchant: " + merchantId + ", document: " + documentId).getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "kyc-document-" + documentId + ".pdf");
        return new ResponseEntity<>(contentBytes, headers, HttpStatus.OK);
    }

    private MerchantViewResponse toView(MerchantApplicationEntity entity) {
        MerchantViewResponse response = new MerchantViewResponse();
        response.setId(entity.getId());
        response.setLegalEntityName(entity.getLegalEntityName());
        response.setBrandName(entity.getBrandName());
        response.setBusinessType(entity.getBusinessType());
        response.setBusinessCategory(entity.getBusinessCategory());
        response.setIncorporationDate(entity.getIncorporationDate());
        response.setBusinessPan(mask(cryptoService.decrypt(entity.getBusinessPanEncrypted()), 4));
        response.setGstin(mask(cryptoService.decrypt(entity.getGstinEncrypted()), 4));
        response.setCin(mask(cryptoService.decrypt(entity.getCinEncrypted()), 4));
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
        response.setSignatoryGovtIdNumber(mask(cryptoService.decrypt(entity.getSignatoryGovtIdNumberEncrypted()), 4));
        response.setAccountHolderName(entity.getAccountHolderName());
        response.setBankName(entity.getBankName());
        response.setBranchName(entity.getBranchName());
        response.setAccountNumber(mask(cryptoService.decrypt(entity.getAccountNumberEncrypted()), 4));
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
                    view.setDownloadUrl("/api/super-merchant/merchants/" + entity.getId() + "/kyc-documents/" + doc.getId() + "/download");
                    return view;
                })
                .collect(Collectors.toList());
        response.setKycDocuments(kycDocs);
        response.setCreatedAt(entity.getCreatedAt());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : "UNKNOWN");
        return response;
    }

    private String mask(String data, int visible) {
        if (data == null || data.length() <= visible) return data;
        return "*".repeat(data.length() - visible) + data.substring(data.length() - visible);
    }

    private Map<String, Object> defaultMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestId", java.util.UUID.randomUUID().toString());
        meta.put("timestamp", java.time.Instant.now().toString());
        return meta;
    }

    // Helper: returns null when authorized, or a ResponseEntity to return to caller (401/403/503)
    private ResponseEntity<?> authorizeSupermerchant() {
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

}
