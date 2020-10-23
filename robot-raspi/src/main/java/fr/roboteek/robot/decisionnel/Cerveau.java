package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.server.RobotEventWebSocket;
import fr.roboteek.robot.systemenerveux.event.ConversationEvent;
import fr.roboteek.robot.systemenerveux.event.DetectionVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.StringUtils;

/**
 * Cerveau du robot nécessaire à la prise de décisions.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Cerveau {

    /**
     * Contexte du robot.
     */
    // TODO Voir pour placer le contexte au niveau du robot
//    private Contexte contexte;

    /** Intelligence artificielle (contenant le système conversationnel). */
//    private IntelligenceArtificielle intelligenceArtificielle;

    /**
     * Intelligence artificielle (contenant le système conversationnel).
     */
//    private IntelligenceArtificielleRivescript intelligenceArtificielle;

    /**
     * Intelligence artificielle (contenant le système conversationnel).
     */
    private IntelligenceArtificielleDialogFlow intelligenceArtificielleDialogFlow;
    private IntelligenceArtificielleGoogleKnowledge intelligenceArtificielleGoogleKnowledge;

    /**
     * Reconnaissance faciale.
     */
    //private ReconnaissanceFaciale reconnaissanceFaciale;

    /**
     * Activité en cours.
     */
//    private AbstractActivite activiteEnCours;

    public Cerveau() {
        //contexte = new Contexte();
        intelligenceArtificielleDialogFlow = new IntelligenceArtificielleDialogFlow();
        intelligenceArtificielleGoogleKnowledge = new IntelligenceArtificielleGoogleKnowledge();
//        reconnaissanceFaciale = new ReconnaissanceFacialeEigenface();
        //reconnaissanceFaciale = new ReconnaissanceFacialeAnnotator();
    }

