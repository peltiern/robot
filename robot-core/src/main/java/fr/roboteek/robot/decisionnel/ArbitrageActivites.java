package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.securite.ArretUrgence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Décide si une demande de changement d'activité mérite d'être honorée.
 * <p>
 * Les demandes naissent de la perception — un visage qui apparaît — et la perception n'a aucune
 * retenue. D'où trois garde-fous, en un seul endroit :
 * <ul>
 *   <li><b>l'arrêt d'urgence</b> : le robot ne se lance dans rien de neuf tant qu'il est armé. Les
 *       activités d'accueil bougent, leurs ordres seraient refusés en route, et une activité à
 *       moitié jouée est pire que pas d'activité ;</li>
 *   <li><b>la priorité</b> : une activité en cours ne cède pas à moins important qu'elle (voir
 *       {@link AbstractActivity#priorite()}) ;</li>
 *   <li><b>la temporisation</b> : une activité qui tourne court — personne muette, prénom refusé —
 *       voit la même demande revenir aussitôt, et repartirait en boucle sans ce délai.</li>
 * </ul>
 * <p>
 * Classe séparée du {@link Cerveau} pour être vérifiable : tout se décide ici avec une horloge
 * qu'un test maîtrise, quand le cerveau est un thread bloqué dans le {@code run()} de son activité.
 */
@Component
public class ArbitrageActivites {

    private static final Logger logger = LoggerFactory.getLogger(ArbitrageActivites.class);

    /** Sort réservé à une demande de changement d'activité. */
    public enum Decision {

        /** La demande est honorée : le cerveau va basculer. */
        ACCEPTEE,

        /** L'activité demandée est déjà celle qui tourne. */
        DEJA_EN_COURS,

        /** Arrêt d'urgence armé : le robot ne se lance dans rien. */
        ARRET_URGENCE,

        /** L'activité en cours est plus prioritaire que celle demandée. */
        PRIORITE_INSUFFISANTE,

        /** L'activité demandée vient tout juste de se terminer. */
        RELANCE_TROP_TOT;

        public boolean estAcceptee() {
            return this == ACCEPTEE;
        }
    }

    private final ArretUrgence arretUrgence;

    private final Clock horloge;

    /** Fin de la dernière exécution, par identifiant d'activité : porte la temporisation. */
    private final Map<String, Instant> finDerniereExecution = new ConcurrentHashMap<>();

    /**
     * {@code @Autowired} obligatoire ici : cette classe a deux constructeurs, et Spring n'en
     * choisit aucun d'office — il se rabat alors sur un constructeur vide, qui n'existe pas, et
     * le contexte entier échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public ArbitrageActivites(ArretUrgence arretUrgence) {
        this(arretUrgence, Clock.systemDefaultZone());
    }

    /** Permet aux tests de maîtriser le temps, dont dépend la temporisation. */
    ArbitrageActivites(ArretUrgence arretUrgence, Clock horloge) {
        this.arretUrgence = arretUrgence;
        this.horloge = horloge;
    }

    /**
     * Tranche une demande de changement d'activité.
     * <p>
     * Les refus sont journalisés en INFO, et c'est délibéré : quand le robot ne réagit pas à
     * quelqu'un qui s'approche, la ligne qui dit pourquoi est exactement celle qu'on cherche.
     *
     * @param demandee l'activité réclamée
     * @param courante l'activité en cours, {@code null} s'il n'y en a pas
     * @return la décision, à interroger via {@link Decision#estAcceptee()}
     */
    public Decision arbitrer(AbstractActivity demandee, AbstractActivity courante) {
        Decision decision = decider(demandee, courante);
        if (decision.estAcceptee()) {
            logger.debug("Demande d'activité {} acceptée", demandee.identifiant());
        } else {
            logger.info("Demande d'activité {} refusée : {}", demandee.identifiant(), decision);
        }
        return decision;
    }

    private Decision decider(AbstractActivity demandee, AbstractActivity courante) {
        if (demandee == courante) {
            return Decision.DEJA_EN_COURS;
        }
        if (arretUrgence.estActif()) {
            return Decision.ARRET_URGENCE;
        }
        if (courante != null && demandee.priorite() < courante.priorite()) {
            return Decision.PRIORITE_INSUFFISANTE;
        }
        Instant fin = finDerniereExecution.get(demandee.identifiant());
        if (fin != null && secondesEcoulees(fin, horloge.instant()) < robotConfig().delaiAvantRelanceActiviteSecondes()) {
            return Decision.RELANCE_TROP_TOT;
        }
        return Decision.ACCEPTEE;
    }

    /**
     * Enregistre qu'une activité vient de rendre la main : c'est de cet instant que court sa
     * temporisation de relance.
     */
    public void noterFinExecution(AbstractActivity activite) {
        finDerniereExecution.put(activite.identifiant(), horloge.instant());
    }

    private static double secondesEcoulees(Instant debut, Instant fin) {
        return Duration.between(debut, fin).toMillis() / 1000d;
    }
}
