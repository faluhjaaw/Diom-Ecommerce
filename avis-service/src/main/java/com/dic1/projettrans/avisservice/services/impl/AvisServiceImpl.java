package com.dic1.projettrans.avisservice.services.impl;

import com.dic1.projettrans.avisservice.entities.Avis;
import com.dic1.projettrans.avisservice.feign.OrderServiceClient;
import com.dic1.projettrans.avisservice.repositories.AvisRepository;
import com.dic1.projettrans.avisservice.services.AvisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvisServiceImpl implements AvisService {

    private final AvisRepository avisRepository;
    private final OrderServiceClient  orderServiceClient;


    @Override
    public Avis ajouterAvis(Avis avis) {
        // Vérifie si l'utilisateur a commandé le produit
        boolean hasOrdered = orderServiceClient.hasUserOrderedProduct(avis.getUserId(), avis.getProduitId());
        if (!hasOrdered) {
            throw new RuntimeException("L'utilisateur n'a pas commandé ce produit, il ne peut pas laisser d'avis.");
        }

        // Remplit la date automatiquement
        avis.setDate(LocalDateTime.now());
        return avisRepository.save(avis);
    }

    @Override
    public Avis modifierAvis(String avisId, Avis avis) {
        Optional<Avis> existingAvis = avisRepository.findById(avisId);
        if(existingAvis.isPresent()){
            Avis a = existingAvis.get();
            if(avis.getNote() != null) a.setNote(avis.getNote());
            if(avis.getCommentaire() != null) a.setCommentaire(avis.getCommentaire());
            // on peut mettre à jour la date aussi
            a.setDate(java.time.LocalDateTime.now());
            return avisRepository.save(a);
        } else {
            throw new RuntimeException("Avis non trouvé avec id : " + avisId);
        }
    }

    @Override
    public void supprimerAvis(String avisId) {
        avisRepository.deleteById(avisId);
    }

    @Override
    public List<Avis> listerAvisParProduit(String produitId) {
        return avisRepository.findByProduitId(produitId);
    }

    @Override
    public List<Avis> listerAvisParUtilisateur(Long userId) {
        return avisRepository.findByUserId(userId);
    }

    @Override
    public Optional<Avis> trouverAvisParId(String avisId) {
        return avisRepository.findById(avisId);
    }
}
