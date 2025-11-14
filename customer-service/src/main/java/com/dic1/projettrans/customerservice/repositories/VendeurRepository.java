package com.dic1.projettrans.customerservice.repositories;

import com.dic1.projettrans.customerservice.entities.Vendeur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendeurRepository extends JpaRepository<Vendeur, Long> {
}
