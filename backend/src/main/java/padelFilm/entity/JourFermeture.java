package com.padelPlay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "jours_fermeture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourFermeture extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    private String raison;

    @Column(nullable = false)
    private Boolean global;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;
}
