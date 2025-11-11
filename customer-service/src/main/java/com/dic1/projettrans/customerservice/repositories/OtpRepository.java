package com.dic1.projettrans.customerservice.repositories;

import com.dic1.projettrans.customerservice.entities.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}
