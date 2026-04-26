package com.padelPlay.dto.response;

import com.padelPlay.entity.enums.TypeMembre;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreResponse {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private TypeMembre typeMembre;
    private Long siteId;
    private String siteNom;
    private Double solde;
    // pas de passwordHash, pas de relations JPA
}
