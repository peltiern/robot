package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.conversation.ConversationActivity;
import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.event.ConversationEvent;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteEvent;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteRefuseeEvent;
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
import java.util.Locale;
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
     * Activité à réclamer quand la phrase entendue se résume à l'un de ces mots.
     * <p>
     * Des mots <b>français courants</b>, et plusieurs par activité : le petit modèle Vosk ne
     * restitue que ce que contient son lexique. « Akinator » n'y est pas — dit au robot, il en
     * ressort « inhalateur » ou « akhenaton ».
     * <p>
     * <b>Les mots-clés d'Akinator ont été retirés le 2026-08-15</b>, et l'activité n'est donc plus
     * atteignable. Elle est cassée depuis la mise à jour d'akiwrapper, qui échoue au démarrage sur
     * {@code 'fa' is not a recognized language} : le cerveau se rattrapait et retombait sur la
     * conversation, mais le robot promettait une devinette qu'il ne savait pas jouer. Remettre
     * « devinette » et « devinettes » ici le jour où la bibliothèque sera réparée.
     * <p>
     * La conversation y figure, et c'est la porte de sortie : sans elle, une activité lancée à la
     * voix ne se quitterait qu'en éteignant le robot, « au revoir » l'arrêtant pour de bon.
     */
    private static final Map<String, String> ACTIVITES_PAR_MOT_CLE = Map.of(
            "conversation", ConversationActivity.class.getSimpleName(),
            "discussion", ConversationActivity.class.getSimpleName());

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
     * Décide des demandes de changement d'activité (priorité, temporisation, arrêt d'urgence).
     */
    private final ArbitrageActivites arbitrageActivites;

    /**
     * Ce que le robot a en tête, dont la personne à qui il parle.
     * <p>
     * Le décisionnel touche ici à la mémoire courte, et c'est assumé : c'est le seul endroit qui
     * sache <b>quand</b> le robot entend, et donc le seul instant où la question « à qui ? » ait
     * un sens. L'accueil et les retrouvailles s'y adressent déjà de la même façon.
     */
    private final MemoireCourtTerme memoireCourtTerme;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(Cerveau.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    public Cerveau(ConversationActivity conversationActivity, List<AbstractActivity> activites,
                   ArbitrageActivites arbitrageActivites, MemoireCourtTerme memoireCourtTerme) {
        super("Brain");
        this.conversationActivity = conversationActivity;
        this.activitesParIdentifiant = activites.stream()
                .collect(Collectors.toMap(AbstractActivity::identifiant, Function.identity()));
        this.arbitrageActivites = arbitrageActivites;
        this.memoireCourtTerme = memoireCourtTerme;
    }

    @Override
    public void initialiser() {
        initNewCurrentActivity(conversationActivity);
    }

    @Override
    public void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            // Copie locale : l'arrêt du robot peut remettre currentActivity à null en cours de route.
            AbstractActivity activiteEnCours = currentActivity;
            // Start activity if exists
            if (activiteEnCours != null && activiteEnCours.isInitialized()) {
                logger.debug("Lancement de l'activité");
                boolean hasBeenStopped = activiteEnCours.run();
                if (activiteEnCours != conversationActivity) {
                    // L'activité par défaut est rejointe par repli et jamais réclamée : la
                    // temporiser reviendrait à risquer de la rendre inaccessible.
                    arbitrageActivites.noterFinExecution(activiteEnCours);
                }
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
            // Qui parle : celui que le robot croit avoir devant lui à l'instant où il entend.
            // C'est une approximation, et elle est tenue pour honnête : quelqu'un qui parle hors
            // champ, ou une reconnaissance qui cligne à cet instant, donnera une bulle anonyme.
            // Une bulle sans portrait dit que le robot ne savait pas à qui il parlait — c'est une
            // information, pas un défaut d'affichage.
            final Personne interlocuteur = memoireCourtTerme.interlocuteur();
            if (interlocuteur != null) {
                conversationEvent.setIdPersonne(interlocuteur.id());
                conversationEvent.setPrenom(interlocuteur.prenom());
            }
            applicationEventPublisher.publishEvent(conversationEvent);

            if (texteReconnu != null && !texteReconnu.equals("")) {
                // Arrêt du robot
                if (texteReconnu.trim().equalsIgnoreCase("au revoir")) {
                    final StopEvent stopEvent = new StopEvent();
                    applicationEventPublisher.publishEvent(stopEvent);
                    dire("Au revoir.");
                } else if (ACTIVITES_PAR_MOT_CLE.containsKey(motCle(texteReconnu))) {
                    // Passe par le circuit commun plutôt que de basculer en dur : c'est aussi le
                    // seul déclencheur de demande d'activité à portée de voix, donc de test.
                    applicationEventPublisher.publishEvent(
                            new DemandeActiviteEvent(ACTIVITES_PAR_MOT_CLE.get(motCle(texteReconnu))));
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
     * La demande n'est pas honorée d'office : {@link ArbitrageActivites} tranche d'abord.
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
        ArbitrageActivites.Decision decision = arbitrageActivites.arbitrer(activite, activiteCourante);
        if (!decision.estAcceptee()) {
            // Le refus est annoncé, et pas seulement journalisé : celui qui a fait la demande doit
            // pouvoir en tenir compte. Sans quoi elle disparaît, et rien ne la rejoue une fois la
            // cause du refus levée.
            applicationEventPublisher.publishEvent(
                    new DemandeActiviteRefuseeEvent(activite.identifiant(), decision.name()));
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
            conversationEvent.setDuRobot(true);
            applicationEventPublisher.publishEvent(conversationEvent);
        }
    }

    /**
     * Commence une nouvelle activité.
     * <p>
     * Une activité qui ne parvient pas à s'initialiser rend la main à la conversation. Sans ce
     * repli, la boucle tournerait indéfiniment à vide sur une activité jamais prête (Akinator
     * sans réseau, par exemple) : le robot resterait allumé, muet, et sans rien pour l'en sortir.
     *
     * @param activity la nouvelle activité
     */
    private synchronized void initNewCurrentActivity(AbstractActivity activity) {
        // Stop current activity
        stopCurrentActivity();
        currentActivity = activity;
        logger.info("Nouvelle activité : {}", currentActivity.identifiant());
        // Activation : réinitialise l'activité et autorise le traitement des évènements
        try {
            currentActivity.activer();
        } catch (RuntimeException e) {
            logger.error("Échec de l'initialisation de l'activité {}", activity.identifiant(), e);
        }
        if (!activity.isInitialized() && activity != conversationActivity) {
            logger.error("L'activité {} ne s'est pas initialisée : retour à la conversation", activity.identifiant());
            // Comptée comme terminée : la cause de l'échec dure en général plus longtemps que
            // l'échec lui-même, et la temporisation évite d'y revenir en boucle.
            arbitrageActivites.noterFinExecution(activity);
            initNewCurrentActivity(conversationActivity);
        }
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
     * Réduit une phrase entendue à sa forme comparable aux mots-clés d'activité.
     */
    private static String motCle(String texteReconnu) {
        return texteReconnu.trim().toLowerCase(Locale.FRENCH);
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
