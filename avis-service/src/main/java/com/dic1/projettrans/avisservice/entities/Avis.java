package com.dic1.projettrans.avisservice.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "avis")
public class Avis {

    @Id
    private String id;

    private String produitId;
    private Long userId;
    private Integer note;        // obligatoire
    private String commentaire;  // facultatif
    private LocalDateTime date;
}
