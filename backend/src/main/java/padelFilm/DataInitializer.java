package com.padelPlay;

import com.padelPlay.entity.*;
import com.padelPlay.entity.enums.TypeAdministrateur;
import com.padelPlay.entity.enums.TypeMembre;
import com.padelPlay.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SiteRepository siteRepository;
    private final TerrainRepository terrainRepository;
    private final MembreRepository membreRepository;
    private final AdministrateurRepository administrateurRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        int currentYear = LocalDate.now().getYear();

        // seeding uniquement si la BDD est vide
        if (siteRepository.count() > 0) {
            log.info("DataInitializer : database already seeded, skipping...");
            return;
        }

        log.info("DataInitializer : seeding database...");

        // ----------------------------------------------------------------
        // Sites
        // ----------------------------------------------------------------
        Site siteLyon = siteRepository.save(Site.builder()
                .nom("Padel Club Lyon")
                .adresse("12 rue de la République, Lyon")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeMatchMinutes(90)
                .dureeEntreMatchMinutes(15)
                .anneeCivile(currentYear)
                .build());

        Site siteParis = siteRepository.save(Site.builder()
                .nom("Padel Club Paris")
                .adresse("45 avenue des Champs, Paris")
                .heureOuverture(LocalTime.of(7, 0))
                .heureFermeture(LocalTime.of(23, 0))
                .dureeMatchMinutes(90)
                .dureeEntreMatchMinutes(15)
                .anneeCivile(currentYear)
                .build());

        log.info("DataInitializer : {} sites created", siteRepository.count());

        // ----------------------------------------------------------------
        // Terrains
        // ----------------------------------------------------------------
        terrainRepository.save(Terrain.builder().nom("Court A").site(siteLyon).build());
        terrainRepository.save(Terrain.builder().nom("Court B").site(siteLyon).build());
        terrainRepository.save(Terrain.builder().nom("Court C").site(siteLyon).build());

        terrainRepository.save(Terrain.builder().nom("Court 1").site(siteParis).build());
        terrainRepository.save(Terrain.builder().nom("Court 2").site(siteParis).build());

        log.info("DataInitializer : {} courts created", terrainRepository.count());

        // ----------------------------------------------------------------
        // Jours de fermeture
        // ----------------------------------------------------------------

        // fermeture globale (tous les sites)
        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(currentYear, 12, 25))
                .raison("Christmas Day")
                .global(true)
                .site(null)
                .build());

        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(currentYear, 1, 1))
                .raison("New Year's Day")
                .global(true)
                .site(null)
                .build());

        // fermeture spécifique au site Lyon
        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(currentYear, 7, 14))
                .raison("Bastille Day — Lyon site maintenance")
                .global(false)
                .site(siteLyon)
                .build());

        log.info("DataInitializer : closing days created");

        // ----------------------------------------------------------------
        // Membres
        // ----------------------------------------------------------------
        membreRepository.save(Membre.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas.martin@email.com")
                .typeMembre(TypeMembre.GLOBAL)
                .solde(0.0)
                .site(null)
                .build());

        membreRepository.save(Membre.builder()
                .matricule("G1002")
                .nom("Dubois")
                .prenom("Emma")
                .email("emma.dubois@email.com")
                .typeMembre(TypeMembre.GLOBAL)
                .solde(0.0)
                .site(null)
                .build());

        membreRepository.save(Membre.builder()
                .matricule("S10001")
                .nom("Bernard")
                .prenom("Tom")
                .email("tom.bernard@email.com")
                .typeMembre(TypeMembre.SITE)
                .solde(0.0)
                .site(siteLyon)
                .build());

        membreRepository.save(Membre.builder()
                .matricule("S10002")
                .nom("Leroy")
                .prenom("Sarah")
                .email("sarah.leroy@email.com")
                .typeMembre(TypeMembre.SITE)
                .solde(0.0)
                .site(siteParis)
                .build());

        membreRepository.save(Membre.builder()
                .matricule("L10001")
                .nom("Petit")
                .prenom("Alex")
                .email("alex.petit@email.com")
                .typeMembre(TypeMembre.LIBRE)
                .solde(0.0)
                .site(null)
                .build());

        log.info("DataInitializer : {} members created", membreRepository.count());

        // ----------------------------------------------------------------
        // Administrateurs
        // ----------------------------------------------------------------
        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN001")
                .nom("Admin")
                .prenom("Global")
                .email("admin@padel.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .typeAdministrateur(TypeAdministrateur.GLOBAL)
                .site(null)
                .build());

        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN002")
                .nom("Admin")
                .prenom("Lyon")
                .email("admin.lyon@padel.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .typeAdministrateur(TypeAdministrateur.SITE)
                .site(siteLyon)
                .build());

        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN003")
                .nom("Admin")
                .prenom("Paris")
                .email("admin.paris@padel.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .typeAdministrateur(TypeAdministrateur.SITE)
                .site(siteParis)
                .build());

        log.info("DataInitializer : {} admins created", administrateurRepository.count());
        log.info("DataInitializer : seeding completed successfully !");
    }
}
