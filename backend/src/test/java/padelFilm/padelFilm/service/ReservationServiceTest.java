package com.padelPlay.service;

import com.padelPlay.entity.*;
import com.padelPlay.entity.enums.*;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.PaiementRepository;
import com.padelPlay.repository.ReservationRepository;
import com.padelPlay.service.impl.ReservationServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService tests")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private MatchService matchService;

    @Mock
    private MembreService membreService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Site site;
    private Site siteB;
    private Terrain terrain;
    private Membre organisateur;
    private Membre joueur;
    private Membre joueurSiteB;
    private Match matchPrive;
    private Match matchPublic;
    private Match matchComplet;

    @BeforeEach
    void setUp() {
        siteB = Site.builder()
                .nom("Padel Club Paris")
                .build();
        siteB.setId(2L);

        site = Site.builder()
                .nom("Padel Club Lyon")
                .build();
        site.setId(1L);

        terrain = Terrain.builder()
                .nom("Court A")
                .site(site)
                .build();
        terrain.setId(1L);

        organisateur = Membre.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .typeMembre(TypeMembre.GLOBAL)
                .solde(0.0)
                .site(null)
                .build();
        organisateur.setId(1L);

        joueur = Membre.builder()
                .matricule("G1002")
                .nom("Dupont")
                .prenom("Julie")
                .typeMembre(TypeMembre.GLOBAL)
                .solde(0.0)
                .site(null)
                .build();
        joueur.setId(2L);

        // membre rattaché à un autre site → ne peut pas réserver sur siteLyon
        joueurSiteB = Membre.builder()
                .matricule("S20001")
                .nom("Leclerc")
                .prenom("Paul")
                .typeMembre(TypeMembre.SITE)
                .solde(0.0)
                .site(siteB)
                .build();
        joueurSiteB.setId(3L);

        matchPrive = Match.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .date(LocalDate.now().plusDays(25))
                .heureDebut(LocalTime.of(15, 0))
                .heureFin(LocalTime.of(16, 30))
                .typeMatch(TypeMatch.PRIVE)
                .statut(StatutMatch.PLANIFIE)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();
        matchPrive.setId(10L);

        matchPublic = Match.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .date(LocalDate.now().plusDays(25))
                .heureDebut(LocalTime.of(17, 0))
                .heureFin(LocalTime.of(18, 30))
                .typeMatch(TypeMatch.PUBLIC)
                .statut(StatutMatch.PLANIFIE)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();
        matchPublic.setId(11L);

        matchComplet = Match.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .date(LocalDate.now().plusDays(25))
                .heureDebut(LocalTime.of(19, 0))
                .heureFin(LocalTime.of(20, 30))
                .typeMatch(TypeMatch.PUBLIC)
                .statut(StatutMatch.COMPLET)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();
        matchComplet.setId(12L);
    }

    // ================================================================
    // CREATE
    // ================================================================
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("✅ should create reservation for PUBLIC match with valid member")
        void shouldCreateReservationForPublicMatch() {
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(membreService.hasActivePenalty(2L)).thenReturn(false);
            when(membreService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByMatchIdAndMembreId(11L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Reservation result = reservationService.create(11L, 2L, 2L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
            assertThat(result.getMembre()).isEqualTo(joueur);
            assertThat(result.getMatch()).isEqualTo(matchPublic);

            // vérifier que le paiement EN_ATTENTE est créé automatiquement
            verify(paiementRepository, times(1)).save(argThat(paiement ->
                    paiement.getMontant().equals(15.0) &&
                            paiement.getStatut() == StatutPaiement.EN_ATTENTE
            ));
        }

        @Test
        @DisplayName("❌ should throw BusinessException when requesterId is missing")
        void shouldThrowWhenRequesterIdIsMissing() {
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Requester id is required");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when member tries to join PUBLIC match for someone else")
        void shouldThrowWhenRequesterTriesPublicJoinForAnotherMember() {
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(reservationRepository.existsByMatchIdAndMembreId(11L, 2L)).thenReturn(false);
            when(membreService.hasActivePenalty(2L)).thenReturn(false);
            when(membreService.hasOutstandingBalance(2L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Public match : a member can only join for themselves");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ should create reservation for PRIVE match when organizer adds a player")
        void shouldCreateReservationForPriveMatchByOrganizer() {
            when(matchService.getById(10L)).thenReturn(matchPrive);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(membreService.hasActivePenalty(2L)).thenReturn(false);
            when(membreService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.existsByMatchIdAndMembreId(10L, 2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // l'organisateur (id=1) ajoute joueur (id=2) → autorisé
            Reservation result = reservationService.create(10L, 2L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        }

        @Test
        @DisplayName("❌ should throw BusinessException when match is COMPLET")
        void shouldThrowWhenMatchIsFull() {
            when(matchService.getById(12L)).thenReturn(matchComplet);
            when(membreService.getById(2L)).thenReturn(joueur);

            assertThatThrownBy(() -> reservationService.create(12L, 2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already full");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when match is ANNULE")
        void shouldThrowWhenMatchIsCancelled() {
            matchPublic.setStatut(StatutMatch.ANNULE);
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cancelled");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when member already registered in match")
        void shouldThrowWhenMemberAlreadyRegistered() {
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(reservationRepository.existsByMatchIdAndMembreId(11L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already registered");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when member has active penalty")
        void shouldThrowWhenMemberHasActivePenalty() {
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(reservationRepository.existsByMatchIdAndMembreId(11L, 2L)).thenReturn(false);
            when(membreService.hasActivePenalty(2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("active penalty");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when member has outstanding balance")
        void shouldThrowWhenMemberHasOutstandingBalance() {
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(reservationRepository.existsByMatchIdAndMembreId(11L, 2L)).thenReturn(false);
            when(membreService.hasActivePenalty(2L)).thenReturn(false);
            when(membreService.hasOutstandingBalance(2L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(11L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("outstanding balance");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when non-organizer tries to join PRIVE match")
        void shouldThrowWhenNonOrganizerJoinsPriveMatch() {
            // joueur (id=2) essaie de rejoindre directement un match privé
            // sans passer par l'organisateur (id=1)
            when(matchService.getById(10L)).thenReturn(matchPrive);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(reservationRepository.existsByMatchIdAndMembreId(10L, 2L)).thenReturn(false);
            when(membreService.hasActivePenalty(2L)).thenReturn(false);
            when(membreService.hasOutstandingBalance(2L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(10L, 2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("only the organizer can add players");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when SITE member books on wrong site")
        void shouldThrowWhenSiteMemberBooksOnWrongSite() {
            // joueurSiteB est rattaché à Paris → ne peut pas réserver sur Lyon
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(3L)).thenReturn(joueurSiteB);
            when(reservationRepository.existsByMatchIdAndMembreId(11L, 3L)).thenReturn(false);
            when(membreService.hasActivePenalty(3L)).thenReturn(false);
            when(membreService.hasOutstandingBalance(3L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(11L, 3L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("only book on their own site");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ paiement created with correct amount = prixParJoueur of match")
        void shouldCreatePaiementWithCorrectAmount() {
            when(matchService.getById(11L)).thenReturn(matchPublic);
            when(membreService.getById(2L)).thenReturn(joueur);
            when(reservationRepository.existsByMatchIdAndMembreId(11L, 2L)).thenReturn(false);
            when(membreService.hasActivePenalty(2L)).thenReturn(false);
            when(membreService.hasOutstandingBalance(2L)).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.create(11L, 2L, 2L);

            // prixParJoueur = 60€ / 4 = 15€
            verify(paiementRepository).save(argThat(p ->
                    p.getMontant().equals(15.0) &&
                            p.getStatut() == StatutPaiement.EN_ATTENTE
            ));
        }
    }

    // ================================================================
    // CANCEL
    // ================================================================
    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("✅ should cancel EN_ATTENTE reservation without decrementing match players")
        void shouldCancelReservation() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.EN_ATTENTE)
                    .build();

            Reservation reservation = Reservation.builder()
                    .match(matchPublic)
                    .membre(joueur)
                    .statut(StatutReservation.EN_ATTENTE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
            verify(matchService, never()).decrementPlayers(any());
        }

        @Test
        @DisplayName("✅ should refund payment when reservation is cancelled after payment")
        void shouldRefundPaymentWhenCancelledAfterPayment() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.PAYE) // déjà payé
                    .build();

            Reservation reservation = Reservation.builder()
                    .match(matchPublic)
                    .membre(joueur)
                    .statut(StatutReservation.CONFIRMEE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            // paiement doit passer à REMBOURSE
            assertThat(paiement.getStatut()).isEqualTo(StatutPaiement.REMBOURSE);
            verify(paiementRepository, times(1)).save(paiement);
            verify(matchService, times(1)).decrementPlayers(matchPublic.getId());
        }

        @Test
        @DisplayName("✅ should NOT refund payment when reservation cancelled before payment")
        void shouldNotRefundWhenNotYetPaid() {
            Paiement paiement = Paiement.builder()
                    .montant(15.0)
                    .statut(StatutPaiement.EN_ATTENTE) // pas encore payé
                    .build();

            Reservation reservation = Reservation.builder()
                    .match(matchPublic)
                    .membre(joueur)
                    .statut(StatutReservation.EN_ATTENTE)
                    .paiement(paiement)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.cancel(1L);

            // paiement reste EN_ATTENTE — pas de remboursement
            assertThat(paiement.getStatut()).isEqualTo(StatutPaiement.EN_ATTENTE);
            verify(paiementRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when reservation already cancelled")
        void shouldThrowWhenAlreadyCancelled() {
            Reservation reservation = Reservation.builder()
                    .match(matchPublic)
                    .membre(joueur)
                    .statut(StatutReservation.ANNULEE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.cancel(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already cancelled");

            verify(matchService, never()).decrementPlayers(any());
        }

        @Test
        @DisplayName("❌ should throw ResourceNotFoundException when reservation not found")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancel(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation not found with id : 99");
        }
    }

    // ================================================================
    // CONFIRM
    // ================================================================
    @Nested
    @DisplayName("confirm()")
    class ConfirmTests {

        @Test
        @DisplayName("✅ should confirm reservation and increment match players")
        void shouldConfirmReservation() {
            Reservation reservation = Reservation.builder()
                    .match(matchPublic)
                    .membre(joueur)
                    .statut(StatutReservation.EN_ATTENTE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reservationService.confirm(1L);

            assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
            verify(matchService, times(1)).incrementPlayers(matchPublic.getId());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when reservation already confirmed")
        void shouldThrowWhenAlreadyConfirmed() {
            Reservation reservation = Reservation.builder()
                    .match(matchPublic)
                    .membre(joueur)
                    .statut(StatutReservation.CONFIRMEE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.confirm(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already confirmed");

            verify(matchService, never()).incrementPlayers(any());
        }
    }

    // ================================================================
    // GET
    // ================================================================
    @Nested
    @DisplayName("getById()")
    class GetTests {

        @Test
        @DisplayName("✅ should return reservation when id exists")
        void shouldReturnReservationById() {
            Reservation reservation = Reservation.builder()
                    .match(matchPublic)
                    .membre(joueur)
                    .statut(StatutReservation.EN_ATTENTE)
                    .build();
            reservation.setId(1L);

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

            Reservation result = reservationService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        }

        @Test
        @DisplayName("❌ should throw ResourceNotFoundException when id not found")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation not found with id : 99");
        }
    }
}