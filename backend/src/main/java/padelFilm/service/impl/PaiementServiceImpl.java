package com.padelPlay.service.impl;

import com.padelPlay.entity.Match;
import com.padelPlay.entity.Membre;
import com.padelPlay.entity.Paiement;
import com.padelPlay.entity.Reservation;
import com.padelPlay.entity.enums.StatutReservation;
import com.padelPlay.entity.enums.StatutPaiement;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.MembreRepository;
import com.padelPlay.repository.PaiementRepository;
import com.padelPlay.service.PaiementService;
import com.padelPlay.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaiementServiceImpl implements PaiementService {

    private final PaiementRepository paiementRepository;
    private final ReservationService reservationService;
    private final MembreRepository membreRepository;

    @Override
    @Transactional
    public Paiement pay(Long reservationId, Long membreId) {
        Reservation reservation = reservationService.getById(reservationId);
        Membre membre = reservation.getMembre();

        // règle : seul le membre concerné peut payer
        if (!membre.getId().equals(membreId)) {
            throw new BusinessException("Only the member of this reservation can pay");
        }

        Paiement paiement = reservation.getPaiement();
        if (paiement == null) {
            throw new ResourceNotFoundException("No payment found for reservation : " + reservationId);
        }

        if (paiement.getStatut() == StatutPaiement.PAYE) {
            throw new BusinessException("Payment already done for this reservation");
        }
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Cannot pay a cancelled reservation");
        }

        // règle : si solde dû, on l'ajoute au montant
        double montantFinal = paiement.getMontant();
        if (membre.getSolde() > 0.0) {
            montantFinal += membre.getSolde();
            log.info("Outstanding balance of {} added to payment for member {}",
                    membre.getSolde(), membreId);
            membre.setSolde(0.0);
            membreRepository.save(membre);
        }

        paiement.setMontant(montantFinal);
        paiement.setStatut(StatutPaiement.PAYE);
        paiement.setDatePaiement(LocalDateTime.now());
        paiementRepository.save(paiement);

        // confirmer la réservation après paiement
        reservationService.confirm(reservationId);

        log.info("Payment done for reservation {} by member {}", reservationId, membreId);

        return paiement;
    }

    @Override
    public Paiement getById(Long id) {
        return paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id : " + id));
    }

    @Override
    public Paiement getByReservationId(Long reservationId) {
        return paiementRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for reservation : " + reservationId));
    }

    @Override
    public List<Paiement> getByMembreId(Long membreId) {
        return paiementRepository.findByReservationMembreId(membreId);
    }

    @Override
    @Transactional
    public void checkUnpaidBeforeMatch() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // récupérer toutes les réservations EN_ATTENTE pour les matchs de demain
        paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE)
                .stream()
                .filter(p -> p.getReservation().getMatch()
                        .getDate().equals(tomorrow))
                .forEach(p -> {
                    Reservation reservation = p.getReservation();
                    Match match = reservation.getMatch();

                    // annuler la réservation non payée
                    reservationService.cancel(reservation.getId());

                    // ajouter le solde dû à l'organisateur si match public non complet
                    double partManquante = match.getPrixParJoueur();
                    Membre organisateur = match.getOrganisateur();
                    organisateur.setSolde(organisateur.getSolde() + partManquante);
                    membreRepository.save(organisateur);

                    log.info("Unpaid reservation {} cancelled, balance {} added to organizer {}",
                            reservation.getId(), partManquante, organisateur.getId());
                });
    }
}
