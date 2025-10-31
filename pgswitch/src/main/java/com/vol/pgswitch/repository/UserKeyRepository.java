package com.vol.pgswitch.repository;

import com.vol.pgswitch.model.UserKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserKeyRepository extends JpaRepository<UserKeyEntity, Long> {
    Optional<UserKeyEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
}


