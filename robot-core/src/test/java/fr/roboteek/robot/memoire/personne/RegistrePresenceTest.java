package fr.roboteek.robot.memoire.personne;

import fr.roboteek.robot.systemenerveux.event.RencontreEvent;
import fr.roboteek.robot.util.HorlogeReglable;
import fr.roboteek.robot.systemenerveux.event.VisagePercu;
import fr.roboteek.robot.systemenerveux.event.VisagePercuEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie l'hystérésis du registre de présence : logique pure, horloge maîtrisée, sans webcam
 * ni Spring.
 * <p>
 * Les seuils sont ceux par défaut de {@code RobotConfig} (aucun {@code robot.properties} n'est
 * lisible en test, Owner retombe donc sur les {@code @DefaultValue}) : 1,5 s de présence à
 * confirmer, 4 s d'absence avant de considérer quelqu'un parti, 120 s entre deux rencontres.
 */
class RegistrePresenceTest {

    private static final String ID_MARIE = "id-marie";

    @TempDir
    File dossierTemp;

    private HorlogeReglable horloge;
    private PersonneRepository personneRepository;
    private RegistrePresence registre;
    private List<RencontreEvent> rencontres;

    @BeforeEach
    void setUp() {
        horloge = new HorlogeReglable(Instant.parse("2026-08-09T15:00:00Z"));
        personneRepository = new PersonneRepository(new File(dossierTemp, "personnes.db").getAbsolutePath());
        rencontres = new ArrayList<>();
        ApplicationEventPublisher publieur = evenement -> {
            if (evenement instanceof RencontreEvent rencontre) {
                rencontres.add(rencontre);
            }
        };
        registre = new RegistrePresence(personneRepository, publieur, horloge);
    }

    @AfterEach
    void tearDown() {
        personneRepository.close();
    }

    @Test
    void quelquUnQuiNeFaitQuePasserNeDeclencheRien() {
        percevoirUnInconnu();
        avancerDe(1.0);
        percevoirUnInconnu();

        assertTrue(rencontres.isEmpty(), "présence trop brève pour être confirmée");
    }

    @Test
    void unInconnuPresentAssezLongtempsEstAnnonceUneSeuleFois() {
        percevoirUnInconnu();
        avancerDe(2.0);
        percevoirUnInconnu();

        assertEquals(1, rencontres.size());
        assertEquals(RencontreEvent.TYPE.INCONNU, rencontres.get(0).getType());
        assertNull(rencontres.get(0).getPersonne());
        assertEquals(-1, rencontres.get(0).getSecondesDepuisDerniereRencontre());

        // Il reste là : rien de plus ne doit partir.
        avancerDe(1.0);
        percevoirUnInconnu();
        assertEquals(1, rencontres.size());
    }

    @Test
    void leClignotementDeLaDetectionNeCassePasLaPresence() {
        // Trous de 300 à 700 ms entre deux perceptions, comme mesuré sur le robot : ils
        // restent sous le seuil d'absence, la venue ne doit donc pas repartir de zéro —
        // sinon la présence ne serait jamais confirmée et le robot n'aborderait personne.
        percevoirUnInconnu();
        avancerDe(0.3);
        percevoirUnInconnu();
        avancerDe(0.7);
        percevoirUnInconnu();
        avancerDe(0.6);
        percevoirUnInconnu();

        assertEquals(1, rencontres.size(), "1,6 s de présence cumulée malgré les trous");
    }

    @Test
    void quelquUnQuiResteNestPasResalueQuandLaTemporisationExpire() {
        percevoirUnInconnu();
        avancerDe(2.0);
        percevoirUnInconnu();
        assertEquals(1, rencontres.size());

        // Il ne bouge pas pendant cinq minutes, en restant vu en continu : la venue a déjà été
        // tranchée, l'expiration de la temporisation ne doit pas le faire resaluer.
        for (int i = 0; i < 100; i++) {
            avancerDe(3.0);
            percevoirUnInconnu();
        }

        assertEquals(1, rencontres.size());
    }

