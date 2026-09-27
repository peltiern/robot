package fr.roboteek.robot.decisionnel.emotion;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.LecteurAnimation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Ce que le robot fait d'une émotion : parfois, une animation qui l'exprime.
 * <p>
 * Parfois seulement. Un robot qui réagit à chaque phrase devient un automate ; la chance de réagir
 * suit donc l'intensité de l'émotion, et deux réactions sont séparées d'un délai minimum. Parmi les
 * animations qui portent l'émotion, une est tirée au sort : le robot ne réagit pas deux fois pareil.
 * <p>
 * La réaction part <b>dès la réponse de l'IA</b>, avant que la phrase ne soit dite : la tête bouge
 * pendant que Piper synthétise, et le geste précède la parole, comme chez nous. Une animation qui a
 * du son se joue tantôt avant la phrase, son compris — la phrase attend alors la fin de la
 * bande-son, voir {@code OrganeParole} —, tantôt pendant, muette : la carte son n'accepte qu'un son.
 */
@Component
public class ReactionEmotionnelle {

    /** Où tombe l'animation par rapport à la phrase. */
    public enum Moment { AVANT, PENDANT }

    /** La réaction choisie : l'animation lancée, telle qu'elle a été lancée (muette ou non). */
    public record Reaction(Animation animation, Moment moment) {
    }

    /** Les réglages de {@code robot.properties}, relus à chaque réaction (voir {@link RobotConfig}). */
    public record Reglages(double facteur, Duration delai, double partAvant) {
        static Reglages depuis(RobotConfig config) {
            return new Reglages(config.emotionsReactionFacteur(), Duration.ofSeconds(config.emotionsReactionDelaiS()),
                    config.emotionsReactionAvantPart());
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ReactionEmotionnelle.class);

    private final BibliothequeDesAnimations bibliotheque;
    private final Predicate<Animation> lancer;
    private final DoubleSupplier hasard;
    private final Clock horloge;
    private final Supplier<Reglages> reglages;

    private Instant derniereReaction;

    /**
     * {@code @Autowired} obligatoire : deux constructeurs, et sans lui Spring n'en choisit aucun,
     * se rabat sur un constructeur vide qui n'existe pas, et tout le contexte échoue au démarrage
     * (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public ReactionEmotionnelle(BibliothequeDesAnimations bibliotheque, LecteurAnimation lecteur) {
        this(bibliotheque, lecteur::jouer, () -> ThreadLocalRandom.current().nextDouble(), Clock.systemUTC(),
                () -> Reglages.depuis(Configurations.robotConfig()));
    }

    /**
     * Pour les tests : le lancement, le hasard (un tirage entre 0 et 1), l'horloge et les réglages
     * qu'on maîtrise.
     */
    ReactionEmotionnelle(BibliothequeDesAnimations bibliotheque, Predicate<Animation> lancer, DoubleSupplier hasard,
                         Clock horloge, Supplier<Reglages> reglages) {
        this.bibliotheque = bibliotheque;
        this.lancer = lancer;
        this.hasard = hasard;
        this.horloge = horloge;
        this.reglages = reglages;
    }

    /**
     * Réagit, peut-être, à ce que la phrase de l'interlocuteur fait ressentir au robot.
     *
     * @return la réaction lancée ; vide si le robot ne réagit pas cette fois
     */
    public synchronized Optional<Reaction> reagir(Emotion emotion, double intensite) {
        if (emotion == null || emotion == Emotion.NEUTRE) {
            return Optional.empty();
        }
        Reglages r = reglages.get();
        Instant maintenant = horloge.instant();
        if (derniereReaction != null && maintenant.isBefore(derniereReaction.plus(r.delai()))) {
            logger.info("Pas de réaction à {} : la dernière date de moins de {} s", emotion.cle(), r.delai().toSeconds());
            return Optional.empty();
        }
        double chance = Math.clamp(intensite * r.facteur(), 0, 1);
        double tirage = hasard.getAsDouble();
        if (tirage >= chance) {
            logger.info("Pas de réaction à {} cette fois (tirage {} pour une chance de {})", emotion.cle(),
                    deux(tirage), deux(chance));
            return Optional.empty();
        }
        List<Animation> candidates = bibliotheque.noms().stream()
                .flatMap(nom -> bibliotheque.charger(nom).stream())
                .filter(animation -> animation.emotion() == emotion)
                .toList();
        if (candidates.isEmpty()) {
            logger.info("Pas de réaction à {} : aucune animation ne porte cette émotion", emotion.cle());
            return Optional.empty();
        }
        Animation choisie = candidates.get(Math.min(candidates.size() - 1, (int) (hasard.getAsDouble() * candidates.size())));
        Reaction reaction = choisie.sons().isEmpty() || hasard.getAsDouble() < r.partAvant()
                ? new Reaction(choisie, choisie.sons().isEmpty() ? Moment.PENDANT : Moment.AVANT)
                : new Reaction(choisie.sansSons(), Moment.PENDANT);
        if (!lancer.test(reaction.animation())) {
            logger.info("Réaction à {} : « {} » n'a pas pu être lancée", emotion.cle(), choisie.nom());
            return Optional.empty();
        }
        derniereReaction = maintenant;
        logger.info("Réaction à {} ({}) : « {} », {}", emotion.cle(), deux(intensite), choisie.nom(),
                reaction.moment() == Moment.AVANT ? "avant la phrase, avec son son"
                        : choisie.sons().isEmpty() ? "pendant la phrase" : "pendant la phrase, muette");
        return Optional.of(reaction);
    }

    private static String deux(double valeur) {
        return String.format(Locale.ROOT, "%.2f", valeur);
    }
}
