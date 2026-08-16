package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.conversation.ConversationActivity;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.event.ConversationEvent;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * <p>
     * Volatile : écrite par le thread de la boucle et par le thread qui dépose une demande
     * de changement d'activité, lue par les deux.
     */
    private volatile AbstractActivity currentActivity;

    /**
     * Activité de conversation (activité par défaut).
     */
    private final ConversationActivity conversationActivity;

    /**
     * Toutes les activités connues, indexées par leur identifiant : ce que peut désigner un
     * {@link DemandeActiviteEvent}.
     */
    private final Map<String, AbstractActivity> activitesParIdentifiant;

    /**
     * Activité réclamée, en attente que la boucle reprenne la main. {@code null} si aucune.
     */
    private final AtomicReference<AbstractActivity> activiteDemandee = new AtomicReference<>();

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(Cerveau.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    public Cerveau(ConversationActivity conversationActivity, List<AbstractActivity> activites) {
        super("Brain");
        this.conversationActivity = conversationActivity;
        this.activitesParIdentifiant = activites.stream()
                .collect(Collectors.toMap(AbstractActivity::identifiant, Function.identity()));
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
                // La boucle est seule à changer d'activité : une demande déposée pendant que
                // l'activité tournait n'est honorée qu'ici, une fois son run() rendu.
                AbstractActivity activiteSuivante = activiteDemandee.getAndSet(null);
                if (!running) {
                    // Arrêt du robot en cours : ne rien relancer.
                    break;
                }
                if (activiteSuivante != null) {
                    initNewCurrentActivity(activiteSuivante);
                } else if (!hasBeenStopped) {
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
     * Intercepte les demandes de changement d'activité.
     * <p>
     * Le changement n'a pas lieu ici : la boucle du cerveau est bloquée dans le {@code run()}
     * de l'activité courante. On dépose la demande, puis on désactive l'activité en cours pour
     * qu'elle rende la main — c'est ensuite le thread de la boucle, seul propriétaire du
     * changement, qui bascule. Le listener reste donc court : il s'exécute sur le thread de
     * l'émetteur (la boucle de capture vidéo, par exemple), qu'il ne doit pas retenir.
     * <p>
     * Aucun arbitrage pour l'instant — ni priorité, ni temporisation par personne, ni refus
     * pendant un arrêt d'urgence : c'est l'étape suivante du chantier d'accueil.
     *
     * @param demandeActiviteEvent demande de changement d'activité
     */
    @EventListener
    public void handleDemandeActiviteEvent(DemandeActiviteEvent demandeActiviteEvent) {
        if (!running) {
            return;
        }
        AbstractActivity activite = activitesParIdentifiant.get(demandeActiviteEvent.getIdActivite());
        if (activite == null) {
            logger.warn("Activité demandée inconnue : {} (connues : {})",
                    demandeActiviteEvent.getIdActivite(), activitesParIdentifiant.keySet());
            return;
        }
        AbstractActivity activiteCourante = currentActivity;
        if (activite == activiteCourante) {
            logger.debug("Activité {} déjà en cours, demande ignorée", activite.identifiant());
            return;
        }
        logger.info("Activité demandée : {}", activite.identifiant());
        activiteDemandee.set(activite);
        if (activiteCourante != null) {
            activiteCourante.desactiver();
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
        logger.info("Nouvelle activité : {}", currentActivity.identifiant());
        // Activation : réinitialise l'activité et autorise le traitement des évènements
        currentActivity.activer();
    }

    private synchronized void stopCurrentActivity() {
        if (currentActivity != null) {
            logger.debug("Arrêt de l'activité : {}", currentActivity.identifiant());
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
