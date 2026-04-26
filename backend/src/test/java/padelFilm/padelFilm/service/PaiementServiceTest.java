package com.padelPlay.service;

import com.padelPlay.entity.*;
import com.padelPlay.entity.enums.*;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.MembreRepository;
import com.padelPlay.repository.PaiementRepository;
import com.padelPlay.service.impl.PaiementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaiementService tests")
class PaiementServiceTest {

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private MembreRepository membreRepository;

    @InjectMocks
    private PaiementServiceImpl paiementService;

    private Site site;
    private Terrain terrain;
    private Membre organisateur;
    private Membre joueur;
    private Match match;
    private Reservation reservation;
    private Paiement paiement;

    @BeforeEach
    void setUp() {
        site = Site.builder()
                .nom("Padel Club Lyon")
                .build();

        terrain = Terrain.builder()
                .nom("Court A")
                .site(site)
                .build();

        organisateur = Membre.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .typeMembre(TypeMembre.GLOBAL)
                .solde(0.0)
                .build();
        organisateur.setId(1L);

        joueur = Membre.builder()
                .matricule("G1002")
                .nom("Dupont")
                .prenom("Julie")
                .typeMembre(TypeMembre.GLOBAL)
                .solde(0.0)
                .build();
        joueur.setId(2L);

        match = Match.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .date(LocalDate.now().plusDays(25))
                .heureDebut(LocalTime.of(15, 0))
                .heureFin(LocalTime.of(16, 30))
                .typeMatch(TypeMatch.PUBLIC)
                .statut(StatutMatch.PLANIFIE)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();
        match.setId(10L);

        reservation = Reservation.builder()
                .match(match)
                .membre(joueur)
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        reservation.setId(1L);

        paiement = Paiement.builder()
                .reservation(reservation)
                .montant(15.0)
                .statut(StatutPaiement.EN_ATTENTE)
                .build();
        paiement.setId(1L);

        reservation.setPaiement(paiement);
    }

    // ================================================================
    // PAY
    // ================================================================
    @Nested
    @DisplayName("pay()")
    class PayTests {

        @Test
        @DisplayName("✅ should process payment and confirm reservation")
        void shouldProcessPayment() {
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getStatut()).isEqualTo(StatutPaiement.PAYE);
            assertThat(result.getMontant()).isEqualTo(15.0);
            assertThat(result.getDatePaiement()).isNotNull();

            // la réservation doit être confirmée après paiement
            verify(reservationService, times(1)).confirm(1L);
        }

