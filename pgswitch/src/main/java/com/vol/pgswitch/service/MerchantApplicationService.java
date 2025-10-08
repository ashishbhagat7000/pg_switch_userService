package com.vol.pgswitch.service;

import com.vol.pgswitch.dto.MerchantApplicationRequest;
import com.vol.pgswitch.dto.MerchantUpdateRequest;
import com.vol.pgswitch.model.ApplicationStatus;
import com.vol.pgswitch.model.KycDocumentEntity;
import com.vol.pgswitch.model.MerchantApplicationEntity;
import com.vol.pgswitch.repository.MerchantApplicationRepository;
import com.vol.pgswitch.repository.KycDocumentRepository;
import com.vol.pgswitch.service.crypto.CryptoService;
import com.vol.pgswitch.service.storage.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MerchantApplicationService - Core business logic for merchant registration
 * 
 * This service handles the complete merchant application submission process including:
 * - Data validation and mapping from DTO to entity
 * - Encryption of sensitive fields before database storage
 * - Secure file storage for KYC documents
 * - Transaction management for data consistency
 * 
 * SECURITY FEATURES:
 * - Encrypts sensitive data before database storage
 * - Handles file uploads securely outside web root
 * - Transactional operations for data consistency
 * - Input validation through DTO layer
 * 
 * ENCRYPTION FLOW:
 * - PAN, GSTIN, CIN numbers are encrypted
 * - Government ID numbers are encrypted
 * - Bank account numbers are encrypted
 * - All other fields stored in plain text for querying
 * 
 * COMPLIANCE:
 * - PCI-DSS compliant data handling
 * - DPDP Act compliant for personal data
 * - GDPR compliant for data protection
 * - RBI PA guidelines compliant for merchant onboarding
 * 
 * PROCESS FLOW:
 * 1. Receive validated DTO and files
 * 2. Map non-sensitive fields to entity
 * 3. Encrypt sensitive fields using CryptoService
 * 4. Store files securely using FileStorageService
 * 5. Create KYC document metadata entities
 * 6. Persist complete application in transaction
 */
@Service
public class MerchantApplicationService {

    private final MerchantApplicationRepository repository; // Data access layer
    private final KycDocumentRepository kycDocumentRepository; // KYC document persistence
    private final CryptoService cryptoService; // Encryption service
    private final FileStorageService fileStorageService; // File storage service

