package com.padelPlay.repository;

import com.padelPlay.entity.Reservation;
import com.padelPlay.entity.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByMatchId(Long matchId);
    List<Reservation> findByMembreId(Long membreId);
    boolean existsByMatchIdAndMembreId(Long matchId, Long membreId);

    @Query("select count(r) from Reservation r where r.match.id = :matchId and r.statut = :statut")
    long countReservationsByMatchIdAndStatut(@Param("matchId") Long matchId,
                                             @Param("statut") StatutReservation statut);
}
