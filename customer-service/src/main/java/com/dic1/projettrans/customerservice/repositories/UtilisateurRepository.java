package com.dic1.projettrans.customerservice.repositories;

import com.dic1.projettrans.customerservice.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByActiveTrue();
    Optional<Utilisateur> findByIdAndActiveTrue(Long id);
    Optional<Utilisateur> findByEmailAndActiveTrue(String email);
}
