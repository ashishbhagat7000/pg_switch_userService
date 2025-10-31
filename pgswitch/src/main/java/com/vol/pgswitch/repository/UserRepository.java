package com.vol.pgswitch.repository;

import com.vol.pgswitch.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByUsername(String username);
    
    Optional<UserEntity> findByEmail(String email);
    
    @Query("SELECT u FROM UserEntity u WHERE u.username = :username AND u.deleted = false")
    Optional<UserEntity> findActiveByUsername(@Param("username") String username);
    
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.deleted = false")
    Optional<UserEntity> findActiveByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM UserEntity u WHERE u.deleted = false")
    Page<UserEntity> findAllActive(Pageable pageable);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.username = :username AND u.deleted = false")
    boolean existsActiveByUsername(@Param("username") String username);
    
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.email = :email AND u.deleted = false")
    boolean existsActiveByEmail(@Param("email") String email);
}