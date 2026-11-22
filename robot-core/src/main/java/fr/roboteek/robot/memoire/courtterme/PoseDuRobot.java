package fr.roboteek.robot.memoire.courtterme;

import fr.roboteek.robot.services.navigation.GeometrieChenilles;
import fr.roboteek.robot.services.navigation.Odometrie;
import fr.roboteek.robot.services.navigation.Pose;
import fr.roboteek.robot.systemenerveux.event.EncodeursChenillesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Où le robot croit être, depuis son démarrage.
 * <p>
 * Rien n'est persisté : au redémarrage le robot repart de l'origine, et c'est voulu tant qu'aucun
 * repère extérieur (les marqueurs ArUco) ne peut lui dire où il est vraiment.
 * <p>
 * Le premier relevé des encodeurs ne déplace rien : il sert de référence. Les compteurs ne partent
 * pas forcément de zéro, et ce qu'ils valent au démarrage n'est pas du chemin parcouru.
 */
@Component
public class PoseDuRobot {

    private static final Logger logger = LoggerFactory.getLogger(PoseDuRobot.class);

    /**
     * Au-delà de ce pas entre deux relevés, le saut n'est pas un déplacement. Les chenilles font au
     * plus 30 cm/s, et les relevés arrivent toutes les 50 ms : un pas d'un demi-mètre ne peut
     * venir que d'un encodeur remis à zéro, quand un câble VINT se débranche puis se rebranche. On
     * reprend alors la référence au lieu de faire reculer le robot de plusieurs mètres.
     */
    private static final double PAS_MAX_PLAUSIBLE_MM = 500;

    private volatile Pose pose = Pose.ORIGINE;

    /** Compteurs du relevé précédent, {@code null} tant qu'aucun relevé n'est arrivé. */
    private Long topsGauchePrecedents;
    private Long topsDroitsPrecedents;

    public Pose pose() {
        return pose;
    }

    /**
     * Relevés traités l'un après l'autre : l'organe les publie depuis une seule tâche planifiée,
     * le {@code synchronized} ne fait que garantir que ça le reste.
     */
    @EventListener
    public synchronized void handleEncodeursChenillesEvent(EncodeursChenillesEvent evenement) {
        if (topsGauchePrecedents != null) {
            GeometrieChenilles geometrie = GeometrieChenilles.depuis(robotConfig());
            double gaucheMm = geometrie.millimetres(evenement.getTopsGauche() - topsGauchePrecedents);
            double droiteMm = geometrie.millimetres(evenement.getTopsDroit() - topsDroitsPrecedents);
            if (Math.abs(gaucheMm) > PAS_MAX_PLAUSIBLE_MM || Math.abs(droiteMm) > PAS_MAX_PLAUSIBLE_MM) {
                logger.warn("Saut des encodeurs ignoré (gauche {} mm, droite {} mm) : compteur remis à zéro ?",
                        Math.round(gaucheMm), Math.round(droiteMm));
            } else {
                pose = Odometrie.avancer(pose, gaucheMm, droiteMm, geometrie.voieMm());
            }
        }
        topsGauchePrecedents = evenement.getTopsGauche();
        topsDroitsPrecedents = evenement.getTopsDroit();
        if (evenement.isArret()) {
            logger.info("Pose à l'arrêt : {}", pose);
        }
    }
}
