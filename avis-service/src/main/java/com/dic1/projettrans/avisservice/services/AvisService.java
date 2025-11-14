package com.dic1.projettrans.avisservice.services;

import com.dic1.projettrans.avisservice.entities.Avis;

import java.util.List;
import java.util.Optional;

public interface AvisService {

    Avis ajouterAvis(Avis avis);

    Avis modifierAvis(String avisId, Avis avis);

    void supprimerAvis(String avisId);

    List<Avis> listerAvisParProduit(String produitId);

    List<Avis> listerAvisParUtilisateur(Long userId);

    Optional<Avis> trouverAvisParId(String avisId);
}