        @Test
        @DisplayName("✅ should add outstanding balance to payment amount and clear it")
        void shouldAddOutstandingBalanceToPayment() {
            // joueur a une dette de 15€
            joueur.setSolde(15.0);

            when(reservationService.getById(1L)).thenReturn(reservation);
            when(membreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            // 15€ (part match) + 15€ (solde dû) = 30€
            assertThat(result.getMontant()).isEqualTo(30.0);
            assertThat(result.getStatut()).isEqualTo(StatutPaiement.PAYE);

            // solde remis à zéro
            assertThat(joueur.getSolde()).isEqualTo(0.0);
            verify(membreRepository, times(1)).save(joueur);
        }

        @Test
        @DisplayName("✅ should NOT add outstanding balance when solde is 0")
        void shouldNotAddBalanceWhenSoldeIsZero() {
            joueur.setSolde(0.0);

            when(reservationService.getById(1L)).thenReturn(reservation);
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            // montant reste 15€
            assertThat(result.getMontant()).isEqualTo(15.0);

            // membreRepository.save() ne doit pas être appelé
            verify(membreRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when payment already done")
        void shouldThrowWhenAlreadyPaid() {
            paiement.setStatut(StatutPaiement.PAYE);

            when(reservationService.getById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> paiementService.pay(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Payment already done");

            verify(paiementRepository, never()).save(any());
            verify(reservationService, never()).confirm(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when member does not own the reservation")
        void shouldThrowWhenWrongMember() {
            // membreId = 99L → pas le propriétaire de la réservation (joueur.id = 2L)
            when(reservationService.getById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> paiementService.pay(1L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only the member of this reservation can pay");

            verify(paiementRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw ResourceNotFoundException when reservation not found")
        void shouldThrowWhenReservationNotFound() {
            when(reservationService.getById(99L))
                    .thenThrow(new ResourceNotFoundException("Reservation not found with id : 99"));

            assertThatThrownBy(() -> paiementService.pay(99L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation not found with id : 99");
        }

        @Test
        @DisplayName("✅ should set datePaiement when payment is processed")
        void shouldSetDatePaiement() {
            when(reservationService.getById(1L)).thenReturn(reservation);
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paiement result = paiementService.pay(1L, 2L);

            assertThat(result.getDatePaiement()).isNotNull();
        }
    }

    // ================================================================
    // GET
    // ================================================================
    @Nested
    @DisplayName("getById() and getByReservationId()")
    class GetTests {

        @Test
        @DisplayName("✅ should return payment when id exists")
        void shouldReturnPaymentById() {
            when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));

            Paiement result = paiementService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getMontant()).isEqualTo(15.0);
            assertThat(result.getStatut()).isEqualTo(StatutPaiement.EN_ATTENTE);
        }

        @Test
        @DisplayName("❌ should throw ResourceNotFoundException when payment id not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paiementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paiementService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment not found with id : 99");
        }

        @Test
        @DisplayName("✅ should return payment by reservation id")
        void shouldReturnPaymentByReservationId() {
            when(paiementRepository.findByReservationId(1L)).thenReturn(Optional.of(paiement));

            Paiement result = paiementService.getByReservationId(1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutPaiement.EN_ATTENTE);
        }

        @Test
        @DisplayName("❌ should throw ResourceNotFoundException when no payment for reservation")
        void shouldThrowWhenNoPaymentForReservation() {
            when(paiementRepository.findByReservationId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paiementService.getByReservationId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment not found for reservation : 99");
        }

        @Test
        @DisplayName("✅ should return all payments for a member")
        void shouldReturnPaymentsByMemberId() {
            Paiement paiement2 = Paiement.builder()
                    .reservation(reservation)
                    .montant(15.0)
                    .statut(StatutPaiement.PAYE)
                    .build();

            when(paiementRepository.findByReservationMembreId(2L))
                    .thenReturn(List.of(paiement, paiement2));

            List<Paiement> result = paiementService.getByMembreId(2L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Paiement::getStatut)
                    .containsExactlyInAnyOrder(
                            StatutPaiement.EN_ATTENTE,
                            StatutPaiement.PAYE
                    );
        }

        @Test
        @DisplayName("✅ should return empty list when member has no payments")
        void shouldReturnEmptyListWhenNoPayments() {
            when(paiementRepository.findByReservationMembreId(99L))
                    .thenReturn(List.of());

            List<Paiement> result = paiementService.getByMembreId(99L);

            assertThat(result).isEmpty();
        }
    }

    // ================================================================
    // SCHEDULER — checkUnpaidBeforeMatch
    // ================================================================
    @Nested
    @DisplayName("checkUnpaidBeforeMatch()")
    class SchedulerTests {

        @Test
        @DisplayName("✅ should cancel unpaid reservation and add balance to organizer")
        void shouldCancelUnpaidReservationAndAddBalanceToOrganizer() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            match.setDate(tomorrow);

            // paiement EN_ATTENTE pour un match de demain
            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE))
                    .thenReturn(List.of(paiement));
            when(membreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paiementService.checkUnpaidBeforeMatch();

            // réservation annulée
            verify(reservationService, times(1)).cancel(reservation.getId());

            // solde ajouté à l'organisateur
            assertThat(organisateur.getSolde()).isEqualTo(15.0);
            verify(membreRepository, times(1)).save(organisateur);
        }

        @Test
        @DisplayName("✅ should NOT cancel reservation when match is not tomorrow")
        void shouldNotCancelWhenMatchIsNotTomorrow() {
            // match dans 5 jours → pas touché par le scheduler
            match.setDate(LocalDate.now().plusDays(5));

            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE))
                    .thenReturn(List.of(paiement));

            paiementService.checkUnpaidBeforeMatch();

            verify(reservationService, never()).cancel(any());
            verify(membreRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ should NOT cancel when no unpaid reservations exist")
        void shouldNotCancelWhenNoUnpaidReservations() {
            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE))
                    .thenReturn(List.of());

            paiementService.checkUnpaidBeforeMatch();

            verify(reservationService, never()).cancel(any());
            verify(membreRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ should add correct outstanding balance — prixParJoueur to organizer")
        void shouldAddCorrectBalanceToOrganizer() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            match.setDate(tomorrow);
            match.setPrixParJoueur(15.0);
            organisateur.setSolde(0.0);

            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE))
                    .thenReturn(List.of(paiement));
            when(membreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paiementService.checkUnpaidBeforeMatch();

            // organisateur récupère exactement 15€ de dette
            assertThat(organisateur.getSolde()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("✅ should accumulate balance when organizer already has outstanding balance")
        void shouldAccumulateBalanceWhenOrganizerAlreadyHasBalance() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            match.setDate(tomorrow);
            organisateur.setSolde(15.0); // déjà 15€ de dette

            when(paiementRepository.findByStatut(StatutPaiement.EN_ATTENTE))
                    .thenReturn(List.of(paiement));
            when(membreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paiementService.checkUnpaidBeforeMatch();

            // 15€ existants + 15€ nouveaux = 30€
            assertThat(organisateur.getSolde()).isEqualTo(30.0);
        }
    }
}