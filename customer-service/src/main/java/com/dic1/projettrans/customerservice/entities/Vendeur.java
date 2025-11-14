package com.dic1.projettrans.customerservice.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DiscriminatorValue("VENDEUR")
public class Vendeur extends Utilisateur {
    private String bio;
    private String nomBoutique;
    private String photoUrl;
}
