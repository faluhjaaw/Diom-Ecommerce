package com.dic1.projettrans.avisservice.repositories;

import com.dic1.projettrans.avisservice.entities.Avis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AvisRepository extends MongoRepository<Avis, String> {

    List<Avis> findByProduitId(String produitId);

    List<Avis> findByUserId(Long userId);

    boolean existsByUserIdAndProduitId(Long userId, String produitId);

}