//    /**
//     * Intercepte les évènements de détection de visages.
//     *
//     * @param visagesEvent évènement de détection de visages
//     */
//    @Handler
//    public void handleVisagesEvent(VisagesEvent visagesEvent) {
//
//    }

    /**
     * Intercepte les évènements de reconnaissance vocale.
     *
     * @param reconnaissanceVocaleEvent évènement de reconnaissance vocale
     */
    @Handler
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

        System.out.println("==================>CERVEAU : " + texteReconnu);

        // Envoi d'un évènement de conversation au serveur
        final ConversationEvent conversationEvent = new ConversationEvent();
        conversationEvent.setTexte(reconnaissanceVocaleEvent.getTexteReconnu());
        conversationEvent.setIdLocuteur(0);
        RobotEventWebSocket.broadcastEvent(conversationEvent);

        System.out.println("==================>CERVEAU après : " + texteReconnu);

        if (texteReconnu != null && !texteReconnu.equals("")) {
            // Arrêt du robot
            if (texteReconnu.trim().equalsIgnoreCase("au revoir.")) {
                final StopEvent stopEvent = new StopEvent();
                RobotEventBus.getInstance().publish(stopEvent);
                dire("Au revoir.");
            }
            // Tracking
//            else if (texteReconnu.trim().equalsIgnoreCase("Tracking.")) {
//                final TrackingActivite trackingActivite = new TrackingActivite(contexte, reconnaissanceFaciale);
//                commencerActivite(trackingActivite);
//            }
            else {
                // Conversation
                RequeteIntelligenceArtificielle requete = new RequeteIntelligenceArtificielle();
                requete.setInputText(texteReconnu);
                ReponseIntelligenceArtificielle reponse = intelligenceArtificielleDialogFlow.repondreARequete(requete);
                ReponseIntelligenceArtificielle firstResponse = reponse;
                if (reponse.isFallback()) {
                    reponse = intelligenceArtificielleGoogleKnowledge.repondreARequete(requete);
                }
                if (reponse.isFallback()) {
                    reponse = firstResponse;
                }
                dire(reponse);
            }

//            dire(texteReconnu);
//            // Conversation
//            if (texteReconnu.trim().equalsIgnoreCase("Sami conversation")) {
//                System.out.println("Activité conversation");
//                final ConversationActivite conversationActivite = new ConversationActivite(contexte, reconnaissanceFaciale);
//                commencerActivite(conversationActivite);
//            }
//
//            // Conversation avec moteur de chat
//            else if (texteReconnu.trim().equalsIgnoreCase("Conversation")) {
//                final ChatBotActivite chatBotActivite = new ChatBotActivite(contexte, reconnaissanceFaciale);
//                commencerActivite(chatBotActivite);
//            }
//

//
//            // Jeu
//            else if (texteReconnu.trim().equalsIgnoreCase("jouer")) {
//                final JeuActivite jeuActivite = new JeuActivite(contexte, reconnaissanceFaciale);
//                commencerActivite(jeuActivite);
//            }
//

        }
    }

    /**
     * Intercepte les évènements de détection vocale.
     *
     * @param detectionVocaleEvent évènement de détection vocale
     */
    @Handler
    public void handleDetectionVocalEvent(DetectionVocaleEvent detectionVocaleEvent) {

        if (detectionVocaleEvent != null && StringUtils.isNotEmpty(detectionVocaleEvent.getCheminFichier())) {
                // Conversation
            RequeteIntelligenceArtificielle requete = new RequeteIntelligenceArtificielle();
            requete.setInputWavFilePath(detectionVocaleEvent.getCheminFichier());
            ReponseIntelligenceArtificielle reponse = intelligenceArtificielleDialogFlow.repondreARequete(requete);
            ReponseIntelligenceArtificielle firstResponse = reponse;
            if (reponse.isFallback()) {
                // Récupération de la requête sous forme textuelle (car la requête initiale était audio)
                if (StringUtils.isNotBlank(reponse.getInputText())) {
                    requete.setInputText(reponse.getInputText());
                }
                reponse = intelligenceArtificielleGoogleKnowledge.repondreARequete(requete);
            }
            if (reponse.isFallback()) {
                reponse = firstResponse;
            }
            dire(reponse);

            // Envoi d'un évènement de conversation au serveur
            final ConversationEvent conversationEvent = new ConversationEvent();
            conversationEvent.setTexte(reponse.getOutputText());
            conversationEvent.setIdLocuteur(0);
            RobotEventWebSocket.broadcastEvent(conversationEvent);
        }
    }

    /**
     * Intercepte les évènements de lecture.
     *
     * @param paroleEvent évènement de lecture
     */
    @Handler
    public void handleParoleEvent(ParoleEvent paroleEvent) {
        if (!paroleEvent.isPourTest()) {
            // Envoi d'un évènement de conversation au serveur
            final ConversationEvent conversationEvent = new ConversationEvent();
            conversationEvent.setTexte(paroleEvent.getTexte());
            conversationEvent.setIdLocuteur(-1);
            RobotEventWebSocket.broadcastEvent(conversationEvent);
        }
    }

    /**
     * Arrête le cerveau.
     */
//    public void arreter() {
//        // Arrêt de l'activité en cours
//        arreterActiviteEnCours();
//    }

//    /**
//     * Commence une nouvelle activité
//     *
//     * @param activite la nouvelle activité
//     */
//    private void commencerActivite(AbstractActivite activite) {
//        // Arrêt de l'activité en cours
//        arreterActiviteEnCours();
//        activiteEnCours = activite;
//        // Inscription de l'activité au système nerveux
//        RobotEventBus.getInstance().subscribe(activiteEnCours);
//        activiteEnCours.initialiser();
//    }
//
//    private void arreterActiviteEnCours() {
//        if (activiteEnCours != null) {
//            // Désinscription de l'activité du système nerveux
//            RobotEventBus.getInstance().unsubscribe(activiteEnCours);
//            // Arrêt de l'activité
//            activiteEnCours.arreter();
//            activiteEnCours = null;
//        }
//    }

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

    /**
     * Envoie un évènement pour dire du texte.
     *
     * @param reponse la réponse à dire
     */
    private void dire(ReponseIntelligenceArtificielle reponse) {
        System.out.println("Réponse = " + reponse);
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(reponse.getOutputText());
        paroleEvent.setAudioContent(reponse.getOutputAudio());
        RobotEventBus.getInstance().publishAsync(paroleEvent);
    }


}
