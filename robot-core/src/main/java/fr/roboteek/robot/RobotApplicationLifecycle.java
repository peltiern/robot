package fr.roboteek.robot;

import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cycle de vie applicatif du robot (remplace l'ancienne classe {@code Robot}) :
 * <ul>
 *     <li>annonce vocale quand tout est démarré ({@link ApplicationReadyEvent}) ;</li>
 *     <li>arrêt de l'application sur {@link StopEvent} (« au revoir ») : la fermeture du
 *     contexte Spring arrête tous les organes dans l'ordre inverse des phases
 *     (cerveau, moteurs, actionneurs, capteurs).</li>
 * </ul>
 */
@Component
public class RobotApplicationLifecycle {

    private final ConfigurableApplicationContext applicationContext;

    private final Logger logger = LoggerFactory.getLogger(RobotApplicationLifecycle.class);

    public RobotApplicationLifecycle(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Annonce la fin de l'initialisation une fois l'application complètement démarrée.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void annoncerDemarrage() {
        logger.info("Robot complètement démarré");
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte("J'ai terminé de m'initialiser");
        RobotEventBus.getInstance().publishAsync(paroleEvent);
    }

    /**
     * Intercepte les évènements pour stopper le robot.
     *
     * @param stopEvent évènement pour stopper le robot
     */
    @EventListener
    public void handleStopEvent(StopEvent stopEvent) {
        logger.info("Arrêt du robot demandé");
        // Arrêt dans un thread dédié pour laisser la publication de l'évènement se terminer
        final Thread threadArret = new Thread(() -> {
            int codeSortie = SpringApplication.exit(applicationContext, () -> 0);
            logger.info("Fin de l'arrêt du robot");
            System.exit(codeSortie);
        }, "arret-robot");
        threadArret.start();
    }
}
