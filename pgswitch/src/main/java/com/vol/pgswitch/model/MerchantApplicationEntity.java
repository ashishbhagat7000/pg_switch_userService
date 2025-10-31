package com.vol.pgswitch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MerchantApplicationEntity - Main document for storing merchant registration data
 * <p>
 * This document represents a complete merchant application with all required information
 * as per RBI Payment Aggregator guidelines. It includes:
 * - Business/Entity Information (legal name, type, category, etc.)
 * - Authorized Signatory details (KYC information)
 * - Bank Account Details for settlement
 * - Transaction Profile (business volume, risk category)
 * - KYC Document references
 * <p>
 * SECURITY FEATURES:
 * - Sensitive fields are encrypted-at-rest using AES-GCM encryption
 * - Encrypted fields: PAN, GSTIN, CIN, Government ID numbers, Bank Account numbers
 * - Non-sensitive fields stored in plain text for querying and reporting
 * <p>
 * COMPLIANCE:
 * - Follows PCI-DSS requirements for sensitive data protection
 * - Implements DPDP Act requirements for personal data protection
 * - GDPR compliant with proper data handling
 * - RBI PA guidelines compliance for merchant onboarding
 * <p>
 * MONGODB FEATURES:
 * - Uses MongoDB document structure for flexible schema
 * - DBRef for referencing KYC documents
 * - Indexed fields for efficient querying
 * - Embedded documents for related data
 */
