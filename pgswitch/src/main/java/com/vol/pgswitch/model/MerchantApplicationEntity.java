package com.vol.pgswitch.model;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MerchantApplicationEntity - Main document for storing merchant registration data
 * 
 * This document represents a complete merchant application with all required information
 * as per RBI Payment Aggregator guidelines. It includes:
 * - Business/Entity Information (legal name, type, category, etc.)
 * - Authorized Signatory details (KYC information)
 * - Bank Account Details for settlement
 * - Transaction Profile (business volume, risk category)
 * - KYC Document references
 * 
 * SECURITY FEATURES:
 * - Sensitive fields are encrypted-at-rest using AES-GCM encryption
 * - Encrypted fields: PAN, GSTIN, CIN, Government ID numbers, Bank Account numbers
 * - Non-sensitive fields stored in plain text for querying and reporting
 * 
 * COMPLIANCE:
 * - Follows PCI-DSS requirements for sensitive data protection
 * - Implements DPDP Act requirements for personal data protection
 * - GDPR compliant with proper data handling
 * - RBI PA guidelines compliance for merchant onboarding
 * 
 * MONGODB FEATURES:
 * - Uses MongoDB document structure for flexible schema
 * - DBRef for referencing KYC documents
 * - Indexed fields for efficient querying
 * - Embedded documents for related data
 */
@Document(collection = "merchant_applications")
public class MerchantApplicationEntity {

    @Id
    private String id;

    // 1. Business / Entity Information
    @NotBlank(message = "Legal entity name is required")
    @Size(max = 255, message = "Legal entity name must not exceed 255 characters")
    @Field("legal_entity_name")
    private String legalEntityName; // Plain text - used for queries and reporting
    
    @Size(max = 255, message = "Brand name must not exceed 255 characters")
    @Field("brand_name")
    private String brandName; // Plain text - optional field
    
    @NotBlank(message = "Business type is required")
    @Size(max = 100, message = "Business type must not exceed 100 characters")
    @Field("business_type")
    private String businessType; // Plain text - used for categorization
    
    @Size(max = 255, message = "Business category must not exceed 255 characters")
    @Field("business_category")
    private String businessCategory; // Plain text - industry classification
    
    @PastOrPresent(message = "Incorporation date must be in the past or present")
    @Field("incorporation_date")
    private LocalDate incorporationDate; // Plain text - date field
    
    // Sensitive IDs stored encrypted-at-rest for PCI-DSS compliance
    @Size(max = 500, message = "Encrypted PAN must not exceed 500 characters")
    @Field("business_pan_encrypted")
    private String businessPanEncrypted; // ENCRYPTED - PAN number (10 chars -> ~200 chars encrypted)
    
    @Size(max = 500, message = "Encrypted GSTIN must not exceed 500 characters")
    @Field("gstin_encrypted")
    private String gstinEncrypted; // ENCRYPTED - GSTIN (15 chars -> ~200 chars encrypted)
    
    @Size(max = 500, message = "Encrypted CIN must not exceed 500 characters")
    @Field("cin_encrypted")
    private String cinEncrypted; // ENCRYPTED - Company Incorporation Number

    // Address
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Field("address_line_1")
    private String addressLine1; // Plain text - address information
    
    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    @Field("address_line_2")
    private String addressLine2; // Plain text - optional address line
    
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Field("city")
    private String city; // Plain text - city name
    
    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Field("state")
    private String state; // Plain text - state name
    
    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Field("country")
    private String country; // Plain text - country name
    
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits")
    @Field("pincode")
    private String pincode; // Plain text - postal code

    // Contacts
    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid contact number format")
    @Field("contact_number")
    private String contactNumber; // Plain text - phone number
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Contact email is required")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    @Field("contact_email")
    private String contactEmail; // Plain text - email address
    
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    @Field("website_url")
    private String websiteUrl; // Plain text - website URL

    // 2. Authorized Signatory (KYC Information as per RBI guidelines)
    @NotBlank(message = "Signatory full name is required")
    @Size(max = 255, message = "Signatory full name must not exceed 255 characters")
    @Field("signatory_full_name")
    private String signatoryFullName; // Plain text - used for identification
    
    @Past(message = "Signatory date of birth must be in the past")
    @Field("signatory_dob")
    private LocalDate signatoryDob; // Plain text - date field
    
    @NotBlank(message = "Signatory designation is required")
    @Size(max = 100, message = "Signatory designation must not exceed 100 characters")
    @Field("signatory_designation")
    private String signatoryDesignation; // Plain text - Owner/Partner/Director
    
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid signatory mobile format")
    @Field("signatory_mobile")
    private String signatoryMobile; // Plain text - for OTP verification
    
