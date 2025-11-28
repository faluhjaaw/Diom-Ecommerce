package com.dic1.projettrans.customerservice.controllers.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UpdateVendeurDTO {
    private String prenom;
    private String nom;
    private String telephone;
    private String adresse;
    private String bio;
    private String nomBoutique;
    private String photoUrl;
}
