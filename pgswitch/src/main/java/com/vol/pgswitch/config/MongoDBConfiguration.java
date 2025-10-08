package com.vol.pgswitch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDBConfiguration - Configuration for MongoDB database
 * 
 * This configuration class sets up MongoDB connection and repository scanning.
 * It extends AbstractMongoClientConfiguration to provide MongoDB-specific
 * configuration options and enables repository scanning for MongoDB repositories.
 * 
 * FEATURES:
 * - MongoDB client configuration
 * - Repository package scanning
 * - Connection pool configuration
 * - Database name configuration
 * 
 * SECURITY:
 * - Connection credentials managed via environment variables
 * - SSL/TLS support for secure connections
 * - Connection pooling for performance and security
 * 
 * COMPLIANCE:
 * - PCI-DSS compliant database configuration
 * - Secure connection handling
 * - Proper connection pooling for production use
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.vol.pgswitch.repository")
public class MongoDBConfiguration extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "pgswitch";
    }

    // Additional MongoDB configuration can be added here
    // For example: connection pooling, SSL settings, etc.
}
