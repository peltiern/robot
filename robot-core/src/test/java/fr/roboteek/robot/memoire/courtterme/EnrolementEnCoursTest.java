package fr.roboteek.robot.memoire.courtterme;

import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la procédure d'apprentissage d'un visage : combien d'empreintes, sous quel délai, et
 * surtout <b>ce qui est écrit en mémoire longue</b>. Sans webcam ni OpenCV — cette logique n'a
 * jamais eu besoin d'une image, elle était seulement enfermée dans l'organe de vision.
 */
class EnrolementEnCoursTest {

    private static final String ID_MARIE = "id-marie";

    private EnrolementEnCours enrolement;

    private final List<EnrolementTermineEvent> resultats = new ArrayList<>();

    /** Ce qui a été écrit en mémoire longue, et rien d'autre : le test tient le stylo. */
    private final List<List<float[]>> ecrituresEnMemoireLongue = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ApplicationEventPublisher publieur = evenement -> {
            if (evenement instanceof EnrolementTermineEvent resultat) {
                resultats.add(resultat);
            }
        };
        enrolement = new EnrolementEnCours(publieur);
    }

    @Test
    void cinqEmpreintesSuffisentAApprendreUnVisage() {
        enrolement.demarrer(ID_MARIE, ecrituresEnMemoireLongue::add);

        for (int i = 0; i < 5; i++) {
            enrolement.avancer(EnrolementEnCoursTest::empreinte);
        }

        assertEquals(1, ecrituresEnMemoireLongue.size(), "une seule écriture, à la fin");
        assertEquals(5, ecrituresEnMemoireLongue.getFirst().size());
        assertEquals(1, resultats.size());
        assertTrue(resultats.getFirst().isReussi());
        assertEquals(5, resultats.getFirst().getNombreEmpreintes());
    }

    /** Rien n'est écrit avant la fin : un enrôlement interrompu ne laisse pas de personne à moitié apprise. */
    @Test
    void rienNEstEcritAvantLeCompte() {
        enrolement.demarrer(ID_MARIE, ecrituresEnMemoireLongue::add);

        for (int i = 0; i < 4; i++) {
            enrolement.avancer(EnrolementEnCoursTest::empreinte);
        }

        assertTrue(ecrituresEnMemoireLongue.isEmpty(), "rien ne doit être écrit");
        assertTrue(resultats.isEmpty(), "personne n'attend un résultat tant que ce n'est pas fini");
    }

    /** Les cycles sans visage ne comptent pas, mais ne font pas échouer l'apprentissage. */
    @Test
    void lesCyclesSansVisageNeComptentPas() {
        enrolement.demarrer(ID_MARIE, ecrituresEnMemoireLongue::add);

        for (int i = 0; i < 10; i++) {
            enrolement.avancer(() -> null);
        }

        assertTrue(resultats.isEmpty(), "le délai n'est pas écoulé, l'enrôlement continue");
    }

    @Test
    void horsEnrolementRienNEstDemandeNiReleve() {
        enrolement.avancer(() -> {
            throw new AssertionError("l'empreinte ne doit pas être calculée hors d'un enrôlement");
        });

        assertTrue(resultats.isEmpty());
    }

    /** La vision indisponible : celui qui attend le résultat doit être libéré quand même. */
    @Test
    void renoncerRepondQuandMeme() {
        enrolement.renoncer(ID_MARIE, "modèles absents");

        assertEquals(1, resultats.size());
        assertFalse(resultats.getFirst().isReussi());
        assertEquals(ID_MARIE, resultats.getFirst().getIdPersonne());
        assertTrue(ecrituresEnMemoireLongue.isEmpty(), "rien ne doit être écrit");
    }

    /**
     * L'écriture échoue : le résultat part quand même, et annonce l'échec.
     * <p>
     * Vécu le 2026-08-16 : la base a refusé les empreintes, l'exception a été avalée par la boucle
     * vidéo, et l'activité de présentation a attendu dix secondes avant de conclure à un visage mal
     * cadré. Une panne d'écriture doit se dire, pas se déguiser.
     */
    @Test
    void uneEcritureQuiEchoueRepondQuandMeme() {
        enrolement.demarrer(ID_MARIE, empreintes -> {
            throw new IllegalStateException("la base refuse");
        });

        for (int i = 0; i < 5; i++) {
            enrolement.avancer(EnrolementEnCoursTest::empreinte);
        }

        assertEquals(1, resultats.size(), "celui qui attend ne doit jamais rester suspendu");
        assertFalse(resultats.getFirst().isReussi());
        assertEquals(ID_MARIE, resultats.getFirst().getIdPersonne());
        assertEquals(0, resultats.getFirst().getNombreEmpreintes(), "rien n'a été retenu");
    }

    /** Et l'échec n'immobilise pas l'enrôlement suivant. */
    @Test
    void unEnrolementSuivantUnEchecDEcritureAboutit() {
        enrolement.demarrer(ID_MARIE, empreintes -> {
            throw new IllegalStateException("la base refuse");
        });
        for (int i = 0; i < 5; i++) {
            enrolement.avancer(EnrolementEnCoursTest::empreinte);
        }

        enrolement.demarrer("id-paul", ecrituresEnMemoireLongue::add);
        for (int i = 0; i < 5; i++) {
            enrolement.avancer(EnrolementEnCoursTest::empreinte);
        }

        assertEquals(1, ecrituresEnMemoireLongue.size());
        assertTrue(resultats.getLast().isReussi());
        assertEquals("id-paul", resultats.getLast().getIdPersonne());
    }

    @Test
    void unNouvelEnrolementRepartDeZero() {
        enrolement.demarrer(ID_MARIE, ecrituresEnMemoireLongue::add);
        enrolement.avancer(EnrolementEnCoursTest::empreinte);
        enrolement.avancer(EnrolementEnCoursTest::empreinte);

        enrolement.demarrer("id-paul", ecrituresEnMemoireLongue::add);
        for (int i = 0; i < 5; i++) {
            enrolement.avancer(EnrolementEnCoursTest::empreinte);
        }

        assertEquals(1, ecrituresEnMemoireLongue.size(), "seul le second enrôlement a abouti");
        assertEquals(5, ecrituresEnMemoireLongue.getFirst().size(), "les empreintes du premier sont oubliées");
        assertEquals("id-paul", resultats.getFirst().getIdPersonne());
    }

    private static float[] empreinte() {
        return new float[]{0.1f, 0.2f, 0.3f};
    }
}