    @Test
    void unRetourApresUneVraieAbsenceMaisAvantLaTemporisationNeRedeclenchePas() {
        percevoirUnInconnu();
        avancerDe(2.0);
        percevoirUnInconnu();
        assertEquals(1, rencontres.size());

        // Parti (plus de 4 s sans être vu), puis revenu assez longtemps : nouvelle venue,
        // mais la temporisation de 120 s n'est pas écoulée.
        avancerDe(10.0);
        percevoirUnInconnu();
        avancerDe(2.0);
        percevoirUnInconnu();

        assertEquals(1, rencontres.size());
    }

    @Test
    void unRetourApresLaTemporisationEstAnnonceANouveau() {
        percevoirUnInconnu();
        avancerDe(2.0);
        percevoirUnInconnu();

        avancerDe(200.0);
        percevoirUnInconnu();
        avancerDe(2.0);
        percevoirUnInconnu();

        assertEquals(2, rencontres.size());
    }

    @Test
    void deuxInconnusSimultanesNeComptentQuePourUnePresence() {
        percevoir(inconnu(), inconnu());
        avancerDe(2.0);
        percevoir(inconnu(), inconnu());

        assertEquals(1, rencontres.size());
    }

    @Test
    void unePersonneConnueDeclencheDesRetrouvailles() {
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", null, null));

        percevoirMarie();
        avancerDe(2.0);
        percevoirMarie();

        assertEquals(1, rencontres.size());
        RencontreEvent rencontre = rencontres.get(0);
        assertEquals(RencontreEvent.TYPE.CONNU_REVU, rencontre.getType());
        assertEquals("Marie", rencontre.getPersonne().prenom());
        assertEquals(-1, rencontre.getSecondesDepuisDerniereRencontre(), "jamais rencontrée jusqu'ici");
    }

    @Test
    void leTempsEcouleDepuisLaDerniereRencontreEstRapporte() {
        LocalDateTime ilYATroisJours = LocalDateTime.now(horloge).minusDays(3);
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", ilYATroisJours, "son chat"));

        percevoirMarie();
        avancerDe(2.0);
        percevoirMarie();

        assertEquals(1, rencontres.size());
        assertEquals(Duration.ofDays(3).toSeconds() + 2, rencontres.get(0).getSecondesDepuisDerniereRencontre());
        assertEquals("son chat", rencontres.get(0).getPersonne().resumeDerniereConversation());
    }

    @Test
    void laRencontreEstDateeDansLaBase() {
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", null, null));

        percevoirMarie();
        avancerDe(2.0);
        percevoirMarie();

        Personne marie = personneRepository.parId(ID_MARIE);
        assertNotNull(marie.derniereRencontre());
        assertEquals(LocalDateTime.now(horloge), marie.derniereRencontre());
    }

    @Test
    void unVisageRattacheAUnePersonneEffaceeEstTraiteCommeUnInconnu() {
        // La base des personnes a été vidée sans celle des visages : plutôt que d'entretenir
        // une identité fantôme, le robot refait connaissance.
        percevoirMarie();
        avancerDe(2.0);
        percevoirMarie();

        assertEquals(1, rencontres.size());
        assertEquals(RencontreEvent.TYPE.INCONNU, rencontres.get(0).getType());
        assertNull(rencontres.get(0).getPersonne());
    }

    @Test
    void unePresenceSansVisageNeDeclencheRien() {
        percevoir();
        avancerDe(10.0);
        percevoir();

        assertTrue(rencontres.isEmpty());
    }

    private void avancerDe(double secondes) {
        horloge.avancerDe(Duration.ofMillis((long) (secondes * 1000)));
    }

    private void percevoirUnInconnu() {
        percevoir(inconnu());
    }

    private void percevoirMarie() {
        percevoir(new VisagePercu(ID_MARIE, "Marie", 100, 100, 50, 50));
    }

    private static VisagePercu inconnu() {
        return new VisagePercu(null, null, 100, 100, 50, 50);
    }

    private void percevoir(VisagePercu... visages) {
        registre.handleVisagePercuEvent(new VisagePercuEvent(List.of(visages), 640, 480));
    }
}
