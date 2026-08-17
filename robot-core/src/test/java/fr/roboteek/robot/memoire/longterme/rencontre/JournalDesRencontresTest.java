package fr.roboteek.robot.memoire.longterme.rencontre;

import fr.roboteek.robot.memoire.longterme.BaseMemoireDeTest;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import fr.roboteek.robot.systemenerveux.event.RencontreEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreSansSuiteEvent;
import fr.roboteek.robot.util.HorlogeReglable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie l'histoire que le robot garde de ses rencontres : ce qui s'y inscrit, ce qui s'en
 * reprend, et ce qui n'y entre jamais.
 */
class JournalDesRencontresTest {

    /** Instant de départ, dans le fuseau de la machine : l'horloge raisonne en instants. */
    private static final LocalDateTime DEBUT = LocalDateTime.of(2026, 8, 16, 10, 0);

    @TempDir
    File dossierTemp;

    private HorlogeReglable horloge;

    private RencontreRepository rencontreRepository;

    private JournalDesRencontres journal;

    private Personne marie;

    private Personne paul;

    @BeforeEach
    void setUp() {
        DataSource memoire = BaseMemoireDeTest.dans(dossierTemp);
        PersonneRepository personneRepository = new PersonneRepository(memoire);
        rencontreRepository = new RencontreRepository(memoire);
        horloge = new HorlogeReglable(DEBUT.atZone(ZoneId.systemDefault()).toInstant());
        journal = new JournalDesRencontres(rencontreRepository, horloge);

        marie = Personne.nouvelle("Marie");
        paul = Personne.nouvelle("Paul");
        personneRepository.enregistrer(marie);
        personneRepository.enregistrer(paul);
    }

    @Test
    void uneHistoireVideNeContientRien() {
        assertTrue(journal.pourPersonne(marie.id()).isEmpty());
        assertTrue(journal.nombreParPersonne().isEmpty());
    }

    /** La date qui compte le plus dans une timeline : le jour où on a fait connaissance. */
    @Test
    void laPremiereRencontreOuvreLHistoire() {
        journal.inscrirePremiereRencontre(marie);

        List<Rencontre> histoire = journal.pourPersonne(marie.id());

        assertEquals(1, histoire.size());
        assertEquals(Rencontre.Type.PREMIERE, histoire.getFirst().type());
        assertEquals(-1, histoire.getFirst().secondesDAbsence());
        assertEquals(DEBUT, histoire.getFirst().instant());
    }

    @Test
    void chaqueRetourAjouteUneLigne() {
        journal.inscrirePremiereRencontre(marie);
        avancerDe(3600);
        journal.handleRencontreEvent(new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, marie, 3600));

        List<Rencontre> histoire = journal.pourPersonne(marie.id());

        assertEquals(2, histoire.size());
        // La plus récente d'abord : c'est dans ce sens qu'une timeline se lit.
        assertEquals(Rencontre.Type.RETOUR, histoire.getFirst().type());
        assertEquals(3600, histoire.getFirst().secondesDAbsence());
        assertEquals(Rencontre.Type.PREMIERE, histoire.getLast().type());
    }

    /** Un inconnu n'a pas d'identité : il n'y a personne à qui rattacher une ligne. */
    @Test
    void unInconnuNEntrePasDansLHistoire() {
        journal.handleRencontreEvent(new RencontreEvent(RencontreEvent.TYPE.INCONNU, null, -1));

        assertTrue(journal.nombreParPersonne().isEmpty());
    }

    /**
     * Le cerveau a refusé : la rencontre est rendue, elle n'a pas eu lieu.
     * <p>
     * Sans cette reprise, quelqu'un qui reste devant le robot pendant qu'une activité l'occupe
     * verrait sa timeline se remplir d'une ligne toutes les cinq secondes — vu sur le robot le
     * 2026-08-16, où Sandra a été annoncée quatre fois en deux minutes.
     */
    @Test
    void uneRencontreRefuseeEstRepriseDeLHistoire() {
        journal.handleRencontreEvent(new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, marie, 30));
        journal.handleRencontreSansSuiteEvent(
                new RencontreSansSuiteEvent(marie, "retrouvailles refusées : PRIORITE_INSUFFISANTE"));

        assertTrue(journal.pourPersonne(marie.id()).isEmpty());
    }

    /** Et le refus ne reprend que la dernière, pas toute l'histoire. */
    @Test
    void unRefusNeRependQueLaDerniereLigne() {
        journal.inscrirePremiereRencontre(marie);
        avancerDe(600);
        journal.handleRencontreEvent(new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, marie, 600));
        journal.handleRencontreSansSuiteEvent(new RencontreSansSuiteEvent(marie, "refusée"));

        List<Rencontre> histoire = journal.pourPersonne(marie.id());

        assertEquals(1, histoire.size());
        assertEquals(Rencontre.Type.PREMIERE, histoire.getFirst().type());
    }

    /** Deux refus d'affilée ne doivent pas manger la ligne d'avant. */
    @Test
    void unSecondRefusSansRencontreEntreLesDeuxNEffaceRien() {
        journal.inscrirePremiereRencontre(marie);
        avancerDe(600);
        journal.handleRencontreEvent(new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, marie, 600));
        journal.handleRencontreSansSuiteEvent(new RencontreSansSuiteEvent(marie, "refusée"));
        journal.handleRencontreSansSuiteEvent(new RencontreSansSuiteEvent(marie, "refusée encore"));

        assertEquals(1, journal.pourPersonne(marie.id()).size());
    }

    @Test
    void lesHistoiresRestentSeparees() {
        journal.inscrirePremiereRencontre(marie);
        journal.inscrirePremiereRencontre(paul);
        avancerDe(600);
        journal.handleRencontreEvent(new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, paul, 600));

        assertEquals(Map.of(marie.id(), 1, paul.id(), 2), journal.nombreParPersonne());
        assertEquals(1, journal.pourPersonne(marie.id()).size());
    }

    /** Quelqu'un qui vit dans la pièce est rencontré sans fin : l'histoire doit rester bornée. */
    @Test
    void lHistoireEstElaguee() {
        journal.inscrirePremiereRencontre(marie);
        for (int i = 0; i < 250; i++) {
            avancerDe(60);
            journal.handleRencontreEvent(new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, marie, 60));
        }

        assertEquals(200, rencontreRepository.nombreParPersonne().get(marie.id()));
        // La plus ancienne conservée n'est pas la première : c'est bien le début qui a été élagué.
        List<Rencontre> gardees = rencontreRepository.parPersonne(marie.id(), 200);
        assertTrue(gardees.stream().noneMatch(rencontre -> rencontre.type() == Rencontre.Type.PREMIERE));
    }

    private void avancerDe(long secondes) {
        horloge.avancerDe(Duration.ofSeconds(secondes));
    }
}
