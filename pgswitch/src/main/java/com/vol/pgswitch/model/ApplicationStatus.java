package com.vol.pgswitch.model;

/**
 * ApplicationStatus - Enum for merchant application status
 * 
 * This enum defines the various states a merchant application can be in
 * throughout its lifecycle from submission to final approval or rejection.
 * 
 * STATUS FLOW:
 * SUBMITTED -> UNDER_REVIEW -> APPROVED/REJECTED
 * 
 * COMPLIANCE:
 * - Follows RBI Payment Aggregator guidelines for merchant onboarding
 * - Provides clear audit trail of application status changes
 * - Enables proper workflow management and notifications
 */
public enum ApplicationStatus {
    
    /**
     * Application has been submitted and is awaiting initial review
     */
    SUBMITTED("Submitted", "Application has been submitted and is awaiting review"),
    
    /**
     * Application is currently under review by compliance team
     */
    UNDER_REVIEW("Under Review", "Application is being reviewed by compliance team"),
    
    /**
     * Application has been approved and merchant can start processing
     */
    APPROVED("Approved", "Application has been approved and merchant is active"),
    
    /**
     * Application has been rejected due to compliance issues
     */
    REJECTED("Rejected", "Application has been rejected due to compliance issues"),
    
    /**
     * Application is on hold pending additional documentation
     */
    ON_HOLD("On Hold", "Application is on hold pending additional documentation"),
    
    /**
     * Application has been suspended due to compliance violations
     */
    SUSPENDED("Suspended", "Application has been suspended due to compliance violations"),
    
    /**
     * Application has been withdrawn by the merchant
     */
    WITHDRAWN("Withdrawn", "Application has been withdrawn by the merchant");

    private final String displayName;
    private final String description;

    ApplicationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if the status indicates the application is active
     */
    public boolean isActive() {
        return this == APPROVED;
    }

    /**
     * Check if the status indicates the application is pending
     */
    public boolean isPending() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == ON_HOLD;
    }

    /**
     * Check if the status indicates the application is closed
     */
    public boolean isClosed() {
        return this == REJECTED || this == WITHDRAWN || this == SUSPENDED;
    }
}
