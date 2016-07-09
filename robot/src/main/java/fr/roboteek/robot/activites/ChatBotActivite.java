package fr.roboteek.robot.activites;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.apache.log4j.Logger;

import fr.roboteek.robot.decisionnel.Contexte;
import fr.roboteek.robot.memoire.ReconnaissanceFaciale;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.VisagesEvent;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;

/**
 * Activité "Conversation".
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ChatBotActivite extends AbstractActivite {
    
    /** Chat. */
    private Chat chat;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(ChatBotActivite.class);

    /**
     * Constructeur.
     * @param systemeNerveux système nerveux du robot
     */
    public ChatBotActivite(MBassador<RobotEvent> systemeNerveux, Contexte contexte, ReconnaissanceFaciale reconnaissanceFaciale) {
        super(systemeNerveux, contexte, reconnaissanceFaciale);
    }

    @Override
    public void initialiser() {
        System.out.println("Init Activité conversation");
        
     // Création du chat
      String botname="amy";
      String path = System.getProperty("robot.dir");
      final Bot bot = new Bot(botname, path);
      chat = new Chat(bot);
        
        dire("Début de la conversation");
    }

    @Override
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();
        if (texteReconnu != null && !texteReconnu.equals("")) {
            System.out.println("HUMAIN : " + texteReconnu);

            final String reponse = chat.multisentenceRespond(texteReconnu);
            
            if (reponse != null && !reponse.equals("")) {
                dire(reponse);
            }
        }
    }
    
    @Override
    @Handler
    public void handleVisagesEvent(VisagesEvent visagesEvent) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void arreter() {
        dire("Fin de la conversation. Au revoir.");
    }

}
