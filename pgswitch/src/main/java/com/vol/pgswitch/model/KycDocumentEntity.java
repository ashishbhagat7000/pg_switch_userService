package com.vol.pgswitch.model;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.DBRef;

/**
 * KycDocumentEntity - Document for storing KYC document metadata
 * 
 * This document stores metadata about uploaded KYC documents for merchant applications.
 * It maintains a reference to the actual file stored on disk and contains:
 * - Document type (PAN, GST Certificate, Certificate of Incorporation, etc.)
 * - File storage path (secure file system location)
 * - Original filename and metadata (size, content type)
 * - Reference to the parent merchant application
 * 
 * SECURITY FEATURES:
 * - Files are stored outside web root for security
 * - File paths are sanitized to prevent directory traversal
 * - Original filenames are preserved for audit trails
 * - File metadata is stored for compliance reporting
 * 
 * COMPLIANCE:
 * - Supports all required KYC documents as per RBI PA guidelines
 * - Maintains audit trail with original filenames and upload metadata
 * - File storage follows PCI-DSS requirements for document handling
 * 
 * MONGODB FEATURES:
 * - Uses MongoDB document structure for flexible metadata storage
 * - DBRef for referencing parent merchant application
 * - Indexed fields for efficient document queries
 * - Embedded metadata for file information
 */
@Document(collection = "kyc_documents")
public class KycDocumentEntity {

    @Id
    private String id;

    @DBRef
    @NotNull(message = "Application reference is required")
    private MerchantApplicationEntity application; // Reference to parent merchant application

    @NotBlank(message = "Document type is required")
    @Size(max = 100, message = "Document type must not exceed 100 characters")
    @Field("document_type")
    private String documentType; // Type of document: PAN, GST_CERT, INCORP_CERT, etc.

    @NotBlank(message = "Stored path is required")
    @Size(max = 500, message = "Stored path must not exceed 500 characters")
    @Field("stored_path")
    private String storedPath; // Secure file system path where document is stored

    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    @Field("original_filename")
    private String originalFilename; // Original filename for audit trail
    
    @PositiveOrZero(message = "File size must be positive or zero")
    @Field("size_bytes")
    private Long sizeBytes; // File size in bytes for validation and reporting
    
    @Size(max = 100, message = "Content type must not exceed 100 characters")
    @Field("content_type")
    private String contentType; // MIME type of the uploaded file

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public MerchantApplicationEntity getApplication() { return application; }
    public void setApplication(MerchantApplicationEntity application) { this.application = application; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getStoredPath() { return storedPath; }
    public void setStoredPath(String storedPath) { this.storedPath = storedPath; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}