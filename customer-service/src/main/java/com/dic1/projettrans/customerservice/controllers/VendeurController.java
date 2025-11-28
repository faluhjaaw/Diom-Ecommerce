package com.dic1.projettrans.customerservice.controllers;

import com.dic1.projettrans.customerservice.controllers.dto.UpdateVendeurDTO;
import com.dic1.projettrans.customerservice.entities.Role;
import com.dic1.projettrans.customerservice.entities.Vendeur;
import com.dic1.projettrans.customerservice.repositories.VendeurRepository;
import org.hibernate.sql.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendeurController {

    private final VendeurRepository vendeurRepository;

    public VendeurController(VendeurRepository vendeurRepository) {
        this.vendeurRepository = vendeurRepository;
    }

    @PostMapping
    public ResponseEntity<Vendeur> create(@RequestBody Vendeur vendeur) {
        // Ensure role is VENDEUR
        vendeur.setRole(Role.VENDEUR);
        Vendeur saved = vendeurRepository.save(vendeur);
        return ResponseEntity.created(URI.create("/api/vendors/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<Vendeur> findAll() {
        return vendeurRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendeur> findById(@PathVariable Long id) {
        return vendeurRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendeur> update(@PathVariable Long id, @RequestBody UpdateVendeurDTO incoming) {
        return vendeurRepository.findById(id)
                .map(existing -> {
                    existing.setNom(incoming.getNom());
                    existing.setPrenom(incoming.getPrenom());
                    existing.setTelephone(incoming.getTelephone());
                    existing.setBio(incoming.getBio());
                    existing.setNomBoutique(incoming.getNomBoutique());
                    existing.setPhotoUrl(incoming.getPhotoUrl());
                    return ResponseEntity.ok(vendeurRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!vendeurRepository.existsById(id)) return ResponseEntity.notFound().build();
        vendeurRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
