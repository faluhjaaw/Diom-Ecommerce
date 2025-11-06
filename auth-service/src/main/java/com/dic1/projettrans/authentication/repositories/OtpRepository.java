package com.dic1.projettrans.authentication.repositories;

import com.dic1.projettrans.authentication.entities.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<OtpCode, String> {
    Optional<OtpCode> findTopByEmailOrderByExpirationDesc(String email);
}
