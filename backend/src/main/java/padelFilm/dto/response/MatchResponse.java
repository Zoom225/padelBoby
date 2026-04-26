package com.padelPlay.dto.response;

import com.padelPlay.entity.enums.StatutMatch;
import com.padelPlay.entity.enums.TypeMatch;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponse {
    private Long id;
    private Long terrainId;
    private String terrainNom;
    private String siteNom;
    private Long organisateurId;
    private String organisateurNom;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private TypeMatch typeMatch;
    private StatutMatch statut;
    private Integer nbJoueursActuels;
    private Double prixParJoueur;
    private LocalDateTime dateConversionPublic;
}
