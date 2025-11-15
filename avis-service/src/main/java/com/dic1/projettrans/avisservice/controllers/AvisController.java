package com.dic1.projettrans.avisservice.controllers;

import com.dic1.projettrans.avisservice.entities.Avis;
import com.dic1.projettrans.avisservice.services.AvisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avis")
@RequiredArgsConstructor
public class AvisController {

    private final AvisService avisService;

    // Ajouter un avis
    @PostMapping
    public ResponseEntity<Avis> ajouterAvis(@RequestBody Avis avis) {
        Avis created = avisService.ajouterAvis(avis);
        return ResponseEntity.ok(created);
    }

    // Modifier un avis
    @PutMapping("/{id}")
    public ResponseEntity<Avis> modifierAvis(@PathVariable String id, @RequestBody Avis avis) {
        Avis updated = avisService.modifierAvis(id, avis);
        return ResponseEntity.ok(updated);
    }

    // Supprimer un avis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerAvis(@PathVariable String id) {
        avisService.supprimerAvis(id);
        return ResponseEntity.noContent().build();
    }

    // Lister les avis d’un produit
    @GetMapping("/produit/{produitId}")
    public ResponseEntity<List<Avis>> listerAvisParProduit(@PathVariable String produitId) {
        List<Avis> avisList = avisService.listerAvisParProduit(produitId);
        return ResponseEntity.ok(avisList);
    }

    // Lister les avis d’un utilisateur
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Avis>> listerAvisParUtilisateur(@PathVariable Long userId) {
        List<Avis> avisList = avisService.listerAvisParUtilisateur(userId);
        return ResponseEntity.ok(avisList);
    }

    @GetMapping("id/{id}")
    public ResponseEntity<Avis> trouverAvisParId(@PathVariable String id) {
        return avisService.trouverAvisParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Dans AvisController.java
    @GetMapping("/produit/{produitId}/moyenne")
    public ResponseEntity<Double> obtenirNoteMoyenne(@PathVariable String produitId) {
        Double moyenne = avisService.calculerNoteMoyenne(produitId);
        return ResponseEntity.ok(moyenne);
    }
}
