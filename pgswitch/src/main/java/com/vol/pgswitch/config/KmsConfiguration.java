package com.vol.pgswitch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

/**
 * KmsConfiguration - AWS KMS client configuration
 * 
 * This configuration sets up the AWS KMS client for encryption/decryption operations.
 * The client uses default AWS credentials provider chain and can be configured
 * for different regions as needed.
 */
@Configuration
public class KmsConfiguration {

    @Bean
    @Conditional(KmsEnabledCondition.class)
    public KmsClient kmsClient(@Value("${app.crypto.kms-region:ap-south-1}") String region) {
        return KmsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
