package com.dic1.projettrans.productservice.client;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String prenom;
    private String nom;
    private String email;
    private String sellerType; // "CUSTOMER" | "SHOP_OWNER"
    private boolean active;
}
