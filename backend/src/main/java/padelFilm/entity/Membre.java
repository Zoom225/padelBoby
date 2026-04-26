package com.padelPlay.entity;

import com.padelPlay.entity.enums.TypeMembre;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "membres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membre extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMembre typeMembre;

    @Column(nullable = false)
    private Double solde;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @OneToMany(mappedBy = "organisateur", cascade = CascadeType.ALL)
    private List<Match> matchesOrganises;

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL)
    private List<Reservation> reservations;

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL)
    private List<Penalite> penalites;
}
