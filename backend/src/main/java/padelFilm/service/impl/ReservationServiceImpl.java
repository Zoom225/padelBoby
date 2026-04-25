package com.padelPlay.service.impl;

import com.padelPlay.entity.Match;
import com.padelPlay.entity.Membre;
import com.padelPlay.entity.Paiement;
import com.padelPlay.entity.Reservation;
import com.padelPlay.entity.enums.*;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.PaiementRepository;
import com.padelPlay.repository.ReservationRepository;
import com.padelPlay.service.MatchService;
import com.padelPlay.service.MembreService;
import com.padelPlay.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final MatchService matchService;
    private final MembreService membreService;

    @Override
    @Transactional
    public Reservation create(Long matchId, Long membreId, Long requesterId) {
        Match match = matchService.getById(matchId);
        Membre membre = membreService.getById(membreId);

        if (requesterId == null) {
            throw new BusinessException("Requester id is required");
        }

        // règle : match doit être PLANIFIE
        if (match.getStatut() == StatutMatch.COMPLET) {
            throw new BusinessException("Match is already full");
        }
        if (match.getStatut() == StatutMatch.ANNULE) {
            throw new BusinessException("Match is cancelled");
        }

        // règle : le membre ne peut pas déjà être inscrit
        if (reservationRepository.existsByMatchIdAndMembreId(matchId, membreId)) {
            throw new BusinessException("Member is already registered in this match");
        }

        // règle : pénalité active bloque la réservation
        if (membreService.hasActivePenalty(membreId)) {
            throw new BusinessException("Member has an active penalty");
        }

        // règle : solde dû bloque la réservation
        if (membreService.hasOutstandingBalance(membreId)) {
            throw new BusinessException("Member has an outstanding balance");
        }

        // règle : match privé → seul l'organisateur peut ajouter des joueurs
        // règle : match public → un membre ne peut rejoindre que pour lui-même
        if (match.getTypeMatch() == TypeMatch.PRIVE) {
            if (!match.getOrganisateur().getId().equals(requesterId)) {
                throw new BusinessException("Private match : only the organizer can add players");
            }
        } else {
            if (!requesterId.equals(membreId)) {
                throw new BusinessException("Public match : a member can only join for themselves");
            }
        }

        // règle : membre SITE ne peut réserver que sur son site
        if (membre.getTypeMembre() == TypeMembre.SITE) {
            Long membreSiteId = membre.getSite().getId();
            Long matchSiteId = match.getTerrain().getSite().getId();
            if (!membreSiteId.equals(matchSiteId)) {
                throw new BusinessException("SITE member can only book on their own site");
            }
        }

        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(membre)
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        reservation = reservationRepository.save(reservation);

        // créer le paiement associé automatiquement
        Paiement paiement = Paiement.builder()
                .reservation(reservation)
                .montant(match.getPrixParJoueur())
                .statut(StatutPaiement.EN_ATTENTE)
                .build();

        paiement = paiementRepository.save(paiement);
        reservation.setPaiement(paiement);
        reservation = reservationRepository.save(reservation);

        log.info("Reservation created for member {} on match {}", membreId, matchId);

        return reservation;
    }

    @Override
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id : " + id));
    }

    @Override
    public List<Reservation> getByMatchId(Long matchId) {
        return reservationRepository.findByMatchId(matchId);
    }

    @Override
    public List<Reservation> getByMembreId(Long membreId) {
        return reservationRepository.findByMembreId(membreId);
    }

    @Override
    @Transactional
    public void cancel(Long reservationId) {
        Reservation reservation = getById(reservationId);
        boolean wasConfirmed = reservation.getStatut() == StatutReservation.CONFIRMEE;

        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Reservation is already cancelled");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        reservationRepository.save(reservation);

        // rembourser si déjà payé
        Paiement paiement = reservation.getPaiement();
        if (paiement != null && paiement.getStatut() == StatutPaiement.PAYE) {
            paiement.setStatut(StatutPaiement.REMBOURSE);
            paiementRepository.save(paiement);
        }

        // décrémenter uniquement si la réservation avait déjà été confirmée/payée
        if (wasConfirmed) {
            matchService.decrementPlayers(reservation.getMatch().getId());
        }

        log.info("Reservation {} cancelled", reservationId);
    }

    @Override
    @Transactional
    public void confirm(Long reservationId) {
        Reservation reservation = getById(reservationId);

        if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
            throw new BusinessException("Reservation is already confirmed");
        }
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Cancelled reservation cannot be confirmed");
        }

        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservationRepository.save(reservation);

        // incrémenter le nombre de joueurs
        matchService.incrementPlayers(reservation.getMatch().getId());

        log.info("Reservation {} confirmed", reservationId);
    }
}
