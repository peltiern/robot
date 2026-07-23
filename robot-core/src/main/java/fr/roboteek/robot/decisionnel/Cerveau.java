package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.conversation.openai.ConversationActivity;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.event.ConversationEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cerveau du robot nécessaire à la prise de décisions.
 * <p>
 * Migré en bean Spring : cycle de vie (thread des activités) géré par {@link SmartLifecycle}
 * en phase {@link RobotLifecyclePhases#CERVEAU} — dernier démarré, premier arrêté.
 * Le cerveau est purement évènementiel : il ne référence aucun organe directement.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Component
public class Cerveau extends AbstractOrganeWithThread implements SmartLifecycle {

    /**
     * Contexte du robot.
     */
    // TODO Voir pour placer le contexte au niveau du robot
//    private Contexte contexte;

    /**
     * Activité en cours.
     */
    private AbstractActivity currentActivity;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(Cerveau.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    public Cerveau() {
        super("Brain");
    }

    @Override
    public void initialiser() {
        initNewCurrentActivity(new ConversationActivity());
    }

    @Override
    public void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            // Start activity if exists
            if (currentActivity != null && currentActivity.isInitialized()) {
                logger.debug("Lancement de l'activité");
                boolean hasBeenStopped = currentActivity.run();
                if (!hasBeenStopped) {
                    initNewCurrentActivity(new ConversationActivity());
                }
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // Restitution du flag d'interruption pour que la condition de la boucle le voie
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Intercepte les évènements de reconnaissance vocale.
     *
     * @param reconnaissanceVocaleEvent évènement de reconnaissance vocale
     */
    @EventListener
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (running && !reconnaissanceVocaleEvent.isProcessedByBrain()) {
            final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

            logger.debug("Texte reconnu par le cerveau : {}", texteReconnu);

            // Envoi d'un évènement de conversation au serveur
            final ConversationEvent conversationEvent = new ConversationEvent();
            conversationEvent.setTexte(reconnaissanceVocaleEvent.getTexteReconnu());
            conversationEvent.setIdLocuteur(0);
            RobotEventBus.getInstance().publishAsync(conversationEvent);

            if (texteReconnu != null && !texteReconnu.equals("")) {
                // Arrêt du robot
                if (texteReconnu.trim().equalsIgnoreCase("au revoir")) {
                    final StopEvent stopEvent = new StopEvent();
                    RobotEventBus.getInstance().publish(stopEvent);
                    dire("Au revoir.");
                } else if (texteReconnu.trim().equalsIgnoreCase("akinator")) {
//                    initNewCurrentActivity(new AkinatorActivity());
                    dire("Cette activité n'est pour l'instant pas disponible");
                } else {
                    ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
                    event.setProcessedByBrain(true);
                    event.setTexteReconnu(reconnaissanceVocaleEvent.getTexteReconnu());
                    RobotEventBus.getInstance().publishAsync(event);
                }
            }
        }
    }

    /**
     * Intercepte les évènements de lecture.
     *
     * @param paroleEvent évènement de lecture
     */
    @EventListener
    public void handleParoleEvent(ParoleEvent paroleEvent) {
        if (running && !paroleEvent.isPourTest()) {
            // Envoi d'un évènement de conversation au serveur
            final ConversationEvent conversationEvent = new ConversationEvent();
            conversationEvent.setTexte(paroleEvent.getTexte());
            conversationEvent.setIdLocuteur(-1);
            RobotEventBus.getInstance().publishAsync(conversationEvent);
        }
    }

    /**
     * Commence une nouvelle activité
     *
     * @param activity la nouvelle activité
     */
    private synchronized void initNewCurrentActivity(AbstractActivity activity) {
        // Stop current activity
        stopCurrentActivity();
        currentActivity = activity;
        logger.debug("Nouvelle activité : {}", currentActivity.getClass().getName());
        currentActivity.init();
        // Subscribe to event bus
        RobotEventBus.getInstance().subscribe(currentActivity);
    }

    private synchronized void stopCurrentActivity() {
        if (currentActivity != null) {
            logger.debug("Arrêt de l'activité : {}", currentActivity.getClass().getName());
            // Unsubscribe to event bus
            RobotEventBus.getInstance().unsubscribe(currentActivity);
            // Stop activity
            currentActivity.stop();
            currentActivity = null;
        }
    }

    /**
     * Envoie un évènement pour dire du texte.
     *
     * @param texte le texte à dire
     */
    private void dire(String texte) {
        logger.debug("Dire = {}", texte);
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(texte);
        RobotEventBus.getInstance().publishAsync(paroleEvent);
    }

    @Override
    public void start() {
        initialiser();
        super.start();
        running = true;
        logger.info("Cerveau démarré");
    }

    @Override
    public void stop() {
        running = false;
        // Arrêt de l'activité en cours puis interruption du thread de la boucle
        stopCurrentActivity();
        arreter();
        logger.info("Cerveau arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.CERVEAU;
    }
}
