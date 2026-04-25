package com.padelPlay.service;


import com.padelPlay.entity.Match;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface MatchService {
    Match create(Match match, Long organisateurId, Long terrainId);
    Match update(Long matchId, Match matchUpdate, Long requesterId, Long terrainId);
    void cancel(Long matchId, Long requesterId);
    Match getById(Long id);
    List<Match> getAll();
    List<Match> getPublicAvailableMatches();
    List<Match> getBySiteId(Long siteId);
    List<Match> getByOrganisateurId(Long organisateurId);
    void convertToPublic(Long matchId);
    void checkAndConvertExpiredPrivateMatches();
    void incrementPlayers(Long matchId);
    void decrementPlayers(Long matchId);
    boolean isMatchFull(Long matchId);
    boolean isSlotAvailable(Long terrainId, LocalDate date, LocalTime heureDebut, LocalTime heureFin);
}
