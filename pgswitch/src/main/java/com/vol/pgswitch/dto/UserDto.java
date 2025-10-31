package com.vol.pgswitch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data transfer object")
public class UserDto {
    
    @Schema(description = "User's unique identifier")
    private Long id;
    
    @Schema(description = "Username for login")
    private String username;
    
    @Schema(description = "User's email address")
    private String email;
    
    @Schema(description = "User's assigned roles")
    private Set<String> roles;
    
    @Schema(description = "Whether the user account is enabled")
    private boolean enabled;
    
    @Schema(description = "When the user was created")
    private LocalDateTime createdAt;
    
    @Schema(description = "When the user was last updated")
    private LocalDateTime updatedAt;
}