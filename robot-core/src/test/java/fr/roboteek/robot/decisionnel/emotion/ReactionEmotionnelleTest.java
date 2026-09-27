package fr.roboteek.robot.decisionnel.emotion;

import fr.roboteek.robot.decisionnel.emotion.ReactionEmotionnelle.Moment;
import fr.roboteek.robot.decisionnel.emotion.ReactionEmotionnelle.Reaction;
import fr.roboteek.robot.decisionnel.emotion.ReactionEmotionnelle.Reglages;
import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import fr.roboteek.robot.organes.actionneurs.animation.modele.SonDeclenche;
import fr.roboteek.robot.util.HorlogeReglable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Quand le robot réagit à une émotion, avec quelle animation, et à quel moment de la phrase. */
class ReactionEmotionnelleTest {

    @TempDir
    Path dossier;

    private BibliothequeDesAnimations bibliotheque;
    private final List<Animation> lancees = new ArrayList<>();
    private final Deque<Double> tirages = new ArrayDeque<>();
    private final HorlogeReglable horloge = new HorlogeReglable(Instant.parse("2026-09-27T14:00:00Z"));
    private ReactionEmotionnelle reaction;

    @BeforeEach
    void setUp() {
        bibliotheque = new BibliothequeDesAnimations(dossier);
        // Facteur 0,5, 20 s entre deux réactions, moitié « avant » : les réglages par défaut.
        reaction = new ReactionEmotionnelle(bibliotheque, lancees::add, tirages::removeFirst, horloge,
                () -> new Reglages(0.5, Duration.ofSeconds(20), 0.5));
    }

    private void tirer(Double... valeurs) {
        tirages.addAll(Arrays.asList(valeurs));
    }

    private void ranger(String nom, Emotion emotion, boolean avecSon) {
        bibliotheque.enregistrer(new Animation(nom, 1000,
                List.of(new Piste(Axe.COU_GAUCHE_DROITE, 40, 200, List.of(new ImageCle(0, 0), new ImageCle(1000, 10)))),
                avecSon ? List.of(new SonDeclenche(0, "content")) : List.of(), Animation.VERSION_COURANTE, emotion));
    }

    @Test
    void uneEmotionNeutreNeFaitJamaisReagir() {
        ranger("Bof", Emotion.NEUTRE, false);

        assertEquals(Optional.empty(), reaction.reagir(Emotion.NEUTRE, 1));
        assertTrue(lancees.isEmpty());
    }

    /** À 0,6 d'intensité et un facteur de 0,5, la chance est de 0,3 : un tirage à 0,29 réagit, à 0,31 non. */
    @Test
    void laChanceDeReagirSuitLIntensite() {
        ranger("Content", Emotion.JOIE, false);

        tirer(0.31);
        assertEquals(Optional.empty(), reaction.reagir(Emotion.JOIE, 0.6));

        tirer(0.29, 0.0);
        assertEquals("Content", reaction.reagir(Emotion.JOIE, 0.6).orElseThrow().animation().nom());
    }

    @Test
    void sansAnimationDeCetteEmotionLeRobotNeFaitRien() {
        ranger("Content", Emotion.JOIE, false);

        tirer(0.0);
        assertEquals(Optional.empty(), reaction.reagir(Emotion.PEUR, 1));
        assertTrue(lancees.isEmpty());
    }

    /** Deux animations de joie : le tirage choisit laquelle. */
    @Test
    void parmiPlusieursAnimationsUneEstTireeAuSort() {
        ranger("Content", Emotion.JOIE, false);
        ranger("Ravi", Emotion.JOIE, false);

        tirer(0.0, 0.99);
        assertEquals("Ravi", reaction.reagir(Emotion.JOIE, 1).orElseThrow().animation().nom());
    }

    @Test
    void deuxReactionsSontSepareesDuDelaiMinimum() {
        ranger("Content", Emotion.JOIE, false);

        tirer(0.0, 0.0);
        assertTrue(reaction.reagir(Emotion.JOIE, 1).isPresent());

        horloge.avancerDe(Duration.ofSeconds(19));
        assertEquals(Optional.empty(), reaction.reagir(Emotion.JOIE, 1));

        horloge.avancerDe(Duration.ofSeconds(1));
        tirer(0.0, 0.0);
        assertTrue(reaction.reagir(Emotion.JOIE, 1).isPresent());
    }

    /** Sans son, rien ne gêne la phrase : l'animation l'accompagne. */
    @Test
    void uneAnimationSansSonAccompagneLaPhrase() {
        ranger("Content", Emotion.JOIE, false);

        tirer(0.0, 0.0);
        assertEquals(Moment.PENDANT, reaction.reagir(Emotion.JOIE, 1).orElseThrow().moment());
    }

    /** Avec son : tantôt avant la phrase, son compris ; tantôt pendant, muette. */
    @Test
    void uneAnimationAvecSonSeJoueAvantAvecSonOuPendantMuette() {
        ranger("Rire", Emotion.AMUSEMENT, true);

        tirer(0.0, 0.0, 0.2);
        Reaction avant = reaction.reagir(Emotion.AMUSEMENT, 1).orElseThrow();
        assertEquals(Moment.AVANT, avant.moment());
        assertEquals(1, avant.animation().sons().size());

        horloge.avancerDe(Duration.ofSeconds(20));
        tirer(0.0, 0.0, 0.8);
        Reaction pendant = reaction.reagir(Emotion.AMUSEMENT, 1).orElseThrow();
        assertEquals(Moment.PENDANT, pendant.moment());
        assertTrue(pendant.animation().sons().isEmpty(), "muette : la voix a besoin de la carte son");
    }

    /** Une animation refusée (arrêt d'urgence…) ne compte pas : la prochaine émotion peut réagir. */
    @Test
    void uneAnimationRefuseeNeDeclenchePasLeDelai() {
        ranger("Content", Emotion.JOIE, false);
        ReactionEmotionnelle refusee = new ReactionEmotionnelle(bibliotheque, animation -> false, tirages::removeFirst,
                horloge, () -> new Reglages(0.5, Duration.ofSeconds(20), 0.5));

        tirer(0.0, 0.0);
        assertEquals(Optional.empty(), refusee.reagir(Emotion.JOIE, 1));
        tirer(0.0, 0.0);
        assertEquals(Optional.empty(), refusee.reagir(Emotion.JOIE, 1));
        assertTrue(tirages.isEmpty(), "le second appel a bien tenté sa chance, sans attendre le délai");
    }
}