    /**
     * Constructor - Dependency injection for required services
     * 
     * @param repository Merchant application repository
     * @param cryptoService Encryption service for sensitive data
     * @param fileStorageService File storage service for KYC documents
     */
    public MerchantApplicationService(MerchantApplicationRepository repository,
                                      CryptoService cryptoService,
                                      FileStorageService fileStorageService,
                                      KycDocumentRepository kycDocumentRepository) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.fileStorageService = fileStorageService;
        this.kycDocumentRepository = kycDocumentRepository;
    }

    /**
     * Submits a complete merchant application with files
     * 
     * This method handles the complete merchant registration process:
     * 1. Maps validated DTO data to entity
     * 2. Encrypts sensitive fields (PAN, GSTIN, CIN, Government ID, Bank Account)
     * 3. Stores uploaded KYC files securely
     * 4. Creates document metadata entities
     * 5. Persists everything in a single transaction
     * 
     * @param request Validated merchant application data
     * @param files Uploaded KYC documents
     * @return Application ID of the created merchant application
     * @throws IOException if file storage fails
     */
    @Transactional
    public String submit(MerchantApplicationRequest request, List<MultipartFile> files) throws IOException {
        MerchantApplicationEntity entity = new MerchantApplicationEntity();
        
        // Map non-sensitive fields (stored in plain text for querying and reporting)
        entity.setLegalEntityName(request.getLegalEntityName());
        entity.setBrandName(request.getBrandName());
        entity.setBusinessType(request.getBusinessType());
        entity.setBusinessCategory(request.getBusinessCategory());
        entity.setIncorporationDate(request.getIncorporationDate());
        entity.setAddressLine1(request.getAddressLine1());
        entity.setAddressLine2(request.getAddressLine2());
        entity.setCity(request.getCity());
        entity.setState(request.getState());
        entity.setCountry(request.getCountry());
        entity.setPincode(request.getPincode());
        entity.setContactNumber(request.getContactNumber());
        entity.setContactEmail(request.getContactEmail());
        entity.setWebsiteUrl(request.getWebsiteUrl());

        // Map signatory information (non-sensitive fields)
        entity.setSignatoryFullName(request.getSignatoryFullName());
        entity.setSignatoryDob(request.getSignatoryDob());
        entity.setSignatoryDesignation(request.getSignatoryDesignation());
        entity.setSignatoryMobile(request.getSignatoryMobile());
        entity.setSignatoryEmail(request.getSignatoryEmail());
        entity.setSignatoryGovtIdType(request.getSignatoryGovtIdType());

        // Map bank account details (non-sensitive fields)
        entity.setIfscCode(request.getIfscCode());
        entity.setAccountHolderName(request.getAccountHolderName());
        entity.setBankName(request.getBankName());
        entity.setBranchName(request.getBranchName());
        entity.setAccountType(request.getAccountType());
        
        // Map transaction profile (all non-sensitive)
        entity.setGoodsOrServices(request.getGoodsOrServices());
        entity.setAverageTicketSizeInr(request.getAverageTicketSizeInr());
        entity.setExpectedMonthlyVolumeInr(request.getExpectedMonthlyVolumeInr());
        entity.setExpectedAnnualTurnoverInr(request.getExpectedAnnualTurnoverInr());
        entity.setRefundPolicyUrl(request.getRefundPolicyUrl());
        entity.setRiskCategory(request.getRiskCategory());

        // Encrypt sensitive fields before database storage
        entity.setBusinessPanEncrypted(cryptoService.encrypt(request.getBusinessPan()));
        entity.setGstinEncrypted(cryptoService.encrypt(request.getGstin()));
        entity.setCinEncrypted(cryptoService.encrypt(request.getCin()));
        entity.setSignatoryGovtIdNumberEncrypted(cryptoService.encrypt(request.getSignatoryGovtIdNumber()));
        entity.setAccountNumberEncrypted(cryptoService.encrypt(request.getAccountNumber()));

        // Set creation timestamp and initial status
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setStatus(ApplicationStatus.SUBMITTED);
        
        // Persist the application FIRST to ensure it has a non-null id for DBRef
        MerchantApplicationEntity savedApplication = repository.save(entity);

        // Handle KYC document uploads and create metadata entities referencing the saved application
        List<KycDocumentEntity> savedDocuments = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile mf = files.get(i);
                if (mf.isEmpty()) continue; // Skip empty files
                
                String type = (request.getKycDocumentTypes() != null && request.getKycDocumentTypes().size() > i)
                        ? request.getKycDocumentTypes().get(i)
                        : "unknown";

                Path stored = fileStorageService.storeKyc(type, mf);
                
                KycDocumentEntity doc = new KycDocumentEntity();
                doc.setApplication(savedApplication);
                doc.setDocumentType(type);
                doc.setStoredPath(stored.toString());
                doc.setOriginalFilename(mf.getOriginalFilename());
                doc.setSizeBytes(mf.getSize());
                doc.setContentType(mf.getContentType());

                savedDocuments.add(kycDocumentRepository.save(doc));
            }
        }

        // Attach saved documents to the application and update the application document
        savedApplication.setKycDocuments(savedDocuments);
        repository.save(savedApplication);

        return savedApplication.getId();
    }

    /**
     * Find merchant by ID
     */
    public Optional<MerchantApplicationEntity> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Find all merchants with pagination and search
     */
    public Page<MerchantApplicationEntity> findAllMerchants(Pageable pageable, String search) {
        if (search != null && !search.trim().isEmpty()) {
            return repository.findByLegalEntityNameContainingIgnoreCaseOrContactEmailContainingIgnoreCase(
                search.trim(), search.trim(), pageable);
        }
        return repository.findAll(pageable);
    }

    /**
     * Find all merchants for export (no pagination)
     */
    public List<MerchantApplicationEntity> findAllMerchantsForExport(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return repository.findByLegalEntityNameContainingIgnoreCaseOrContactEmailContainingIgnoreCase(
                search.trim(), search.trim());
        }
        return repository.findAll();
    }

    /**
     * Update merchant application
     */
    @Transactional
    public MerchantApplicationEntity updateMerchant(String id, MerchantUpdateRequest updateRequest) {
        MerchantApplicationEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found with ID: " + id));

        // Update only provided fields
        if (updateRequest.getLegalEntityName() != null) {
            entity.setLegalEntityName(updateRequest.getLegalEntityName());
        }
        if (updateRequest.getBrandName() != null) {
            entity.setBrandName(updateRequest.getBrandName());
        }
        if (updateRequest.getBusinessType() != null) {
            entity.setBusinessType(updateRequest.getBusinessType());
        }
        if (updateRequest.getBusinessCategory() != null) {
            entity.setBusinessCategory(updateRequest.getBusinessCategory());
        }
        if (updateRequest.getIncorporationDate() != null) {
            entity.setIncorporationDate(updateRequest.getIncorporationDate());
        }

        // Update sensitive fields if provided
        if (updateRequest.getBusinessPan() != null) {
            entity.setBusinessPanEncrypted(cryptoService.encrypt(updateRequest.getBusinessPan()));
        }
        if (updateRequest.getGstin() != null) {
            entity.setGstinEncrypted(cryptoService.encrypt(updateRequest.getGstin()));
        }
        if (updateRequest.getCin() != null) {
            entity.setCinEncrypted(cryptoService.encrypt(updateRequest.getCin()));
        }

        // Update address
        if (updateRequest.getAddressLine1() != null) {
            entity.setAddressLine1(updateRequest.getAddressLine1());
        }
        if (updateRequest.getAddressLine2() != null) {
            entity.setAddressLine2(updateRequest.getAddressLine2());
        }
        if (updateRequest.getCity() != null) {
            entity.setCity(updateRequest.getCity());
        }
        if (updateRequest.getState() != null) {
            entity.setState(updateRequest.getState());
        }
        if (updateRequest.getCountry() != null) {
            entity.setCountry(updateRequest.getCountry());
        }
        if (updateRequest.getPincode() != null) {
            entity.setPincode(updateRequest.getPincode());
        }

        // Update contacts
        if (updateRequest.getContactNumber() != null) {
            entity.setContactNumber(updateRequest.getContactNumber());
        }
        if (updateRequest.getContactEmail() != null) {
            entity.setContactEmail(updateRequest.getContactEmail());
        }
        if (updateRequest.getWebsiteUrl() != null) {
            entity.setWebsiteUrl(updateRequest.getWebsiteUrl());
        }

        // Update signatory information
        if (updateRequest.getSignatoryFullName() != null) {
            entity.setSignatoryFullName(updateRequest.getSignatoryFullName());
        }
        if (updateRequest.getSignatoryDob() != null) {
            entity.setSignatoryDob(updateRequest.getSignatoryDob());
        }
        if (updateRequest.getSignatoryDesignation() != null) {
            entity.setSignatoryDesignation(updateRequest.getSignatoryDesignation());
        }
        if (updateRequest.getSignatoryMobile() != null) {
            entity.setSignatoryMobile(updateRequest.getSignatoryMobile());
        }
        if (updateRequest.getSignatoryEmail() != null) {
            entity.setSignatoryEmail(updateRequest.getSignatoryEmail());
        }
        if (updateRequest.getSignatoryGovtIdType() != null) {
            entity.setSignatoryGovtIdType(updateRequest.getSignatoryGovtIdType());
        }
        if (updateRequest.getSignatoryGovtIdNumber() != null) {
            entity.setSignatoryGovtIdNumberEncrypted(cryptoService.encrypt(updateRequest.getSignatoryGovtIdNumber()));
        }

        // Update bank account details
        if (updateRequest.getAccountHolderName() != null) {
            entity.setAccountHolderName(updateRequest.getAccountHolderName());
        }
        if (updateRequest.getBankName() != null) {
            entity.setBankName(updateRequest.getBankName());
        }
        if (updateRequest.getBranchName() != null) {
            entity.setBranchName(updateRequest.getBranchName());
        }
        if (updateRequest.getAccountNumber() != null) {
            entity.setAccountNumberEncrypted(cryptoService.encrypt(updateRequest.getAccountNumber()));
        }
        if (updateRequest.getIfscCode() != null) {
            entity.setIfscCode(updateRequest.getIfscCode());
        }
        if (updateRequest.getAccountType() != null) {
            entity.setAccountType(updateRequest.getAccountType());
        }

        // Update transaction profile
        if (updateRequest.getGoodsOrServices() != null) {
            entity.setGoodsOrServices(updateRequest.getGoodsOrServices());
        }
        if (updateRequest.getAverageTicketSizeInr() != null) {
            entity.setAverageTicketSizeInr(updateRequest.getAverageTicketSizeInr());
        }
        if (updateRequest.getExpectedMonthlyVolumeInr() != null) {
            entity.setExpectedMonthlyVolumeInr(updateRequest.getExpectedMonthlyVolumeInr());
        }
        if (updateRequest.getExpectedAnnualTurnoverInr() != null) {
            entity.setExpectedAnnualTurnoverInr(updateRequest.getExpectedAnnualTurnoverInr());
        }
        if (updateRequest.getRefundPolicyUrl() != null) {
            entity.setRefundPolicyUrl(updateRequest.getRefundPolicyUrl());
        }
        if (updateRequest.getRiskCategory() != null) {
            entity.setRiskCategory(updateRequest.getRiskCategory());
        }

        return repository.save(entity);
    }

    /**
     * Delete merchant application
     */
    @Transactional
    public void deleteMerchant(String id) {
        MerchantApplicationEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found with ID: " + id));

        // Delete associated KYC documents
        kycDocumentRepository.deleteByApplication_Id(id);

        // Delete the merchant application
        repository.delete(entity);
    }
}


