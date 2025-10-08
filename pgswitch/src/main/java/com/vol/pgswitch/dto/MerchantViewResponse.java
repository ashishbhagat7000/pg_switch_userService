package com.vol.pgswitch.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * MerchantViewResponse - DTO for viewing merchant application details
 * 
 * This DTO provides a safe view of merchant data with masked sensitive fields.
 * Sensitive data is decrypted and masked for display purposes.
 */
public class MerchantViewResponse {

    private String id;
    private String legalEntityName;
    private String brandName;
    private String businessType;
    private String businessCategory;
    private LocalDate incorporationDate;
    
    // Masked sensitive fields
    private String businessPan; // Masked: ******1234F
    private String gstin; // Masked: *************1234
    private String cin; // Masked
    
    // Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pincode;
    
    // Contacts
    private String contactNumber;
    private String contactEmail;
    private String websiteUrl;
    
    // Authorized Signatory
    private String signatoryFullName;
    private LocalDate signatoryDob;
    private String signatoryDesignation;
    private String signatoryMobile;
    private String signatoryEmail;
    private String signatoryGovtIdType;
    private String signatoryGovtIdNumber; // Masked
    
    // Bank Account Details
    private String accountHolderName;
    private String bankName;
    private String branchName;
    private String accountNumber; // Masked
    private String ifscCode;
    private String accountType;
    
    // Transaction Profile
    private String goodsOrServices;
    private Long averageTicketSizeInr;
    private Long expectedMonthlyVolumeInr;
    private Long expectedAnnualTurnoverInr;
    private String refundPolicyUrl;
    private String riskCategory;
    
    // KYC Documents
    private List<KycDocumentView> kycDocuments;
    
    // Audit fields
    private Long createdAt;
    private String status; // Application status (enum as string for JSON serialization)

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLegalEntityName() { return legalEntityName; }
    public void setLegalEntityName(String legalEntityName) { this.legalEntityName = legalEntityName; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }
    public LocalDate getIncorporationDate() { return incorporationDate; }
    public void setIncorporationDate(LocalDate incorporationDate) { this.incorporationDate = incorporationDate; }
    public String getBusinessPan() { return businessPan; }
    public void setBusinessPan(String businessPan) { this.businessPan = businessPan; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getSignatoryFullName() { return signatoryFullName; }
    public void setSignatoryFullName(String signatoryFullName) { this.signatoryFullName = signatoryFullName; }
    public LocalDate getSignatoryDob() { return signatoryDob; }
    public void setSignatoryDob(LocalDate signatoryDob) { this.signatoryDob = signatoryDob; }
    public String getSignatoryDesignation() { return signatoryDesignation; }
    public void setSignatoryDesignation(String signatoryDesignation) { this.signatoryDesignation = signatoryDesignation; }
    public String getSignatoryMobile() { return signatoryMobile; }
    public void setSignatoryMobile(String signatoryMobile) { this.signatoryMobile = signatoryMobile; }
    public String getSignatoryEmail() { return signatoryEmail; }
    public void setSignatoryEmail(String signatoryEmail) { this.signatoryEmail = signatoryEmail; }
    public String getSignatoryGovtIdType() { return signatoryGovtIdType; }
    public void setSignatoryGovtIdType(String signatoryGovtIdType) { this.signatoryGovtIdType = signatoryGovtIdType; }
    public String getSignatoryGovtIdNumber() { return signatoryGovtIdNumber; }
    public void setSignatoryGovtIdNumber(String signatoryGovtIdNumber) { this.signatoryGovtIdNumber = signatoryGovtIdNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getGoodsOrServices() { return goodsOrServices; }
    public void setGoodsOrServices(String goodsOrServices) { this.goodsOrServices = goodsOrServices; }
    public Long getAverageTicketSizeInr() { return averageTicketSizeInr; }
    public void setAverageTicketSizeInr(Long averageTicketSizeInr) { this.averageTicketSizeInr = averageTicketSizeInr; }
    public Long getExpectedMonthlyVolumeInr() { return expectedMonthlyVolumeInr; }
    public void setExpectedMonthlyVolumeInr(Long expectedMonthlyVolumeInr) { this.expectedMonthlyVolumeInr = expectedMonthlyVolumeInr; }
    public Long getExpectedAnnualTurnoverInr() { return expectedAnnualTurnoverInr; }
    public void setExpectedAnnualTurnoverInr(Long expectedAnnualTurnoverInr) { this.expectedAnnualTurnoverInr = expectedAnnualTurnoverInr; }
    public String getRefundPolicyUrl() { return refundPolicyUrl; }
    public void setRefundPolicyUrl(String refundPolicyUrl) { this.refundPolicyUrl = refundPolicyUrl; }
    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    public List<KycDocumentView> getKycDocuments() { return kycDocuments; }
    public void setKycDocuments(List<KycDocumentView> kycDocuments) { this.kycDocuments = kycDocuments; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Inner class for KYC document view
    public static class KycDocumentView {
        private String id;
        private String documentType;
        private String originalFilename;
        private Long sizeBytes;
        private String contentType;
        private String downloadUrl; // URL to download the document

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        public Long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }
}
