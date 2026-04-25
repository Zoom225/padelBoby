package com.padelPlay.service.impl;

import com.padelPlay.entity.Match;
import com.padelPlay.entity.Membre;
import com.padelPlay.entity.Terrain;
import com.padelPlay.entity.JourFermeture;
import com.padelPlay.entity.enums.StatutMatch;
import com.padelPlay.entity.enums.StatutReservation;
import com.padelPlay.entity.enums.TypeMatch;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.JourFermetureRepository;
import com.padelPlay.repository.MatchRepository;
import com.padelPlay.repository.ReservationRepository;
import com.padelPlay.service.MatchService;
import com.padelPlay.service.MembreService;
import com.padelPlay.service.TerrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private static final int MAX_PLAYERS     = 4;
    private static final double MATCH_PRICE  = 60.0;

    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final MembreService membreService;
    private final TerrainService terrainService;

    @Override
    @Transactional
    public Match create(Match match, Long organisateurId, Long terrainId) {
        Membre organisateur = membreService.getById(organisateurId);
        Terrain terrain     = terrainService.getById(terrainId);
        LocalTime heureFin = match.getHeureDebut()
                .plusMinutes(terrain.getSite().getDureeMatchMinutes());

        // règle : solde dû bloque la création
        if (membreService.hasOutstandingBalance(organisateurId)) {
            throw new BusinessException("Member has an outstanding balance and cannot create a match");
        }

        // règle : pénalité active bloque la création
        if (membreService.hasActivePenalty(organisateurId)) {
            throw new BusinessException("Member has an active penalty and cannot create a match");
        }

        // règle : vérifier le délai de réservation selon le type de membre
        validateBookingDelay(organisateur, match.getDate());
        validateSiteSchedulingRules(terrain, match.getDate(), match.getHeureDebut(), heureFin);

        // règle : vérifier que le créneau est disponible sur ce terrain
        if (!isSlotAvailable(terrainId, match.getDate(), match.getHeureDebut(), heureFin)) {
            throw new BusinessException("This slot is already booked on terrain : " + terrainId);
        }

        // règle : vérifier que le site n'est pas fermé ce jour là
        validateSiteNotClosed(terrain, match.getDate());

        // calcul des heures de fin selon la config du site
        match.setOrganisateur(organisateur);
        match.setTerrain(terrain);
        match.setHeureFin(heureFin);
        match.setPrixTotal(MATCH_PRICE);
        match.setPrixParJoueur(MATCH_PRICE / MAX_PLAYERS);
        match.setStatut(StatutMatch.PLANIFIE);
        match.setNbJoueursActuels(0);

        log.info("Match created by member {} on terrain {} at {}",
                organisateurId, terrainId, match.getDate());

        return matchRepository.save(match);
    }

    @Override
    @Transactional
    public Match update(Long matchId, Match matchUpdate, Long requesterId, Long terrainId) {
        Match existingMatch = getById(matchId);
        Membre requester = membreService.getById(requesterId);
        Terrain terrain = terrainService.getById(terrainId);

        if (!existingMatch.getOrganisateur().getId().equals(requester.getId())) {
            throw new BusinessException("Only the organizer can update this match");
        }

        if (existingMatch.getStatut() == StatutMatch.ANNULE) {
            throw new BusinessException("Cancelled match cannot be updated");
        }

        LocalDateTime startDateTime = LocalDateTime.of(existingMatch.getDate(), existingMatch.getHeureDebut());
        if (startDateTime.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new BusinessException("Match cannot be updated less than 24 hours before start");
        }

        LocalTime heureFin = matchUpdate.getHeureDebut().plusMinutes(terrain.getSite().getDureeMatchMinutes());

        validateBookingDelay(requester, matchUpdate.getDate());
        validateSiteSchedulingRules(terrain, matchUpdate.getDate(), matchUpdate.getHeureDebut(), heureFin);

        if (!isSlotAvailableForUpdate(matchId, terrainId, matchUpdate.getDate(), matchUpdate.getHeureDebut(), heureFin)) {
            throw new BusinessException("This slot is already booked on terrain : " + terrainId);
        }

        validateSiteNotClosed(terrain, matchUpdate.getDate());

        existingMatch.setTerrain(terrain);
        existingMatch.setDate(matchUpdate.getDate());
        existingMatch.setHeureDebut(matchUpdate.getHeureDebut());
        existingMatch.setHeureFin(heureFin);
        existingMatch.setTypeMatch(matchUpdate.getTypeMatch());

        log.info("Match {} updated by organizer {}", matchId, requesterId);
        return matchRepository.save(existingMatch);
    }

    @Override
    @Transactional
    public void cancel(Long matchId, Long requesterId) {
        Match match = getById(matchId);

        if (!match.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Only the organizer can cancel this match");
        }

        if (match.getStatut() == StatutMatch.ANNULE) {
            throw new BusinessException("Match is already cancelled");
        }

        match.setStatut(StatutMatch.ANNULE);
        matchRepository.save(match);
        log.info("Match {} cancelled by organizer {}", matchId, requesterId);
    }

    @Override
    public Match getById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id : " + id));
    }

    @Override
    public List<Match> getAll() {
        return matchRepository.findAll();
    }

    @Override
    public List<Match> getPublicAvailableMatches() {
        return matchRepository.findByTypeMatchAndStatut(
                TypeMatch.PUBLIC,
                StatutMatch.PLANIFIE
        );
    }

    @Override
    public List<Match> getBySiteId(Long siteId) {
        return matchRepository.findByTerrainSiteId(siteId);
    }

    @Override
    public List<Match> getByOrganisateurId(Long organisateurId) {
        return matchRepository.findByOrganisateurId(organisateurId)
                .stream()
                .filter(match -> match.getStatut() != StatutMatch.ANNULE)
                .toList();
    }

    @Override
    @Transactional
    public void convertToPublic(Long matchId) {
        Match match = getById(matchId);

        if (match.getTypeMatch() == TypeMatch.PUBLIC) {
            throw new BusinessException("Match is already public");
        }

        match.setTypeMatch(TypeMatch.PUBLIC);
        match.setDateConversionPublic(java.time.LocalDateTime.now());
        matchRepository.save(match);

        // pénalité pour l'organisateur
        membreService.addPenalty(match.getOrganisateur().getId());

        log.info("Match {} converted to public, penalty applied to organizer {}",
                matchId, match.getOrganisateur().getId());
    }

    @Override
    @Transactional
    public void checkAndConvertExpiredPrivateMatches() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Match> expiredMatches = matchRepository
                .findByDateAndStatut(tomorrow, StatutMatch.PLANIFIE)
                .stream()
                .filter(m -> m.getTypeMatch() == TypeMatch.PRIVE)
                .filter(m -> !isMatchFull(m.getId()))
                .toList();

        expiredMatches.forEach(m -> convertToPublic(m.getId()));

        log.info("Scheduler : {} private match(es) converted to public", expiredMatches.size());
    }

    @Override
    @Transactional
    public void incrementPlayers(Long matchId) {
        Match match = getById(matchId);
        int confirmedPlayers = (int) reservationRepository
                .countReservationsByMatchIdAndStatut(matchId, StatutReservation.CONFIRMEE);

        if (confirmedPlayers > MAX_PLAYERS) {
            throw new BusinessException("Match is already full");
        }

        match.setNbJoueursActuels(confirmedPlayers);
        match.setStatut(confirmedPlayers == MAX_PLAYERS ? StatutMatch.COMPLET : StatutMatch.PLANIFIE);

        matchRepository.save(match);
    }

    @Override
    @Transactional
    public void decrementPlayers(Long matchId) {
        Match match = getById(matchId);
        int confirmedPlayers = (int) reservationRepository
                .countReservationsByMatchIdAndStatut(matchId, StatutReservation.CONFIRMEE);

        if (match.getStatut() != StatutMatch.ANNULE) {
            match.setNbJoueursActuels(confirmedPlayers);
            match.setStatut(confirmedPlayers >= MAX_PLAYERS ? StatutMatch.COMPLET : StatutMatch.PLANIFIE);
        }
        matchRepository.save(match);
    }

    @Override
    public boolean isMatchFull(Long matchId) {
        return reservationRepository.countReservationsByMatchIdAndStatut(matchId, StatutReservation.CONFIRMEE)
                >= MAX_PLAYERS;
    }

    @Override
    public boolean isSlotAvailable(Long terrainId, LocalDate date, LocalTime heureDebut, LocalTime heureFin) {
        List<Match> existing = matchRepository.findByTerrainId(terrainId)
                .stream()
                .filter(m -> m.getDate().equals(date))
                .filter(m -> m.getStatut() != StatutMatch.ANNULE)
                .toList();

        Terrain terrain = terrainService.getById(terrainId);
        int breakMinutes = terrain.getSite().getDureeEntreMatchMinutes();

        return existing.stream().noneMatch(existingMatch ->
                hasSlotConflictWithBuffer(heureDebut, heureFin, existingMatch, breakMinutes));
    }

    private boolean isSlotAvailableForUpdate(Long matchId, Long terrainId, LocalDate date, LocalTime heureDebut, LocalTime heureFin) {
        List<Match> existing = matchRepository.findByTerrainId(terrainId)
                .stream()
                .filter(m -> !m.getId().equals(matchId))
                .filter(m -> m.getDate().equals(date))
                .filter(m -> m.getStatut() != StatutMatch.ANNULE)
                .toList();

        Terrain terrain = terrainService.getById(terrainId);
        int breakMinutes = terrain.getSite().getDureeEntreMatchMinutes();

        return existing.stream().noneMatch(existingMatch ->
                hasSlotConflictWithBuffer(heureDebut, heureFin, existingMatch, breakMinutes));
    }

    private boolean hasSlotConflictWithBuffer(LocalTime newStart, LocalTime newEnd, Match existingMatch, int breakMinutes) {
        LocalTime existingStart = existingMatch.getHeureDebut();
        LocalTime existingEnd = existingMatch.getHeureFin();

        boolean newEndsBeforeExistingWithBreak = !newEnd.plusMinutes(breakMinutes).isAfter(existingStart);
        boolean newStartsAfterExistingWithBreak = !newStart.isBefore(existingEnd.plusMinutes(breakMinutes));

        return !(newEndsBeforeExistingWithBreak || newStartsAfterExistingWithBreak);
    }

    private void validateBookingDelay(Membre membre, LocalDate matchDate) {
        LocalDate today = LocalDate.now();
        long daysUntilMatch = ChronoUnit.DAYS.between(today, matchDate);

        int requiredDays = switch (membre.getTypeMembre()) {
            case GLOBAL -> 21;  // 3 semaines
            case SITE   -> 14;  // 2 semaines
            case LIBRE  -> 5;   // 5 jours
        };

        if (daysUntilMatch < requiredDays) {
            throw new BusinessException(
                    "Member type " + membre.getTypeMembre() +
                            " must book at least " + requiredDays + " days in advance"
            );
        }
    }

    private void validateSiteNotClosed(Terrain terrain, LocalDate date) {
        List<JourFermeture> globalClosures = Optional
                .ofNullable(jourFermetureRepository.findByGlobalTrue())
                .orElse(List.of());

        List<JourFermeture> siteClosures = Optional
                .ofNullable(jourFermetureRepository.findBySiteId(terrain.getSite().getId()))
                .orElse(List.of());

        boolean isClosed = Stream.concat(globalClosures.stream(), siteClosures.stream())
                .anyMatch(j -> j.getDate().equals(date));

        if (isClosed) {
            throw new BusinessException("The site is closed on : " + date);
        }
    }

    private void validateSiteSchedulingRules(Terrain terrain, LocalDate date, LocalTime heureDebut, LocalTime heureFin) {

        LocalTime ouverture = terrain.getSite().getHeureOuverture();
        LocalTime fermeture = terrain.getSite().getHeureFermeture();

        if (heureDebut.isBefore(ouverture) || heureFin.isAfter(fermeture)) {
            throw new BusinessException("Requested slot is outside site opening hours");
        }
    }
}
