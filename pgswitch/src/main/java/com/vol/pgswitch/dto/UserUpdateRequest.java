package com.vol.pgswitch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Request payload for updating an existing user")
public class UserUpdateRequest {
    
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "New username", example = "john.doe")
    private String username;
    
    @Email(message = "Invalid email format")
    @Schema(description = "New email address", example = "john.doe@example.com")
    private String email;
    
    @Schema(description = "Updated roles (will replace existing roles)", example = "[\"merchant\", \"admin\"]")
    private Set<String> roles;
    
    @Schema(description = "Whether the account should be enabled")
    private Boolean enabled;
}