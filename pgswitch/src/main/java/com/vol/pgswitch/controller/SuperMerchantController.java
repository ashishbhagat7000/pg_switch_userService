package com.vol.pgswitch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vol.pgswitch.dto.ApiResponse;
import com.vol.pgswitch.dto.MerchantApplicationRequest;
import com.vol.pgswitch.dto.MerchantUpdateRequest;
import com.vol.pgswitch.dto.MerchantViewResponse;
import com.vol.pgswitch.model.MerchantApplicationEntity;
import com.vol.pgswitch.service.MerchantApplicationService;
import com.vol.pgswitch.service.crypto.CryptoService;
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
@RequestMapping("/api/super-merchant/merchants")
public class SuperMerchantController {

    private final MerchantApplicationService merchantApplicationService;
    private final CryptoService cryptoService;

    public SuperMerchantController(MerchantApplicationService merchantApplicationService,
                                   CryptoService cryptoService) {
        this.merchantApplicationService = merchantApplicationService;
        this.cryptoService = cryptoService;
    }

    @PostMapping(value = "/submit", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(
            @RequestPart("data") String requestJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MerchantApplicationRequest request = objectMapper.readValue(requestJson, MerchantApplicationRequest.class);
            String id = merchantApplicationService.submit(request, files);
            Map<String, Object> data = Map.of("applicationId", id);
            return ResponseEntity.ok(new ApiResponse<>("success", "APPLICATION_SUBMITTED", "Application submitted.", data, defaultMeta()));
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
    public ResponseEntity<ApiResponse<MerchantViewResponse>> get(@PathVariable String id) {
        MerchantApplicationEntity m = merchantApplicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Merchant not found with ID: " + id));
        return ResponseEntity.ok(new ApiResponse<>("success", "MERCHANT_DETAILS", "Merchant fetched.", toView(m), defaultMeta()));
    }

    @GetMapping("/{id}/decrypted")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDecrypted(
            @PathVariable String id,
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable String id,
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable String id) {
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> kyc(@PathVariable String id) {
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
}
