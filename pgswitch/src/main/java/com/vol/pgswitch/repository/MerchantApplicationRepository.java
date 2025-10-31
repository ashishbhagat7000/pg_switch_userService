package com.vol.pgswitch.repository;

import com.vol.pgswitch.model.MerchantApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.nio.file.LinkOption;
import java.util.List;

/**
 * MerchantApplicationRepository - Data access layer for merchant applications
 * 
 * This repository provides standard CRUD operations for merchant application documents.
 * It extends MongoRepository to inherit common database operations and can be extended
 * with custom query methods as needed.
 * 
 * FEATURES:
 * - Standard CRUD operations (save, findById, findAll, delete, etc.)
 * - Spring Data MongoDB automatic implementation
 * - Transaction support through Spring framework
 * - Can be extended with custom query methods
 * 
 * USAGE:
 * - Used by MerchantApplicationService for data persistence
 * - Handles encrypted sensitive data transparently
 * - Supports transactional operations for data consistency
 * 
 * SECURITY:
 * - Sensitive fields are encrypted at the document level
 * - Repository operates on encrypted data transparently
 * - No direct access to decrypted sensitive data
 */
@Repository
public interface MerchantApplicationRepository extends JpaRepository<MerchantApplicationEntity, Long> {
    
    /**
     * Find merchants by legal entity name or contact email (case insensitive)
     */
    Page<MerchantApplicationEntity> findByLegalEntityNameContainingIgnoreCaseOrContactEmailContainingIgnoreCase(
        String legalEntityName, String contactEmail, Pageable pageable);
    
    /**
     * Find merchants by legal entity name or contact email (case insensitive) - no pagination
     */
    List<MerchantApplicationEntity> findByLegalEntityNameContainingIgnoreCaseOrContactEmailContainingIgnoreCase(
        String legalEntityName, String contactEmail);
}


