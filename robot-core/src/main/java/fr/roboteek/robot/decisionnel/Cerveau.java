package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.conversation.ConversationActivity;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.event.ConversationEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
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
     * Activité de conversation (activité par défaut).
     */
    private final ConversationActivity conversationActivity;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(Cerveau.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    public Cerveau(ConversationActivity conversationActivity) {
        super("Brain");
        this.conversationActivity = conversationActivity;
    }

    @Override
    public void initialiser() {
        initNewCurrentActivity(conversationActivity);
    }

    @Override
    public void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            // Start activity if exists
            if (currentActivity != null && currentActivity.isInitialized()) {
                logger.debug("Lancement de l'activité");
                boolean hasBeenStopped = currentActivity.run();
                if (!hasBeenStopped) {
                    initNewCurrentActivity(conversationActivity);
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

            logger.info("Texte reconnu par le cerveau : {}", texteReconnu);

            // Envoi d'un évènement de conversation au serveur
            final ConversationEvent conversationEvent = new ConversationEvent();
            conversationEvent.setTexte(reconnaissanceVocaleEvent.getTexteReconnu());
            conversationEvent.setIdLocuteur(0);
            applicationEventPublisher.publishEvent(conversationEvent);

            if (texteReconnu != null && !texteReconnu.equals("")) {
                // Arrêt du robot
                if (texteReconnu.trim().equalsIgnoreCase("au revoir")) {
                    final StopEvent stopEvent = new StopEvent();
                    applicationEventPublisher.publishEvent(stopEvent);
                    dire("Au revoir.");
                } else if (texteReconnu.trim().equalsIgnoreCase("akinator")) {
//                    initNewCurrentActivity(new AkinatorActivity());
                    dire("Cette activité n'est pour l'instant pas disponible");
                } else {
                    ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
                    event.setProcessedByBrain(true);
                    event.setTexteReconnu(reconnaissanceVocaleEvent.getTexteReconnu());
                    applicationEventPublisher.publishEvent(event);
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
            applicationEventPublisher.publishEvent(conversationEvent);
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
        logger.info("Nouvelle activité : {}", currentActivity.getClass().getName());
        // Activation : réinitialise l'activité et autorise le traitement des évènements
        currentActivity.activer();
    }

    private synchronized void stopCurrentActivity() {
        if (currentActivity != null) {
            logger.debug("Arrêt de l'activité : {}", currentActivity.getClass().getName());
            // Désactivation : les évènements ne sont plus traités par cette activité
            currentActivity.desactiver();
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
        applicationEventPublisher.publishEvent(paroleEvent);
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
