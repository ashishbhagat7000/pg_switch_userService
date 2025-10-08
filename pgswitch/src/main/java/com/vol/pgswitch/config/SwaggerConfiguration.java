package com.vol.pgswitch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SwaggerConfiguration - OpenAPI/Swagger documentation configuration
 * 
 * This configuration class sets up Swagger UI and OpenAPI documentation
 * for the PGSwitch merchant registration API. It provides comprehensive
 * API documentation with security information, examples, and interactive
 * testing capabilities.
 * 
 * FEATURES:
 * - Interactive API documentation with Swagger UI
 * - OpenAPI 3.0 specification
 * - Security scheme documentation
 * - Request/response examples
 * - API versioning support
 * 
 * ACCESS:
 * - Swagger UI: http://localhost:9095/swagger-ui.html
 * - OpenAPI JSON: http://localhost:9095/v3/api-docs
 * - OpenAPI YAML: http://localhost:9095/v3/api-docs.yaml
 * 
 * SECURITY:
 * - Documents encryption requirements
 * - Shows required authentication
 * - Explains sensitive data handling
 * - Provides security best practices
 * 
 * COMPLIANCE:
 * - Documents PCI-DSS compliance features
 * - Shows DPDP Act compliance measures
 * - Explains GDPR data protection
 * - Documents RBI PA guidelines adherence
 */
@Configuration
public class SwaggerConfiguration {

    @Value("${server.port:9095}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PGSwitch Merchant Registration API")
                        .version("1.0.0")
                        .description("""
                                **PGSwitch Merchant Registration API**
                                
                                This API provides comprehensive merchant registration capabilities as per RBI Payment Aggregator guidelines.
                                
                                ## 🔐 Security Features
                                - **AES-256-GCM Encryption**: All sensitive data encrypted at rest
                                - **Secure File Storage**: KYC documents stored outside web root
                                - **Input Validation**: Comprehensive validation for all fields
                                - **PCI-DSS Compliance**: Industry-standard security measures
                                
                                ## 📋 Compliance Standards
                                - **RBI PA Guidelines**: Complete merchant onboarding compliance
                                - **PCI-DSS**: Payment card industry security standards
                                - **DPDP Act**: Personal data protection compliance
                                - **GDPR**: General data protection regulation compliance
                                
                                ## 🔒 Encrypted Fields
                                The following sensitive fields are encrypted before database storage:
                                - PAN numbers (businessPanEncrypted)
                                - GSTIN numbers (gstinEncrypted)
                                - CIN numbers (cinEncrypted)
                                - Government ID numbers (signatoryGovtIdNumberEncrypted)
                                - Bank account numbers (accountNumberEncrypted)
                                
                                ## 📁 File Upload
                                - Maximum file size: 10MB per file
                                - Maximum request size: 25MB total
                                - Supported formats: PDF, JPG, PNG, DOC, DOCX
                                - Files stored securely outside web root
                                
                                ## 🚀 Getting Started
                                1. Generate a master encryption key
                                2. Set environment variables
                                3. Start the application
                                4. Use Swagger UI for interactive testing
                                
                                ## 📞 Support
                                For technical support or compliance questions, contact the development team.
                                """)
                        .contact(new Contact()
                                .name("PGSwitch Development Team")
                                .email("support@pgswitch.com")
                                .url("https://pgswitch.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://pgswitch.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.pgswitch.com")
                                .description("Production Server")))
                .tags(List.of(
                        new Tag()
                                .name("Merchant Applications")
                                .description("Operations for merchant registration and management"),
                        new Tag()
                                .name("Key Management")
                                .description("Cryptographic key generation and management operations"),
                        new Tag()
                                .name("Health Check")
                                .description("Application health and status endpoints")));
    }
}
