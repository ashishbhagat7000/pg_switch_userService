package com.vol.pgswitch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Entity
@Table(name = "kyc_documents")
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Changed type from String to Long, standard for JPA IDs

    // Reference to parent merchant application (Many-to-One relationship)
    // This creates a foreign key column named 'application_id' in the kyc_documents table.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @NotNull(message = "Application reference is required")
    private MerchantApplicationEntity application;

    @NotBlank(message = "Document type is required")
    @Size(max = 100, message = "Document type must not exceed 100 characters")
    @Column(name = "document_type", length = 100, nullable = false)
    private String documentType; // Type of document: PAN, GST_CERT, INCORP_CERT, etc.

    @NotBlank(message = "Stored path is required")
    @Size(max = 500, message = "Stored path must not exceed 500 characters")
    @Column(name = "stored_path", length = 500, nullable = false)
    private String storedPath; // Secure file system path where document is stored

    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    @Column(name = "original_filename", length = 255)
    private String originalFilename; // Original filename for audit trail

    @PositiveOrZero(message = "File size must be positive or zero")
    @Column(name = "size_bytes")
    private Long sizeBytes; // File size in bytes for validation and reporting

    @Size(max = 100, message = "Content type must not exceed 100 characters")
    @Column(name = "content_type", length = 100)
    private String contentType; // MIME type of the uploaded file

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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