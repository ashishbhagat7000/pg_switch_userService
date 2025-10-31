package com.vol.pgswitch.repository;

import com.vol.pgswitch.model.KycDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * KycDocumentRepository - Data access layer for KYC documents
 * 
 * This repository provides standard CRUD operations for KYC document entities.
 * It extends MongoRepository to inherit common database operations and can be extended
 * with custom query methods for document management.
 * 
 * FEATURES:
 * - Standard CRUD operations (save, findById, findAll, delete, etc.)
 * - Spring Data MongoDB automatic implementation
 * - Transaction support through Spring framework
 * - Can be extended with custom query methods
 * 
 * USAGE:
 * - Used by MerchantApplicationService for document metadata persistence
 * - Handles document metadata (not actual file content)
 * - Supports transactional operations for data consistency
 * 
 * SECURITY:
 * - Stores only document metadata, not file content
 * - File content stored securely outside database
 * - Paths are sanitized to prevent directory traversal
 */
@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocumentEntity, Long> {
    
    /**
     * Delete KYC documents by application ID
     */
    void deleteByApplication_Id(Long applicationId);
}