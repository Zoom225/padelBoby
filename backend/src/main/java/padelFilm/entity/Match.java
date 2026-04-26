package com.padelPlay.entity;


import com.padelPlay.entity.enums.StatutMatch;
import com.padelPlay.entity.enums.TypeMatch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terrain_id", nullable = false)
    private Terrain terrain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Membre organisateur;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime heureDebut;

    @Column(nullable = false)
    private LocalTime heureFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMatch typeMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMatch statut;

    // Colonne legacy conservée pour compatibilité base (NOT NULL)
    @Builder.Default
    @Column(name = "nb_joueurs_actuels", nullable = false)
    private Integer nbJoueursActuels = 0;


    @Column(nullable = false)
    private Double prixTotal;

    @Column(nullable = false)
    private Double prixParJoueur;

    private LocalDateTime dateConversionPublic;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations;
}
