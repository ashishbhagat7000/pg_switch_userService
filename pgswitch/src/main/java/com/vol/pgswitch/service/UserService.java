package com.vol.pgswitch.service;

import com.vol.pgswitch.dto.RelationTupleDto;
import com.vol.pgswitch.dto.UserCreateRequest;
import com.vol.pgswitch.dto.UserDto;
import com.vol.pgswitch.dto.UserUpdateRequest;
import com.vol.pgswitch.exception.ResourceNotFoundException;
import com.vol.pgswitch.exception.UserAlreadyExistsException;
import com.vol.pgswitch.model.UserEntity;
import com.vol.pgswitch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserDto> findAllUsers(Pageable pageable) {
        return userRepository.findAllActive(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public UserDto findUserById(Long id) {
        return userRepository.findById(id)
                .filter(user -> !user.isDeleted())
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        // 1️⃣ Validate unique constraints
        if (userRepository.existsActiveByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsActiveByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // 2️⃣ Create user entity
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setEnabled(true);

        // 3️⃣ Store roles (for information only, since Cognito + Keto are sources of truth)
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(request.getRoles());
        } else {
            user.setRoles(Set.of("merchant")); // default role if not specified
        }

        // 4️⃣ Save user in DB
        user.prePersist();
        user = userRepository.save(user);

       /* // 5️⃣ Sync roles to Keto tuples
        for (String role : user.getRoles()) {
            RelationTupleDto tuple = new RelationTupleDto();
            tuple.setNamespace("roles");
            tuple.setObject(role.toLowerCase());
            tuple.setRelation("member");
            tuple.setSubjectId("user:" + user.getUsername());
            ketoService.createRelation(tuple);
        }*/

       /* // 6️⃣ Optional: Send welcome email
        if (request.isSendWelcomeEmail()) {
            notificationService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        }
*/
        // 7️⃣ Audit logging
        auditService.logUserCreated(user.getId(), user.getUsername());

        return mapToDto(user);
    }


    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request) {
        UserEntity user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check username uniqueness if changed
        if (request.getUsername() != null &&
            !request.getUsername().equals(user.getUsername()) &&
            userRepository.existsActiveByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Check email uniqueness if changed
        if (request.getEmail() != null &&
            !request.getEmail().equals(user.getEmail()) &&
            userRepository.existsActiveByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // Update fields if provided
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Audit
        auditService.logUserUpdated(user.getId(), user.getUsername());

        return mapToDto(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Soft delete
        user.setDeleted(true);
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Audit
        auditService.logUserDeleted(user.getId(), user.getUsername());
    }

    private UserDto mapToDto(UserEntity user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                // ✅ Use roles from the entity itself
                .roles(user.getRoles() != null ? user.getRoles() : Set.of())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }


    private Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsStringList("cognito:groups")
                        .stream()
                        .collect(Collectors.toSet());
            }
        }
        return Set.of();
    }
}
