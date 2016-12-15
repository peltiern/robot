package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.AbstractActivite;
import fr.roboteek.robot.activites.ChatBotActivite;
import fr.roboteek.robot.activites.ConversationActivite;
import fr.roboteek.robot.activites.JeuActivite;
import fr.roboteek.robot.activites.TrackingActivite;
import fr.roboteek.robot.memoire.ReconnaissanceFaciale;
import fr.roboteek.robot.memoire.ReconnaissanceFacialeAnnotator;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import fr.roboteek.robot.systemenerveux.event.VisagesEvent;
import net.engio.mbassy.listener.Handler;

/**
 * Cerveau du robot nécessaire à la prise de décisions.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Cerveau {
    
    /** Contexte du robot. */
    // TODO Voir pour placer le contexte au niveau du robot
    private Contexte contexte;
    
    /** Intelligence artificielle (contenant le système conversationnel). */
    private IntelligenceArtificielle intelligenceArtificielle;
    
    /** Reconnaissance faciale. */
    private ReconnaissanceFaciale reconnaissanceFaciale;
    
    /** Activité en cours. */
    private AbstractActivite activiteEnCours;

    public Cerveau() {
        contexte = new Contexte();
        intelligenceArtificielle = new IntelligenceArtificielle();
//        reconnaissanceFaciale = new ReconnaissanceFacialeEigenface();
        reconnaissanceFaciale = new ReconnaissanceFacialeAnnotator();
    }

    /**
     * Intercepte les évènements de détection de visages.
     * @param visagesEvent évènement de détection de visages
     */
    @Handler
    public void handleVisagesEvent(VisagesEvent visagesEvent) {
        
    }
    
    /**
     * Intercepte les évènements de reconnaissance vocale.
     * @param reconnaissanceVocaleEvent évènement de reconnaissance vocale
     */
    @Handler
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();
        
        System.out.println("==================>CERVEAU : " + texteReconnu);
        
        if (texteReconnu != null && !texteReconnu.equals("")) {
            
            // Conversation
            if (texteReconnu.trim().equalsIgnoreCase("Sami conversation")) {
                System.out.println("Activité conversation");
                final ConversationActivite conversationActivite = new ConversationActivite(contexte, reconnaissanceFaciale);
                commencerActivite(conversationActivite);
            }
            
            // Conversation avec moteur de chat
            else if (texteReconnu.trim().equalsIgnoreCase("Conversation")) {
                final ChatBotActivite chatBotActivite = new ChatBotActivite(contexte, reconnaissanceFaciale);
                commencerActivite(chatBotActivite);
            }
            
            // Tracking
            else if (texteReconnu.trim().equalsIgnoreCase("Sami suivi visage")) {
                final TrackingActivite trackingActivite = new TrackingActivite(contexte, reconnaissanceFaciale);
                commencerActivite(trackingActivite);
            }
            
            // Jeu
            else if (texteReconnu.trim().equalsIgnoreCase("jouer")) {
                final JeuActivite jeuActivite = new JeuActivite(contexte, reconnaissanceFaciale);
                commencerActivite(jeuActivite);
            }
            
            // Arrêt du robot
            else if (texteReconnu.trim().equalsIgnoreCase("au revoir")) {
                dire("Au revoir.");
                final StopEvent stopEvent = new StopEvent();
                RobotEventBus.getInstance().publish(stopEvent);
            }
        }
    }
    
    /**
     * Arrête le cerveau.
     */
    public void arreter() {
        // Arrêt de l'activité en cours
        arreterActiviteEnCours();
    }
    
    /**
     * Commence une nouvelle activité
     * @param activite la nouvelle activité
     */
    private void commencerActivite(AbstractActivite activite) {
        // Arrêt de l'activité en cours
        arreterActiviteEnCours();
        activiteEnCours = activite;
        // Inscription de l'activité au système nerveux
        RobotEventBus.getInstance().subscribe(activiteEnCours);
        activiteEnCours.initialiser();
    }
    
    private void arreterActiviteEnCours() {
        if (activiteEnCours != null) {
            // Désinscription de l'activité du système nerveux
        	RobotEventBus.getInstance().unsubscribe(activiteEnCours);
            // Arrêt de l'activité
            activiteEnCours.arreter();
            activiteEnCours = null;
        }
    }
    
    /**
     * Envoie un évènement pour dire du texte.
     * @param texte le texte à dire
     */
    private void dire(String texte) {
        System.out.println("Dire = " + texte);
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(texte);
        RobotEventBus.getInstance().publish(paroleEvent);
    }
    

}