@Entity
@Table(name = "merchant_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Uses PostgreSQL auto-increment feature (SERIAL/BIGSERIAL)
    private Long id; // Changed type from String to Long, standard for JPA IDs in relational databases

    // 1. Business / Entity Information
    @NotBlank(message = "Legal entity name is required")
    @Size(max = 255, message = "Legal entity name must not exceed 255 characters")
    @Column(name = "legal_entity_name", length = 255, nullable = false)
    private String legalEntityName;

    @Size(max = 255, message = "Brand name must not exceed 255 characters")
    @Column(name = "brand_name", length = 255)
    private String brandName;

    @NotBlank(message = "Business type is required")
    @Size(max = 100, message = "Business type must not exceed 100 characters")
    @Column(name = "business_type", length = 100, nullable = false)
    private String businessType;

    @Size(max = 255, message = "Business category must not exceed 255 characters")
    @Column(name = "business_category", length = 255)
    private String businessCategory;

    @PastOrPresent(message = "Incorporation date must be in the past or present")
    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    // Sensitive IDs stored encrypted-at-rest
    @Size(max = 500, message = "Encrypted PAN must not exceed 500 characters")
    @Column(name = "business_pan_encrypted", length = 500)
    private String businessPanEncrypted;

    @Size(max = 500, message = "Encrypted GSTIN must not exceed 500 characters")
    @Column(name = "gstin_encrypted", length = 500)
    private String gstinEncrypted;

    @Size(max = 500, message = "Encrypted CIN must not exceed 500 characters")
    @Column(name = "cin_encrypted", length = 500)
    private String cinEncrypted;

    // Address
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Column(name = "address_line_1", length = 255, nullable = false)
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Column(name = "state", length = 100, nullable = false)
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Column(name = "country", length = 100, nullable = false)
    private String country;

    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits")
    @Column(name = "pincode", length = 6)
    private String pincode;

    // Contacts
    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid contact number format")
    @Column(name = "contact_number", length = 15, nullable = false)
    private String contactNumber;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Contact email is required")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    @Column(name = "contact_email", length = 255, nullable = false)
    private String contactEmail;

    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    // 2. Authorized Signatory (KYC Information)
    @NotBlank(message = "Signatory full name is required")
    @Size(max = 255, message = "Signatory full name must not exceed 255 characters")
    @Column(name = "signatory_full_name", length = 255, nullable = false)
    private String signatoryFullName;

    @Past(message = "Signatory date of birth must be in the past")
    @Column(name = "signatory_dob")
    private LocalDate signatoryDob;

    @NotBlank(message = "Signatory designation is required")
    @Size(max = 100, message = "Signatory designation must not exceed 100 characters")
    @Column(name = "signatory_designation", length = 100, nullable = false)
    private String signatoryDesignation;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid signatory mobile format")
    @Column(name = "signatory_mobile", length = 15)
    private String signatoryMobile;

    @Email(message = "Invalid signatory email format")
    @NotBlank(message = "Signatory email is required")
    @Size(max = 255, message = "Signatory email must not exceed 255 characters")
    @Column(name = "signatory_email", length = 255, nullable = false)
    private String signatoryEmail;

    @NotBlank(message = "Signatory government ID type is required")
    @Size(max = 50, message = "Signatory government ID type must not exceed 50 characters")
    @Column(name = "signatory_govt_id_type", length = 50, nullable = false)
    private String signatoryGovtIdType;

    @Size(max = 500, message = "Encrypted signatory government ID must not exceed 500 characters")
    @Column(name = "signatory_govt_id_number_encrypted", length = 500)
    private String signatoryGovtIdNumberEncrypted;

    // 3. Bank Account Details (Settlement Account)
    @NotBlank(message = "Account holder name is required")
    @Size(max = 255, message = "Account holder name must not exceed 255 characters")
    @Column(name = "account_holder_name", length = 255, nullable = false)
    private String accountHolderName;

    @NotBlank(message = "Bank name is required")
    @Size(max = 255, message = "Bank name must not exceed 255 characters")
    @Column(name = "bank_name", length = 255, nullable = false)
    private String bankName;

    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Size(max = 500, message = "Encrypted account number must not exceed 500 characters")
    @Column(name = "account_number_encrypted", length = 500)
    private String accountNumberEncrypted;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    @NotBlank(message = "Account type is required")
    @Size(max = 50, message = "Account type must not exceed 50 characters")
    @Column(name = "account_type", length = 50, nullable = false)
    private String accountType;

    // 4. Transaction Profile
    @NotBlank(message = "Goods or services description is required")
    @Size(max = 500, message = "Goods or services description must not exceed 500 characters")
    @Column(name = "goods_or_services", length = 500, nullable = false)
    private String goodsOrServices;

    @PositiveOrZero(message = "Average ticket size must be positive or zero")
    @Column(name = "average_ticket_size_inr")
    private Long averageTicketSizeInr;

    @PositiveOrZero(message = "Expected monthly volume must be positive or zero")
    @Column(name = "expected_monthly_volume_inr")
    private Long expectedMonthlyVolumeInr;

    @PositiveOrZero(message = "Expected annual turnover must be positive or zero")
    @Column(name = "expected_annual_turnover_inr")
    private Long expectedAnnualTurnoverInr;

    @Size(max = 500, message = "Refund policy URL must not exceed 500 characters")
    @Column(name = "refund_policy_url", length = 500)
    private String refundPolicyUrl;

    @Size(max = 50, message = "Risk category must not exceed 50 characters")
    @Column(name = "risk_category", length = 50)
    private String riskCategory;

    // KYC files metadata - JPA relationship (Replaced @DBRef)
    // Assuming KycDocumentEntity has a field 'merchantApplication' pointing back to this entity.
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KycDocumentEntity> kycDocuments = new ArrayList<>();

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = true)
    private Long updatedAt = System.currentTimeMillis();

    // ------------------- Approval / Rejection Tracking -------------------
    @Column(name = "approved_at")
    private Long approvedAt;

    @Column(name = "rejected_at")
    private Long rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Enumerated(EnumType.STRING) // Store enum name as string in the database
    @Column(name = "status", length = 50, nullable = false)
    private ApplicationStatus status;
    // ------------------- Lifecycle Hooks -------------------
    @PrePersist
    public void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getApprovedAt() {
        return approvedAt;
    }

    public Long getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Long rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setApprovedAt(Long approvedAt) {
        this.approvedAt = approvedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    // ------------------- Convenience Methods -------------------
    public void setApprovedNow() {
        this.approvedAt = System.currentTimeMillis();
        this.status = ApplicationStatus.APPROVED;
    }

    public void setRejectedNow(String reason) {
        this.rejectedAt = System.currentTimeMillis();
        this.rejectionReason = reason;
        this.status = ApplicationStatus.REJECTED;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessCategory() {
        return businessCategory;
    }

    public void setBusinessCategory(String businessCategory) {
        this.businessCategory = businessCategory;
    }

    public LocalDate getIncorporationDate() {
        return incorporationDate;
    }

    public void setIncorporationDate(LocalDate incorporationDate) {
        this.incorporationDate = incorporationDate;
    }

    public String getBusinessPanEncrypted() {
        return businessPanEncrypted;
    }

    public void setBusinessPanEncrypted(String businessPanEncrypted) {
        this.businessPanEncrypted = businessPanEncrypted;
    }

    public String getGstinEncrypted() {
        return gstinEncrypted;
    }

    public void setGstinEncrypted(String gstinEncrypted) {
        this.gstinEncrypted = gstinEncrypted;
    }

    public String getCinEncrypted() {
        return cinEncrypted;
    }

    public void setCinEncrypted(String cinEncrypted) {
        this.cinEncrypted = cinEncrypted;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getSignatoryFullName() {
        return signatoryFullName;
    }

    public void setSignatoryFullName(String signatoryFullName) {
        this.signatoryFullName = signatoryFullName;
    }

    public LocalDate getSignatoryDob() {
        return signatoryDob;
    }

    public void setSignatoryDob(LocalDate signatoryDob) {
        this.signatoryDob = signatoryDob;
    }

    public String getSignatoryDesignation() {
        return signatoryDesignation;
    }

    public void setSignatoryDesignation(String signatoryDesignation) {
        this.signatoryDesignation = signatoryDesignation;
    }

    public String getSignatoryMobile() {
        return signatoryMobile;
    }

    public void setSignatoryMobile(String signatoryMobile) {
        this.signatoryMobile = signatoryMobile;
    }

    public String getSignatoryEmail() {
        return signatoryEmail;
    }

    public void setSignatoryEmail(String signatoryEmail) {
        this.signatoryEmail = signatoryEmail;
    }

    public String getSignatoryGovtIdType() {
        return signatoryGovtIdType;
    }

    public void setSignatoryGovtIdType(String signatoryGovtIdType) {
        this.signatoryGovtIdType = signatoryGovtIdType;
    }

    public String getSignatoryGovtIdNumberEncrypted() {
        return signatoryGovtIdNumberEncrypted;
    }

    public void setSignatoryGovtIdNumberEncrypted(String signatoryGovtIdNumberEncrypted) {
        this.signatoryGovtIdNumberEncrypted = signatoryGovtIdNumberEncrypted;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAccountNumberEncrypted() {
        return accountNumberEncrypted;
    }

    public void setAccountNumberEncrypted(String accountNumberEncrypted) {
        this.accountNumberEncrypted = accountNumberEncrypted;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getGoodsOrServices() {
        return goodsOrServices;
    }

    public void setGoodsOrServices(String goodsOrServices) {
        this.goodsOrServices = goodsOrServices;
    }

    public Long getAverageTicketSizeInr() {
        return averageTicketSizeInr;
    }

    public void setAverageTicketSizeInr(Long averageTicketSizeInr) {
        this.averageTicketSizeInr = averageTicketSizeInr;
    }

    public Long getExpectedMonthlyVolumeInr() {
        return expectedMonthlyVolumeInr;
    }

    public void setExpectedMonthlyVolumeInr(Long expectedMonthlyVolumeInr) {
        this.expectedMonthlyVolumeInr = expectedMonthlyVolumeInr;
    }

    public Long getExpectedAnnualTurnoverInr() {
        return expectedAnnualTurnoverInr;
    }

    public void setExpectedAnnualTurnoverInr(Long expectedAnnualTurnoverInr) {
        this.expectedAnnualTurnoverInr = expectedAnnualTurnoverInr;
    }

    public String getRefundPolicyUrl() {
        return refundPolicyUrl;
    }

    public void setRefundPolicyUrl(String refundPolicyUrl) {
        this.refundPolicyUrl = refundPolicyUrl;
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }

    public List<KycDocumentEntity> getKycDocuments() {
        return kycDocuments;
    }

    public void setKycDocuments(List<KycDocumentEntity> kycDocuments) {
        this.kycDocuments = kycDocuments;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
    // endregion
}