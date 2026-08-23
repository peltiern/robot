package fr.roboteek.robot.securite;

import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Arrêt d'urgence des moteurs : un seul état, plusieurs déclencheurs — bouton B de la manette,
 * bouton du HUD, {@link WatchDog}, ou {@link #declencher(String)} depuis n'importe quel code.
 * <p>
 * <b>Ce que ça fait.</b> Chaque organe à moteur coupe sur-le-champ, le lecteur d'animations vide
 * sa file, et <b>tout ordre de mouvement est refusé</b> jusqu'au réarmement. C'est ce verrouillage
 * qui distingue l'arrêt d'urgence d'un simple « stop » : sans lui, le premier évènement venu
 * relancerait le mouvement dans la seconde.
 * <p>
 * <b>Ce que ça ne fait pas.</b> Les servos ne sont pas désengagés — sans couple, la tête tomberait
 * sous son poids ; ils restent figés là où ils sont. Ce n'est pas une coupure d'alimentation, la
 * seule vraie sécurité matérielle reste l'interrupteur. Et ça ne touche ni la parole ni les
 * capteurs : le robot reste joignable et peut expliquer ce qui se passe.
 * <p>
 * L'état est propagé par un {@link ArretUrgenceEvent} : aucun organe ne dépend de cette classe,
 * ils ne connaissent que l'évènement.
 */
@Component
public class ArretUrgence {

    private static final Logger logger = LoggerFactory.getLogger(ArretUrgence.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    /** État courant. Lu par plusieurs threads (manette, Websocket, REST), d'où l'atomique. */
    private final AtomicBoolean actif = new AtomicBoolean(false);

    public ArretUrgence(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /** Arrêt d'urgence en cours ? */
    public boolean estActif() {
        return actif.get();
    }

    /**
     * Suit l'état porté par le bus, d'où qu'il vienne.
     * <p>
     * Indispensable pour les évènements <b>reçus du Websocket</b> (bouton du HUD) : ils sont
     * publiés directement sur le bus par {@code WebsocketController}, sans passer par
     * {@link #declencher(String)}. Sans ce rattrapage, la bascule de la manette raisonnerait sur
     * un état périmé et un appui sur B « réarmerait » un robot déjà arrêté d'urgence.
     */
    @EventListener
    public void handleArretUrgenceEvent(ArretUrgenceEvent evenement) {
        actif.set(evenement.isActif());
    }

    /**
     * Déclenche l'arrêt d'urgence. Sans effet s'il est déjà actif : un second appui ne doit
     * surtout pas réarmer.
     *
     * @param origine qui déclenche, pour les journaux et l'affichage
     * @return vrai si l'état a changé
     */
    public boolean declencher(String origine) {
        if (!actif.compareAndSet(false, true)) {
            return false;
        }
        // WARN et non INFO : quand on relit les journaux après coup, c'est la ligne qu'on cherche.
        logger.warn("ARRÊT D'URGENCE déclenché ({})", origine);
        applicationEventPublisher.publishEvent(new ArretUrgenceEvent(true, origine));
        return true;
    }

    /**
     * Réarme : les ordres de mouvement sont acceptés à nouveau. Ne bouge rien par lui-même.
     *
     * @return vrai si l'état a changé
     */
    public boolean rearmer(String origine) {
        if (!actif.compareAndSet(true, false)) {
            return false;
        }
        logger.warn("Arrêt d'urgence réarmé ({})", origine);
        applicationEventPublisher.publishEvent(new ArretUrgenceEvent(false, origine));
        return true;
    }

    /**
     * Bascule l'état. Pour les déclencheurs à bouton unique : la manette n'a pas de quoi dédier
     * deux boutons, et rester bloqué en arrêt d'urgence faute de tablette à portée serait pire.
     */
    public void basculer(String origine) {
        if (estActif()) {
            rearmer(origine);
        } else {
            declencher(origine);
        }
    }
}
