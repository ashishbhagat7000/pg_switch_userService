package com.vol.pgswitch.repository;

import com.vol.pgswitch.model.UserKeyEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserKeyRepository extends MongoRepository<UserKeyEntity, String> {
    Optional<UserKeyEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
}


