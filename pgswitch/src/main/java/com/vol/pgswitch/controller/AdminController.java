package com.vol.pgswitch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vol.pgswitch.dto.MerchantApplicationRequest;
import com.vol.pgswitch.dto.MerchantUpdateRequest;
import com.vol.pgswitch.dto.MerchantViewResponse;
import com.vol.pgswitch.model.MerchantApplicationEntity;
import com.vol.pgswitch.service.MerchantApplicationService;
import com.vol.pgswitch.service.crypto.CryptoService;
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

    private final MerchantApplicationService merchantApplicationService;
    private final CryptoService cryptoService;

    public AdminController(MerchantApplicationService merchantApplicationService,
                           CryptoService cryptoService) {
        this.merchantApplicationService = merchantApplicationService;
        this.cryptoService = cryptoService;
    }

    @Operation(summary = "Submit Merchant Application")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application submitted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"applicationId\": \"507f1f77bcf86cd799439011\"}")))
    })
    @PostMapping(value = "/submit", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> submitMerchantApplication(
            @Parameter(description = "Merchant application data as JSON string", required = true)
            @RequestPart("data") String requestJson,
            @Parameter(description = "KYC documents (PAN, GST Certificate, etc.)", required = false)
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MerchantApplicationRequest request = objectMapper.readValue(requestJson, MerchantApplicationRequest.class);
            String id = merchantApplicationService.submit(request, files);
            return ResponseEntity.ok(Map.of("applicationId", id));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid submission payload", e);
        }
    }

    @Operation(summary = "Get all merchants", description = "Retrieve paginated list of all merchant applications")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<MerchantApplicationEntity> merchants = merchantApplicationService.findAllMerchants(pageable, search);
        List<MerchantViewResponse> merchantViews = merchants.getContent().stream()
                .map(this::convertToViewResponse)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("merchants", merchantViews);
        response.put("currentPage", merchants.getNumber());
        response.put("totalItems", merchants.getTotalElements());
        response.put("totalPages", merchants.getTotalPages());
        response.put("hasNext", merchants.hasNext());
        response.put("hasPrevious", merchants.hasPrevious());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get merchant by ID", description = "Retrieve detailed information about a specific merchant")
    @GetMapping("/{id}")
    public ResponseEntity<MerchantViewResponse> getMerchantById(@PathVariable String id) {
        MerchantApplicationEntity merchant = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        return ResponseEntity.ok(convertToViewResponse(merchant));
    }

    @Operation(summary = "Get decrypted merchant by ID", description = "Return decrypted fields using master key")
    @GetMapping("/{id}/decrypted")
    public ResponseEntity<Map<String, Object>> getMerchantByIdDecrypted(
            @PathVariable String id,
            @RequestParam(defaultValue = "master") String mode,
            @RequestParam(required = false) String userId) {
        MerchantApplicationEntity entity = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
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

    @Operation(summary = "Update merchant", description = "Update merchant application details")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMerchant(
            @PathVariable String id,
            @Valid @RequestBody MerchantUpdateRequest updateRequest) {
        try {
            MerchantApplicationEntity updatedMerchant = merchantApplicationService.updateMerchant(id, updateRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Merchant updated successfully");
            response.put("merchantId", updatedMerchant.getId());
            response.put("updatedAt", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Operation(summary = "Delete merchant", description = "Delete merchant application and associated KYC documents")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable String id) {
        try {
            merchantApplicationService.deleteMerchant(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Merchant deleted successfully");
            response.put("merchantId", id);
            response.put("deletedAt", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Operation(summary = "Get merchant KYC documents", description = "Retrieve all KYC documents for a merchant")
    @GetMapping("/{id}/kyc-documents")
    public ResponseEntity<Map<String, Object>> getMerchantKycDocuments(@PathVariable String id) {
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
                    view.setDownloadUrl("/api/admin/merchants/" + id + "/kyc-documents/" + doc.getId() + "/download");
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

    @Operation(summary = "Download KYC document", description = "Download a specific KYC document")
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

    @Operation(summary = "Export merchants to CSV", description = "Export merchant list to CSV format")
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportMerchantsToCsv(@RequestParam(required = false) String search) {
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

    @Operation(summary = "Export merchants to PDF", description = "Export merchant list to PDF format")
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportMerchantsToPdf(@RequestParam(required = false) String search) {
        String pdfContent = "PDF Export of Merchants\n\nSearch: " + (search != null ? search : "All") + "\n\n";
        pdfContent += "This would contain the actual PDF content with merchant data.\n";
        pdfContent += "Generated at: " + new Date();
        byte[] pdfBytes = pdfContent.getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "merchants-export-" + System.currentTimeMillis() + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
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
        List<MerchantViewResponse.KycDocumentView> kycDocs = entity.getKycDocuments().stream()
                .map(doc -> {
                    MerchantViewResponse.KycDocumentView view = new MerchantViewResponse.KycDocumentView();
                    view.setId(doc.getId());
                    view.setDocumentType(doc.getDocumentType());
                    view.setOriginalFilename(doc.getOriginalFilename());
                    view.setSizeBytes(doc.getSizeBytes());
                    view.setContentType(doc.getContentType());
                    view.setDownloadUrl("/api/admin/merchants/" + entity.getId() + "/kyc-documents/" + doc.getId() + "/download");
                    return view;
                })
                .collect(Collectors.toList());
        response.setKycDocuments(kycDocs);
        response.setCreatedAt(entity.getCreatedAt());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : "UNKNOWN");
        return response;
    }

    private String maskSensitiveData(String data, int visibleChars) {
        if (data == null || data.length() <= visibleChars) {
            return data;
        }
        return "*".repeat(data.length() - visibleChars) + data.substring(data.length() - visibleChars);
    }
}
