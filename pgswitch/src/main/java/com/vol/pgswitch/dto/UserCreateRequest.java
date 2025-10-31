package com.vol.pgswitch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Request payload for creating a new user")
public class UserCreateRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Username for login", example = "john.doe")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;
    
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Optional password (if not provided, will be generated)", example = "strongPassword123")
    private String password;
    
    @Schema(description = "Initial roles to assign to the user", example = "[\"merchant\", \"admin\"]")
    private Set<String> roles;
    
    @Schema(description = "Whether to send welcome email with credentials")
    private boolean sendWelcomeEmail = true;
}