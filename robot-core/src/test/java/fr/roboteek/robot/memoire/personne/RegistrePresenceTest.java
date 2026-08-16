package fr.roboteek.robot.memoire.personne;

import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreInconnuInaboutieEvent;
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

    /** Cadence de la reconnaissance de visages sur le robot : une image sur trois, ~10 images/s. */
    private static final double CYCLES_PAR_SECONDE = 3;

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
        percevoirUnInconnuPendant(2.0);

        assertEquals(1, rencontres.size());
        assertEquals(RencontreEvent.TYPE.INCONNU, rencontres.get(0).getType());
        assertNull(rencontres.get(0).getPersonne());
        assertEquals(-1, rencontres.get(0).getSecondesDAbsence());

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

    /**
     * Le cœur de la règle : des perceptions éparses ne valent pas une présence, même si elles
     * s'étalent bien au-delà de la durée exigée. Sans quoi le robot aborde quelqu'un qu'il n'a
     * fait qu'entrevoir — ou pire, quelqu'un qu'il connaît mais reconnaît mal.
     */
    @Test
    void desPerceptionsEparsesNeSuffisentPasAConfirmerUnePresence() {
        for (int i = 0; i < 10; i++) {
            percevoirUnInconnu();
            avancerDe(2.0);
        }

        assertTrue(rencontres.isEmpty(), "vingt secondes écoulées, mais jamais deux vues qui se suivent");
    }

    /**
     * L'arbitrage demandé : un même visage sort tantôt reconnu, tantôt inconnu. La lecture
     * majoritaire l'emporte, et le robot ne demande pas son prénom à quelqu'un qu'il reconnaît
     * trois fois sur cinq.
     */
    @Test
    void unVisageMajoritairementReconnuNeDeclenchePasDInconnu() {
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", null));

        // Trois perceptions sur cinq la reconnaissent, deux la manquent.
        for (int i = 0; i < 4; i++) {
            percevoirMarie();
            avancerDe(1 / 3.0);
            percevoirMarie();
            avancerDe(1 / 3.0);
            percevoirUnInconnu();
            avancerDe(1 / 3.0);
            percevoirMarie();
            avancerDe(1 / 3.0);
            percevoirUnInconnu();
            avancerDe(1 / 3.0);
        }

        assertTrue(rencontres.stream().noneMatch(rencontre -> rencontre.getType() == RencontreEvent.TYPE.INCONNU),
                "aucun inconnu ne doit être annoncé, rencontres : " + rencontres.size());
    }

    /** Et l'inverse : un vrai inconnu, majoritaire, doit bien être annoncé. */
    @Test
    void unInconnuMajoritaireEstAnnonceMalgreUneReconnaissanceIsolee() {
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", null));

        for (int i = 0; i < 4; i++) {
            percevoirUnInconnu();
            avancerDe(1 / 3.0);
            percevoirUnInconnu();
            avancerDe(1 / 3.0);
            percevoirMarie();
            avancerDe(1 / 3.0);
            percevoirUnInconnu();
            avancerDe(1 / 3.0);
        }

        assertTrue(rencontres.stream().anyMatch(rencontre -> rencontre.getType() == RencontreEvent.TYPE.INCONNU),
                "l'inconnu est majoritaire, il doit être annoncé");
    }

    /**
     * Une rencontre d'inconnu qui s'est révélée être une méprise ne doit pas consommer le tour du
     * prochain inconnu, celui-là bien réel.
     */
    @Test
    void unInconnuEstAnnonceANouveauApresUneMeprise() {
        percevoirUnInconnuPendant(2.0);
        assertEquals(1, rencontres.size());

        registre.handleRencontreInconnuInaboutieEvent(new RencontreInconnuInaboutieEvent("Nicolas"));

        // Bien avant les 120 s de temporisation, qui ne s'appliquent plus.
        avancerDe(10.0);
        percevoirUnInconnuPendant(2.0);

        assertEquals(2, rencontres.size(), "le vrai inconnu doit être annoncé");
    }

    @Test
    void quelquUnQuiResteNestPasResalueQuandLaTemporisationExpire() {
        percevoirUnInconnuPendant(2.0);
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
        percevoirUnInconnuPendant(2.0);
        assertEquals(1, rencontres.size());

        // Parti (plus de 4 s sans être vu), puis revenu assez longtemps : nouvelle venue,
        // mais la temporisation de 120 s n'est pas écoulée.
        avancerDe(10.0);
        percevoirUnInconnuPendant(2.0);

        assertEquals(1, rencontres.size());
    }

    @Test
    void unRetourApresLaTemporisationEstAnnonceANouveau() {
        percevoirUnInconnuPendant(2.0);

        avancerDe(200.0);
        percevoirUnInconnuPendant(2.0);

        assertEquals(2, rencontres.size());
    }

    @Test
    void deuxInconnusSimultanesNeComptentQuePourUnePresence() {
        percevoirPendant(2.0, inconnu(), inconnu());

        assertEquals(1, rencontres.size());
    }

    @Test
    void unePersonneConnueDeclencheDesRetrouvailles() {
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", null));

        percevoirMariePendant(2.0);

        assertEquals(1, rencontres.size());
        RencontreEvent rencontre = rencontres.get(0);
        assertEquals(RencontreEvent.TYPE.CONNU_REVU, rencontre.getType());
        assertEquals("Marie", rencontre.getPersonne().prenom());
        assertEquals(-1, rencontre.getSecondesDAbsence(), "jamais rencontrée jusqu'ici");
    }

    /**
     * Ce qui est rapporté est la <b>vraie absence</b> — le temps pendant lequel la personne n'a
     * pas été vue — et non le temps écoulé depuis la dernière rencontre annoncée. Quelqu'un peut
     * parler sans discontinuer pendant dix minutes sans qu'aucune rencontre ne soit annoncée.
     */
    @Test
    void laDureeDeLAbsenceEstRapportee() {
        LocalDateTime ilYATroisJours = LocalDateTime.now(horloge).minusDays(3);
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", ilYATroisJours));

        // Première venue : personne ne sait combien de temps elle a duré, d'où -1.
        percevoirMariePendant(2.0);
        assertEquals(1, rencontres.size());
        assertEquals(-1, rencontres.get(0).getSecondesDAbsence(), "première apparition");

        // Elle disparaît une trentaine de secondes, puis revient : c'est ce trou-là qui compte.
        avancerDe(30.0);
        percevoirMariePendant(2.0);

        assertEquals(2, rencontres.size());
        long absence = rencontres.get(1).getSecondesDAbsence();
        assertTrue(absence >= 30 && absence <= 32, "environ 30 s d'absence attendues, obtenu " + absence);
    }

    /**
     * Le pendant du test précédent : quelqu'un vu sans interruption pendant des minutes ne doit
     * jamais se voir attribuer une absence, même si la temporisation entre rencontres a expiré.
     */
    @Test
    void quelquUnVuSansInterruptionNaJamaisDAbsence() {
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", null));

        percevoirMariePendant(180.0);

        assertEquals(1, rencontres.size(), "une seule venue, donc une seule rencontre");
    }

    @Test
    void laRencontreEstDateeDansLaBase() {
        personneRepository.enregistrer(new Personne(ID_MARIE, "Marie", null));

        percevoirMariePendant(2.0);

        Personne marie = personneRepository.parId(ID_MARIE);
        assertNotNull(marie.derniereRencontre());
        // Datée à l'instant où la présence a été confirmée, donc pendant la venue simulée.
        assertTrue(!marie.derniereRencontre().isAfter(LocalDateTime.now(horloge)),
                "la rencontre ne peut pas être datée du futur : " + marie.derniereRencontre());
        assertTrue(marie.derniereRencontre().isAfter(LocalDateTime.now(horloge).minusSeconds(2)),
                "la rencontre doit dater de la venue en cours : " + marie.derniereRencontre());
    }

    @Test
    void unVisageRattacheAUnePersonneEffaceeEstTraiteCommeUnInconnu() {
        // La base des personnes a été vidée sans celle des visages : plutôt que d'entretenir
        // une identité fantôme, le robot refait connaissance.
        percevoirMariePendant(2.0);

        assertEquals(1, rencontres.size());
        assertEquals(RencontreEvent.TYPE.INCONNU, rencontres.get(0).getType());
        assertNull(rencontres.get(0).getPersonne());
    }

    @Test
    void unePresenceSansVisageNeDeclencheRien() {
        percevoirPendant(10.0);

        assertTrue(rencontres.isEmpty());
    }

    private void avancerDe(double secondes) {
        horloge.avancerDe(Duration.ofMillis((long) (secondes * 1000)));
    }

    private void percevoirUnInconnu() {
        percevoir(inconnu());
    }

    private void percevoirMarie() {
        percevoir(marie());
    }

    private void percevoirUnInconnuPendant(double secondes) {
        percevoirPendant(secondes, inconnu());
    }

    private void percevoirMariePendant(double secondes) {
        percevoirPendant(secondes, marie());
    }

    /**
     * Perçoit sans discontinuer pendant la durée demandée, à la cadence réelle de la
     * reconnaissance (environ trois cycles par seconde).
     * <p>
     * Deux perceptions espacées de deux secondes ne valent pas une présence de deux secondes, et
     * le registre a désormais raison de les refuser : c'est exactement ce qui lui faisait aborder
     * quelqu'un qu'il n'avait fait qu'entrevoir.
     */
    private void percevoirPendant(double secondes, VisagePercu... visages) {
        int cycles = (int) Math.round(secondes * CYCLES_PAR_SECONDE);
        for (int i = 0; i < cycles; i++) {
            percevoir(visages);
            avancerDe(1.0 / CYCLES_PAR_SECONDE);
        }
    }

    private static VisagePercu marie() {
        return new VisagePercu(ID_MARIE, "Marie", 100, 100, 50, 50);
    }

    private static VisagePercu inconnu() {
        return new VisagePercu(null, null, 100, 100, 50, 50);
    }

    private void percevoir(VisagePercu... visages) {
        registre.handleVisagePercuEvent(new VisagePercuEvent(List.of(visages), 640, 480));
    }

    @Test
    void unVisageAppisLibereLaPlaceDesInconnus() {
        // Un premier inconnu est salué, puis enrôlé : le suivant est forcément quelqu'un d'autre
        // et doit être annoncé, alors que la temporisation des inconnus court encore.
        percevoirUnInconnuPendant(2.0);
        assertEquals(1, rencontres.size());

        registre.handleEnrolementTermineEvent(new EnrolementTermineEvent("id-nouveau", 5, true));

        avancerDe(1.0);
        percevoirUnInconnuPendant(2.0);

        assertEquals(2, rencontres.size(), "le second inconnu est bien annoncé");
    }

    @Test
    void unVisageAppisNeDeclenchePasDeRetrouvaillesImmediates() {
        // Sans précaution, la personne dont on vient d'apprendre le visage est reconnue dans la
        // seconde et le robot lui annonce des retrouvailles.
        personneRepository.enregistrer(Personne.nouvelle("Marie"));
        Personne marie = personneRepository.toutes().iterator().next();

        registre.handleEnrolementTermineEvent(new EnrolementTermineEvent(marie.id(), 5, true));

        percevoir(new VisagePercu(marie.id(), "Marie", 100, 100, 50, 50));
        avancerDe(3.0);
        percevoir(new VisagePercu(marie.id(), "Marie", 100, 100, 50, 50));

        assertTrue(rencontres.isEmpty(), "on vient de faire connaissance, pas de retrouvailles");
    }
}
