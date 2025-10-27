package com.vol.pgswitch.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * KmsEnabledCondition - Condition to check if KMS is enabled
 * 
 * This condition checks if the KMS key ID is configured in application properties.
 * If configured, the KmsClient bean will be created. If not, it will be skipped.
 */
public class KmsEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String kmsKeyId = context.getEnvironment().getProperty("app.crypto.kms-key-id");
        return kmsKeyId != null && !kmsKeyId.trim().isEmpty();
    }
}
