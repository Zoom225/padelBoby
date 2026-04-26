package com.padelPlay.service;

import com.padelPlay.entity.*;
import com.padelPlay.entity.enums.StatutMatch;
import com.padelPlay.entity.enums.StatutReservation;
import com.padelPlay.entity.enums.TypeMatch;
import com.padelPlay.entity.enums.TypeMembre;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.JourFermetureRepository;
import com.padelPlay.repository.MatchRepository;
import com.padelPlay.repository.ReservationRepository;
import com.padelPlay.service.impl.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService tests")
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private JourFermetureRepository jourFermetureRepository;

    @Mock
    private MembreService membreService;

    @Mock
    private TerrainService terrainService;

    @InjectMocks
    private MatchServiceImpl matchService;

    private Site site;
    private Terrain terrain;
    private Membre organisateurGlobal;
    private Membre organisateurSite;
    private Membre organisateurLibre;
    private Match matchPrive;
    private Match matchPublic;

    @BeforeEach
    void setUp() {
        site = Site.builder()
                .nom("Padel Club Lyon")
                .adresse("12 rue de la République")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeMatchMinutes(90)
                .dureeEntreMatchMinutes(15)
                .anneeCivile(2025)
                .build();

        // ← initialiser la liste vide pour éviter NullPointerException
        site.setJoursFermeture(new ArrayList<>());

        terrain = Terrain.builder()
                .nom("Court A")
                .site(site)
                .build();

        organisateurGlobal = Membre.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .typeMembre(TypeMembre.GLOBAL)
                .solde(0.0)
                .build();
        organisateurGlobal.setId(1L);

        organisateurSite = Membre.builder()
                .matricule("S10001")
                .nom("Bernard")
                .prenom("Tom")
                .typeMembre(TypeMembre.SITE)
                .solde(0.0)
                .site(site)
                .build();
        organisateurSite.setId(2L);

        organisateurLibre = Membre.builder()
                .matricule("L10001")
                .nom("Petit")
                .prenom("Alex")
                .typeMembre(TypeMembre.LIBRE)
                .solde(0.0)
                .build();
        organisateurLibre.setId(3L);

        matchPrive = Match.builder()
                .terrain(terrain)
                .organisateur(organisateurGlobal)
                .date(LocalDate.now().plusDays(25))
                .heureDebut(LocalTime.of(15, 0))
                .heureFin(LocalTime.of(16, 30))
                .typeMatch(TypeMatch.PRIVE)
                .statut(StatutMatch.PLANIFIE)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();

        matchPublic = Match.builder()
                .terrain(terrain)
                .organisateur(organisateurGlobal)
                .date(LocalDate.now().plusDays(25))
                .heureDebut(LocalTime.of(17, 0))
                .heureFin(LocalTime.of(18, 30))
                .typeMatch(TypeMatch.PUBLIC)
                .statut(StatutMatch.PLANIFIE)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();
    }

    // ================================================================
    // CREATE
    // ================================================================
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("✅ should create a PRIVE match with valid organizer GLOBAL — 25 days in advance")
        void shouldCreatePriveMatchWithGlobalMember() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);
            when(matchRepository.findByTerrainId(any())).thenReturn(List.of());
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            Match result = matchService.create(match, 1L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getTypeMatch()).isEqualTo(TypeMatch.PRIVE);
            assertThat(result.getStatut()).isEqualTo(StatutMatch.PLANIFIE);
            assertThat(result.getPrixTotal()).isEqualTo(60.0);
            assertThat(result.getPrixParJoueur()).isEqualTo(15.0);
            assertThat(result.getHeureFin()).isEqualTo(LocalTime.of(16, 30));
        }

        @Test
        @DisplayName("✅ should create a PUBLIC match with valid organizer GLOBAL")
        void shouldCreatePublicMatch() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);
            when(matchRepository.findByTerrainId(any())).thenReturn(List.of());
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(17, 0))
                    .typeMatch(TypeMatch.PUBLIC)
                    .build();

            Match result = matchService.create(match, 1L, 1L);

            assertThat(result.getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);
            assertThat(result.getStatut()).isEqualTo(StatutMatch.PLANIFIE);
        }

        @Test
        @DisplayName("✅ should auto calculate heureFin from site dureeMatchMinutes")
        void shouldAutoCalculateHeureFin() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);
            when(matchRepository.findByTerrainId(any())).thenReturn(List.of());
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(10, 0)) // début 10h00
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            Match result = matchService.create(match, 1L, 1L);

            // site.dureeMatchMinutes = 90 → fin = 10h00 + 90min = 11h30
            assertThat(result.getHeureFin()).isEqualTo(LocalTime.of(11, 30));
        }

        @Test
        @DisplayName("❌ should throw BusinessException when organizer has outstanding balance")
        void shouldThrowWhenOrganizerHasBalance() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(true);

            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(match, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("outstanding balance");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when organizer has active penalty")
        void shouldThrowWhenOrganizerHasPenalty() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(true);

            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(match, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("active penalty");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when GLOBAL books less than 21 days in advance")
        void shouldThrowWhenGlobalBooksTooLate() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);

            // GLOBAL doit réserver 21 jours avant → 15 jours c'est trop tard
            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(15))
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(match, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("21 days in advance");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when SITE books less than 14 days in advance")
        void shouldThrowWhenSiteBooksTooLate() {
            when(membreService.getById(1L)).thenReturn(organisateurSite);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);

            // SITE doit réserver 14 jours avant → 10 jours c'est trop tard
            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(10))
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(match, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("14 days in advance");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when LIBRE books less than 5 days in advance")
        void shouldThrowWhenLibreBooksTooLate() {
            when(membreService.getById(1L)).thenReturn(organisateurLibre);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);

            // LIBRE doit réserver 5 jours avant → 3 jours c'est trop tard
            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(3))
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(match, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("5 days in advance");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ should allow GLOBAL to book exactly 21 days in advance")
        void shouldAllowGlobalToBookExactly21Days() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);
            when(matchRepository.findByTerrainId(any())).thenReturn(List.of());
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // exactement 21 jours → doit passer
            Match match = Match.builder()
                    .date(LocalDate.now().plusDays(21))
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatCode(() -> matchService.create(match, 1L, 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("❌ should throw BusinessException when slot already taken on same date")
        void shouldThrowWhenSlotAlreadyTaken() {
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);

            LocalDate matchDate = LocalDate.now().plusDays(25);

            // un match existe déjà sur ce terrain à cette date
            Match existingMatch = Match.builder()
                    .terrain(terrain)
                    .date(matchDate)
                    .heureDebut(LocalTime.of(15, 0))
                    .heureFin(LocalTime.of(16, 30))
                    .statut(StatutMatch.PLANIFIE)
                    .build();

            when(matchRepository.findByTerrainId(any()))
                    .thenReturn(List.of(existingMatch));

            Match newMatch = Match.builder()
                    .date(matchDate)
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(newMatch, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already booked");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when site is closed on match date")
        void shouldThrowWhenSiteIsClosed() {
            LocalDate closedDate = LocalDate.now().plusDays(25);

            JourFermeture jourFermeture = JourFermeture.builder()
                    .date(closedDate)
                    .raison("Maintenance")
                    .global(false)
                    .site(site)
                    .build();

            site.setJoursFermeture(List.of(jourFermeture));

            when(jourFermetureRepository.findByGlobalTrue()).thenReturn(List.of());
            when(jourFermetureRepository.findBySiteId(site.getId())).thenReturn(List.of(jourFermeture));

            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);

            Match match = Match.builder()
                    .date(closedDate)
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(match, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("closed");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when date is globally closed")
        void shouldThrowWhenDateIsGloballyClosed() {
            LocalDate closedDate = LocalDate.now().plusDays(30);

            JourFermeture globalClosure = JourFermeture.builder()
                    .date(closedDate)
                    .raison("Global closure")
                    .global(true)
                    .site(null)
                    .build();

            when(jourFermetureRepository.findByGlobalTrue()).thenReturn(List.of(globalClosure));
            when(jourFermetureRepository.findBySiteId(site.getId())).thenReturn(List.of());

            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);
            when(matchRepository.findByTerrainId(any())).thenReturn(List.of());

            Match match = Match.builder()
                    .date(closedDate)
                    .heureDebut(LocalTime.of(15, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(match, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("closed");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when break between matches is not respected")
        void shouldThrowWhenBreakBetweenMatchesIsNotRespected() {

            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);

            Match existingMatch = Match.builder()
                    .terrain(terrain)
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(15, 0))
                    .heureFin(LocalTime.of(16, 30))
                    .statut(StatutMatch.PLANIFIE)
                    .build();

            when(matchRepository.findByTerrainId(any())).thenReturn(List.of(existingMatch));

            Match newMatch = Match.builder()
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(16, 35))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            assertThatThrownBy(() -> matchService.create(newMatch, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already booked");

            verify(matchRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ should allow slot when break between matches is respected")
        void shouldAllowSlotWhenBreakIsRespected() {
            when(jourFermetureRepository.findByGlobalTrue()).thenReturn(List.of());
            when(jourFermetureRepository.findBySiteId(site.getId())).thenReturn(List.of());

            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(membreService.hasOutstandingBalance(1L)).thenReturn(false);
            when(membreService.hasActivePenalty(1L)).thenReturn(false);

            Match existingMatch = Match.builder()
                    .terrain(terrain)
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(15, 0))
                    .heureFin(LocalTime.of(16, 30))
                    .statut(StatutMatch.PLANIFIE)
                    .build();

            when(matchRepository.findByTerrainId(any())).thenReturn(List.of(existingMatch));
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Match newMatch = Match.builder()
                    .date(LocalDate.now().plusDays(25))
                    .heureDebut(LocalTime.of(16, 45))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            Match result = matchService.create(newMatch, 1L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getHeureFin()).isEqualTo(LocalTime.of(18, 15));
        }
    }

    // ================================================================
    // UPDATE
    // ================================================================
    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("✅ should allow organizer to update match when start is more than 24h away")
        void shouldUpdateWhenMoreThan24HoursBeforeStart() {
            Match existing = Match.builder()
                    .terrain(terrain)
                    .organisateur(organisateurGlobal)
                    .date(LocalDate.now().plusDays(30))
                    .heureDebut(LocalTime.of(10, 0))
                    .heureFin(LocalTime.of(11, 30))
                    .typeMatch(TypeMatch.PRIVE)
                    .statut(StatutMatch.PLANIFIE)
                    .prixTotal(60.0)
                    .prixParJoueur(15.0)
                    .build();
            existing.setId(1L);

            Match update = Match.builder()
                    .date(LocalDate.now().plusDays(31))
                    .heureDebut(LocalTime.of(12, 0))
                    .typeMatch(TypeMatch.PUBLIC)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);
            when(matchRepository.findByTerrainId(1L)).thenReturn(List.of(existing));
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Match result = matchService.update(1L, update, 1L, 1L);

            assertThat(result.getDate()).isEqualTo(update.getDate());
            assertThat(result.getHeureDebut()).isEqualTo(LocalTime.of(12, 0));
            assertThat(result.getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);
            assertThat(result.getHeureFin()).isEqualTo(LocalTime.of(13, 30));
        }

        @Test
        @DisplayName("❌ should reject update less than 24h before start")
        void shouldRejectUpdateWhenLessThan24HoursBeforeStart() {
            LocalDateTime startSoon = LocalDateTime.now().plusHours(23);

            Match existing = Match.builder()
                    .terrain(terrain)
                    .organisateur(organisateurGlobal)
                    .date(startSoon.toLocalDate())
                    .heureDebut(startSoon.toLocalTime().withSecond(0).withNano(0))
                    .heureFin(startSoon.toLocalTime().withSecond(0).withNano(0).plusMinutes(90))
                    .typeMatch(TypeMatch.PRIVE)
                    .statut(StatutMatch.PLANIFIE)
                    .prixTotal(60.0)
                    .prixParJoueur(15.0)
                    .build();
            existing.setId(1L);

            Match update = Match.builder()
                    .date(startSoon.plusDays(1).toLocalDate())
                    .heureDebut(LocalTime.of(14, 0))
                    .typeMatch(TypeMatch.PRIVE)
                    .build();

            when(matchRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(membreService.getById(1L)).thenReturn(organisateurGlobal);
            when(terrainService.getById(1L)).thenReturn(terrain);

            assertThatThrownBy(() -> matchService.update(1L, update, 1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("less than 24 hours");

            verify(matchRepository, never()).save(any());
        }
    }

    // ================================================================
    // GET
    // ================================================================
    @Nested
    @DisplayName("getById()")
    class GetTests {

        @Test
        @DisplayName("✅ should return match when id exists")
        void shouldReturnMatchById() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPrive));

            Match result = matchService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getTypeMatch()).isEqualTo(TypeMatch.PRIVE);
        }

        @Test
        @DisplayName("❌ should throw ResourceNotFoundException when id not found")
        void shouldThrowWhenMatchNotFound() {
            when(matchRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Match not found with id : 99");
        }

        @Test
        @DisplayName("✅ should return only PUBLIC PLANIFIE matches")
        void shouldReturnOnlyPublicAvailableMatches() {
            when(matchRepository.findByTypeMatchAndStatut(TypeMatch.PUBLIC, StatutMatch.PLANIFIE))
                    .thenReturn(List.of(matchPublic));

            List<Match> result = matchService.getPublicAvailableMatches();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);
            assertThat(result.getFirst().getStatut()).isEqualTo(StatutMatch.PLANIFIE);
        }
    }

    // ================================================================
    // CONVERT TO PUBLIC
    // ================================================================
    @Nested
    @DisplayName("convertToPublic()")
    class ConvertToPublicTests {

        @Test
        @DisplayName("✅ should convert PRIVE match to PUBLIC and apply penalty to organizer")
        void shouldConvertPriveToPublic() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPrive));
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.convertToPublic(1L);

            assertThat(matchPrive.getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);
            assertThat(matchPrive.getDateConversionPublic()).isNotNull();

            // pénalité appliquée à l'organisateur
            verify(membreService, times(1)).addPenalty(matchPrive.getOrganisateur().getId());
        }

        @Test
        @DisplayName("❌ should throw BusinessException when match is already PUBLIC")
        void shouldThrowWhenMatchAlreadyPublic() {
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPublic));

            assertThatThrownBy(() -> matchService.convertToPublic(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already public");

            verify(membreService, never()).addPenalty(any());
        }
    }

    // ================================================================
    // INCREMENT / DECREMENT PLAYERS
    // ================================================================
    @Nested
    @DisplayName("incrementPlayers() and decrementPlayers() with reservation counts")
    class PlayersTests {

        @Test
        @DisplayName("✅ should keep PLANIFIE when confirmed reservations are below 4")
        void shouldIncrementPlayers() {
            matchPrive.setId(1L);
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPrive));
            when(reservationRepository.countReservationsByMatchIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(2L);
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.incrementPlayers(1L);

            assertThat(matchPrive.getStatut()).isEqualTo(StatutMatch.PLANIFIE);
        }

        @Test
        @DisplayName("✅ should set statut to COMPLET when confirmed reservations reach 4")
        void shouldSetCompletWhenFull() {
            matchPrive.setId(1L);
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPrive));
            when(reservationRepository.countReservationsByMatchIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(4L);
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.incrementPlayers(1L);

            assertThat(matchPrive.getStatut()).isEqualTo(StatutMatch.COMPLET);
        }

        @Test
        @DisplayName("❌ should throw BusinessException when confirmed reservations are already above 4")
        void shouldThrowWhenMatchAlreadyFull() {
            matchPrive.setId(1L);
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPrive));
            when(reservationRepository.countReservationsByMatchIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(5L);

            assertThatThrownBy(() -> matchService.incrementPlayers(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already full");
        }

        @Test
        @DisplayName("✅ should reset statut to PLANIFIE when confirmed reservations are below 4")
        void shouldDecrementPlayers() {
            matchPrive.setId(1L);
            matchPrive.setStatut(StatutMatch.COMPLET);
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPrive));
            when(reservationRepository.countReservationsByMatchIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(3L);
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.decrementPlayers(1L);

            assertThat(matchPrive.getStatut()).isEqualTo(StatutMatch.PLANIFIE);
        }

        @Test
        @DisplayName("✅ should keep PLANIFIE when confirmed reservations are 0")
        void shouldKeepPlanifieWhenNoConfirmedPlayers() {
            matchPrive.setId(1L);
            when(matchRepository.findById(1L)).thenReturn(Optional.of(matchPrive));
            when(reservationRepository.countReservationsByMatchIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(0L);
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.decrementPlayers(1L);

            assertThat(matchPrive.getStatut()).isEqualTo(StatutMatch.PLANIFIE);
        }
    }

    // ================================================================
    // IS MATCH FULL
    // ================================================================
    @Nested
    @DisplayName("isMatchFull()")
    class IsMatchFullTests {

        @Test
        @DisplayName("✅ should return true when match has 4 confirmed reservations")
        void shouldReturnTrueWhenFull() {
            when(reservationRepository.countReservationsByMatchIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(4L);

            assertThat(matchService.isMatchFull(1L)).isTrue();
        }

        @Test
        @DisplayName("✅ should return false when match has less than 4 confirmed reservations")
        void shouldReturnFalseWhenNotFull() {
            when(reservationRepository.countReservationsByMatchIdAndStatut(1L, StatutReservation.CONFIRMEE)).thenReturn(2L);

            assertThat(matchService.isMatchFull(1L)).isFalse();
        }
    }

    // ================================================================
    // SCHEDULER
    // ================================================================
    @Nested
    @DisplayName("checkAndConvertExpiredPrivateMatches()")
    class SchedulerTests {

        @Test
        @DisplayName("✅ should convert all PRIVE PLANIFIE matches scheduled for tomorrow with less than 4 players")
        void shouldConvertExpiredPrivateMatches() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            Match expiredMatch = Match.builder()
                    .terrain(terrain)
                    .organisateur(organisateurGlobal)
                    .date(tomorrow)
                    .typeMatch(TypeMatch.PRIVE)
                    .statut(StatutMatch.PLANIFIE)
                    .build();
            expiredMatch.setId(20L);

            when(matchRepository.findByDateAndStatut(tomorrow, StatutMatch.PLANIFIE))
                    .thenReturn(List.of(expiredMatch));
            when(reservationRepository.countReservationsByMatchIdAndStatut(20L, StatutReservation.CONFIRMEE)).thenReturn(2L);
            when(matchRepository.findById(any())).thenReturn(Optional.of(expiredMatch));
            when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.checkAndConvertExpiredPrivateMatches();

            assertThat(expiredMatch.getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);
            verify(membreService, times(1)).addPenalty(any());
        }

        @Test
        @DisplayName("✅ should NOT convert PRIVE match that is already full — 4 players")
        void shouldNotConvertFullPrivateMatch() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            Match fullMatch = Match.builder()
                    .terrain(terrain)
                    .organisateur(organisateurGlobal)
                    .date(tomorrow)
                    .typeMatch(TypeMatch.PRIVE)
                    .statut(StatutMatch.PLANIFIE)
                    .build();
            fullMatch.setId(21L);

            when(matchRepository.findByDateAndStatut(tomorrow, StatutMatch.PLANIFIE))
                    .thenReturn(List.of(fullMatch));
            when(reservationRepository.countReservationsByMatchIdAndStatut(21L, StatutReservation.CONFIRMEE)).thenReturn(4L);

            matchService.checkAndConvertExpiredPrivateMatches();

            // le match reste PRIVE
            assertThat(fullMatch.getTypeMatch()).isEqualTo(TypeMatch.PRIVE);
            verify(membreService, never()).addPenalty(any());
        }

        @Test
        @DisplayName("✅ should NOT convert PUBLIC match during scheduler run")
        void shouldNotConvertPublicMatch() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            Match publicMatch = Match.builder()
                    .terrain(terrain)
                    .organisateur(organisateurGlobal)
                    .date(tomorrow)
                    .typeMatch(TypeMatch.PUBLIC)
                    .statut(StatutMatch.PLANIFIE)
                    .build();

            when(matchRepository.findByDateAndStatut(tomorrow, StatutMatch.PLANIFIE))
                    .thenReturn(List.of(publicMatch));

            matchService.checkAndConvertExpiredPrivateMatches();

            assertThat(publicMatch.getTypeMatch()).isEqualTo(TypeMatch.PUBLIC);
            verify(membreService, never()).addPenalty(any());
        }
    }
}