package fr.roboteek.robot.decisionnel;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.akinator.AkinatorActivity;
import fr.roboteek.robot.activites.main.MainActivity;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleGoogle;
import fr.roboteek.robot.organes.actionneurs.SoundPlayer;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationPlayer;
import fr.roboteek.robot.organes.capteurs.CapteurVocalAvecReconnaissance;
import fr.roboteek.robot.systemenerveux.event.ConversationEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;

/**
 * Cerveau du robot nécessaire à la prise de décisions.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Cerveau extends AbstractOrganeWithThread {

    /**
     * Contexte du robot.
     */
    // TODO Voir pour placer le contexte au niveau du robot
//    private Contexte contexte;

    /**
     * Activité en cours.
     */
    private AbstractActivity currentActivity;

    public Cerveau() {
        super("Brain");
    }

    @Override
    public void initialiser() {
        initNewCurrentActivity(new MainActivity());
    }

    @Override
    public void loop() {
        while (true) {
            // Start activity if exists
            if (currentActivity != null && currentActivity.isInitialized()) {
                System.out.println("Lancement de l'activité");
                boolean hasBeenStopped = currentActivity.run();
                if (!hasBeenStopped) {
                    initNewCurrentActivity(new MainActivity());
                }
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // TODO bien finaliser la fermeture du thread
        //stopCurrentActivity();
    }

    /**
     * Intercepte les évènements de reconnaissance vocale.
     *
     * @param reconnaissanceVocaleEvent évènement de reconnaissance vocale
     */
    @Subscribe
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (!reconnaissanceVocaleEvent.isProcessedByBrain()) {
            final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

            System.out.println(Thread.currentThread().getName() + "==================>CERVEAU : " + texteReconnu);

            // Envoi d'un évènement de conversation au serveur
            final ConversationEvent conversationEvent = new ConversationEvent();
            conversationEvent.setTexte(reconnaissanceVocaleEvent.getTexteReconnu());
            conversationEvent.setIdLocuteur(0);
            RobotEventBus.getInstance().publishAsync(conversationEvent);

            System.out.println("==================>CERVEAU après : " + texteReconnu);

            if (texteReconnu != null && !texteReconnu.equals("")) {
                // Arrêt du robot
                if (texteReconnu.trim().equalsIgnoreCase("au revoir")) {
                    final StopEvent stopEvent = new StopEvent();
                    RobotEventBus.getInstance().publish(stopEvent);
                    dire("Au revoir.");
                } else if (texteReconnu.trim().equalsIgnoreCase("akinator")) {
                    initNewCurrentActivity(new AkinatorActivity());
                } else {
                    ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
                    event.setProcessedByBrain(true);
                    event.setTexteReconnu(reconnaissanceVocaleEvent.getTexteReconnu());
                    RobotEventBus.getInstance().publishAsync(event);
                }
            }
        }
    }

//    /**
//     * Intercepte les évènements de lecture.
//     *
//     * @param paroleEvent évènement de lecture
//     */
    @Subscribe
    public void handleParoleEvent(ParoleEvent paroleEvent) {
        if (!paroleEvent.isPourTest()) {
            // Envoi d'un évènement de conversation au serveur
            final ConversationEvent conversationEvent = new ConversationEvent();
            conversationEvent.setTexte(paroleEvent.getTexte());
            conversationEvent.setIdLocuteur(-1);
            RobotEventBus.getInstance().publishAsync(conversationEvent);
        }
    }

    /**
     * Arrête le cerveau.
     */
//    public void arreter() {
//        // Arrêt de l'activité en cours
//        arreterActiviteEnCours();
//    }

    /**
     * Commence une nouvelle activité
     *
     * @param activity la nouvelle activité
     */
    private synchronized void initNewCurrentActivity(AbstractActivity activity) {
        // Stop current activity
        stopCurrentActivity();
        currentActivity = activity;
        System.out.println("initNewCurrentActivity : " + currentActivity.getClass().getName());
        currentActivity.init();
        // Subscribe to event bus
        RobotEventBus.getInstance().subscribe(currentActivity);
    }

    private synchronized void stopCurrentActivity() {
        if (currentActivity != null) {
            System.out.println("stopCurrentActivity : " + currentActivity.getClass().getName());
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
        System.out.println("Dire = " + texte);
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(texte);
        RobotEventBus.getInstance().publishAsync(paroleEvent);
    }

    public static void main(String[] args) {
        // Lecteur de sons
        SoundPlayer soundPlayer = new SoundPlayer();
        soundPlayer.initialiser();
        RobotEventBus.getInstance().subscribe(soundPlayer);

        AnimationPlayer animationPlayer = new AnimationPlayer();
        animationPlayer.initialiser();
        animationPlayer.start();
        RobotEventBus.getInstance().subscribe(animationPlayer);

        Cerveau cerveau = new Cerveau();
        cerveau.initialiser();
        RobotEventBus.getInstance().subscribe(cerveau);

        CapteurVocalAvecReconnaissance capteurVocalAvecReconnaissance = new CapteurVocalAvecReconnaissance();
        capteurVocalAvecReconnaissance.initialiser();
        capteurVocalAvecReconnaissance.start();
        OrganeParoleGoogle organeParoleGoogle = new OrganeParoleGoogle();
        organeParoleGoogle.initialiser();

        RobotEventBus.getInstance().subscribe(capteurVocalAvecReconnaissance);
        RobotEventBus.getInstance().subscribe(organeParoleGoogle);

        cerveau.start();
    }
}
