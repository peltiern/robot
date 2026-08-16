package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.decisionnel.ArbitrageActivites.Decision;
import fr.roboteek.robot.securite.ArretUrgence;
import fr.roboteek.robot.util.HorlogeReglable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie l'arbitrage des demandes d'activité : logique pure, horloge maîtrisée, sans Spring
 * ni thread de cerveau.
 * <p>
 * La temporisation est celle par défaut de {@code RobotConfig} (aucun {@code robot.properties}
 * n'est lisible en test, Owner retombe donc sur les {@code @DefaultValue}) : 30 secondes.
 */
class ArbitrageActivitesTest {

    private HorlogeReglable horloge;
    private ArretUrgence arretUrgence;
    private ArbitrageActivites arbitrage;

    private ActiviteFactice conversation;
    private ActiviteFactice jeu;
    private ActiviteFactice accueil;

    @BeforeEach
    void setUp() {
        horloge = new HorlogeReglable(Instant.parse("2026-08-12T10:00:00Z"));
        // Publieur muet : seul l'état de l'arrêt d'urgence nous intéresse ici.
        arretUrgence = new ArretUrgence(evenement -> {
        });
        arbitrage = new ArbitrageActivites(arretUrgence, horloge);

        conversation = new ActiviteFactice("Conversation", AbstractActivity.PRIORITE_NORMALE);
        jeu = new ActiviteFactice("Jeu", AbstractActivity.PRIORITE_NORMALE);
        accueil = new ActiviteFactice("Accueil", AbstractActivity.PRIORITE_HAUTE);
    }

    @Test
    void uneDemandeOrdinaireEstAcceptee() {
        assertTrue(arbitrage.arbitrer(jeu, conversation).estAcceptee());
    }

    @Test
    void uneDemandeEstAccepteeMemeSansActiviteEnCours() {
        assertTrue(arbitrage.arbitrer(jeu, null).estAcceptee());
    }

    @Test
    void lActiviteDejaEnCoursNEstPasRelancee() {
        assertEquals(Decision.DEJA_EN_COURS, arbitrage.arbitrer(jeu, jeu));
    }

    @Test
    void rienNeDemarrePendantUnArretDUrgence() {
        arretUrgence.declencher("test");

        assertEquals(Decision.ARRET_URGENCE, arbitrage.arbitrer(jeu, conversation));
    }

    @Test
    void lesDemandesRepassentUneFoisLArretDUrgenceRearme() {
        arretUrgence.declencher("test");
        arretUrgence.rearmer("test");

        assertTrue(arbitrage.arbitrer(jeu, conversation).estAcceptee());
    }

    @Test
    void uneActivitePrioritaireInterromptUneActiviteOrdinaire() {
        assertTrue(arbitrage.arbitrer(accueil, conversation).estAcceptee());
    }

    @Test
    void uneActiviteOrdinaireNInterromptPasUneActivitePrioritaire() {
        assertEquals(Decision.PRIORITE_INSUFFISANTE, arbitrage.arbitrer(jeu, accueil));
    }

    @Test
    void deuxActivitesDeMemePrioriteSeCedentLaPlace() {
        assertTrue(arbitrage.arbitrer(jeu, conversation).estAcceptee());
        assertTrue(arbitrage.arbitrer(conversation, jeu).estAcceptee());
    }

    @Test
    void uneActiviteQuiVientDeSeTerminerNeRepartPasAussitot() {
        arbitrage.noterFinExecution(jeu);
        avancerDe(5);

        assertEquals(Decision.RELANCE_TROP_TOT, arbitrage.arbitrer(jeu, conversation));
    }

    @Test
    void laTemporisationFinitParExpirer() {
        arbitrage.noterFinExecution(jeu);
        avancerDe(31);

        assertTrue(arbitrage.arbitrer(jeu, conversation).estAcceptee());
    }

    @Test
    void laTemporisationNeVautQuePourLActiviteConcernee() {
        arbitrage.noterFinExecution(jeu);

        assertTrue(arbitrage.arbitrer(accueil, conversation).estAcceptee(), "l'accueil n'a rien à voir avec le jeu");
        assertFalse(arbitrage.arbitrer(jeu, conversation).estAcceptee());
    }

    @Test
    void laTemporisationCourtDepuisLaDerniereFin() {
        arbitrage.noterFinExecution(jeu);
        avancerDe(20);
        // L'activité a retourné, puis a retourné à nouveau plus tard : c'est la seconde fin
        // qui compte, sans quoi une activité qui s'interrompt en boucle finirait par passer.
        arbitrage.noterFinExecution(jeu);
        avancerDe(20);

        assertEquals(Decision.RELANCE_TROP_TOT, arbitrage.arbitrer(jeu, conversation));
    }

    private void avancerDe(long secondes) {
        horloge.avancerDe(Duration.ofSeconds(secondes));
    }

    /** Activité de test : un nom, une priorité, et rien d'autre. */
    private static final class ActiviteFactice extends AbstractActivity {

        private final String nom;

        private final int priorite;

        private ActiviteFactice(String nom, int priorite) {
            this.nom = nom;
            this.priorite = priorite;
        }

        @Override
        public String identifiant() {
            return nom;
        }

        @Override
        public int priorite() {
            return priorite;
        }

        @Override
        public void init() {
            initialized = true;
        }

        @Override
        public boolean run() {
            return false;
        }
    }
}