    @Email(message = "Invalid signatory email format")
    @NotBlank(message = "Signatory email is required")
    @Size(max = 255, message = "Signatory email must not exceed 255 characters")
    @Field("signatory_email")
    private String signatoryEmail; // Plain text - for OTP verification
    
    @NotBlank(message = "Signatory government ID type is required")
    @Size(max = 50, message = "Signatory government ID type must not exceed 50 characters")
    @Field("signatory_govt_id_type")
    private String signatoryGovtIdType; // Plain text - PAN/Aadhaar/Passport/Driving License
    
    @Size(max = 500, message = "Encrypted signatory government ID must not exceed 500 characters")
    @Field("signatory_govt_id_number_encrypted")
    private String signatoryGovtIdNumberEncrypted; // ENCRYPTED - Government ID number

    // 3. Bank Account Details (Settlement Account - Mandatory as per RBI PA Guidelines)
    @NotBlank(message = "Account holder name is required")
    @Size(max = 255, message = "Account holder name must not exceed 255 characters")
    @Field("account_holder_name")
    private String accountHolderName; // Plain text - must match entity/legal name
    
    @NotBlank(message = "Bank name is required")
    @Size(max = 255, message = "Bank name must not exceed 255 characters")
    @Field("bank_name")
    private String bankName; // Plain text - bank name
    
    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    @Field("branch_name")
    private String branchName; // Plain text - branch name
    
    @Size(max = 500, message = "Encrypted account number must not exceed 500 characters")
    @Field("account_number_encrypted")
    private String accountNumberEncrypted; // ENCRYPTED - Bank account number (sensitive)
    
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    @Field("ifsc_code")
    private String ifscCode; // Plain text - IFSC code for domestic settlements
    
    @NotBlank(message = "Account type is required")
    @Size(max = 50, message = "Account type must not exceed 50 characters")
    @Field("account_type")
    private String accountType; // Plain text - Current/Savings

    // 4. Transaction Profile (Mandatory as per RBI PA Guidelines)
    @NotBlank(message = "Goods or services description is required")
    @Size(max = 500, message = "Goods or services description must not exceed 500 characters")
    @Field("goods_or_services")
    private String goodsOrServices; // Plain text - nature of goods/services sold
    
    @PositiveOrZero(message = "Average ticket size must be positive or zero")
    @Field("average_ticket_size_inr")
    private Long averageTicketSizeInr; // Plain text - average transaction amount in INR
    
    @PositiveOrZero(message = "Expected monthly volume must be positive or zero")
    @Field("expected_monthly_volume_inr")
    private Long expectedMonthlyVolumeInr; // Plain text - expected monthly transaction volume
    
    @PositiveOrZero(message = "Expected annual turnover must be positive or zero")
    @Field("expected_annual_turnover_inr")
    private Long expectedAnnualTurnoverInr; // Plain text - expected annual turnover
    
    @Size(max = 500, message = "Refund policy URL must not exceed 500 characters")
    @Field("refund_policy_url")
    private String refundPolicyUrl; // Plain text - URL to refund & cancellation policy
    
    @Size(max = 50, message = "Risk category must not exceed 50 characters")
    @Field("risk_category")
    private String riskCategory; // Plain text - Low/Medium/High (assigned by Bank/PA)

    // KYC files metadata - references to uploaded documents
    @DBRef
    private List<KycDocumentEntity> kycDocuments = new ArrayList<>();

    // Audit fields
    @Field("created_at")
    private Long createdAt; // Timestamp when the application was created
    
    @Field("status")
    private ApplicationStatus status; // Current status of the application

    // region Getters/Setters
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
    public String getBusinessPanEncrypted() { return businessPanEncrypted; }
    public void setBusinessPanEncrypted(String businessPanEncrypted) { this.businessPanEncrypted = businessPanEncrypted; }
    public String getGstinEncrypted() { return gstinEncrypted; }
    public void setGstinEncrypted(String gstinEncrypted) { this.gstinEncrypted = gstinEncrypted; }
    public String getCinEncrypted() { return cinEncrypted; }
    public void setCinEncrypted(String cinEncrypted) { this.cinEncrypted = cinEncrypted; }
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
    public String getSignatoryGovtIdNumberEncrypted() { return signatoryGovtIdNumberEncrypted; }
    public void setSignatoryGovtIdNumberEncrypted(String signatoryGovtIdNumberEncrypted) { this.signatoryGovtIdNumberEncrypted = signatoryGovtIdNumberEncrypted; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getAccountNumberEncrypted() { return accountNumberEncrypted; }
    public void setAccountNumberEncrypted(String accountNumberEncrypted) { this.accountNumberEncrypted = accountNumberEncrypted; }
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
    public List<KycDocumentEntity> getKycDocuments() { return kycDocuments; }
    public void setKycDocuments(List<KycDocumentEntity> kycDocuments) { this.kycDocuments = kycDocuments; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    // endregion
}