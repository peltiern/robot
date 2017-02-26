package fr.roboteek.robot.organes.actionneurs;

import java.io.IOException;

import org.apache.log4j.Logger;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.server.WebSpeechServer;
import fr.roboteek.robot.server.WebSpeechServerListener;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import net.engio.mbassy.listener.Handler;

public class OrganeParoleEspeak extends AbstractOrgane {

    private String fichierSyntheseVocale;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(OrganeParoleEspeak.class);

    /** Constructeur. */
    public OrganeParoleEspeak() {
        super();
        fichierSyntheseVocale = "C:/Program Files (x86)/eSpeak/command_line/espeak.exe";
        //fichierSyntheseVocale = System.getProperty("robot.dir") + File.separator + "synthese-vocale" + File.separator + "synthese.sh";
    }

    /**
     * Lit un texte.
     * @param texte le texte à dire
     */
    public void lire(String texte) {
        if(texte != null && !texte.isEmpty()) {
            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(CONTROLE.METTRE_EN_PAUSE);
            RobotEventBus.getInstance().publishAsync(eventPause);

            System.out.println("Lecture :\t" + texte);
            String[] params = {fichierSyntheseVocale, "-v fr -p 80", texte};
            try {
            	Process p = Runtime.getRuntime().exec("C:/Program Files (x86)/eSpeak/command_line/espeak.exe -v fr -p 80 \"" + texte + "\"");
                p.waitFor();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            logger.debug("Fin lecture :\t" + texte);

            // Envoi d'un évènement pour redémarrer la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
            eventRedemarrage.setControle(CONTROLE.DEMARRER);
            RobotEventBus.getInstance().publish(eventRedemarrage);
        }
    }
    
    /**
     * Intercepte les évènements pour lire du texte.
     * @param paroleEvent évènement pour lire du texte
     */
    @Handler
    public void handleParoleEvent(ParoleEvent paroleEvent) {
        if (paroleEvent.getTexte() != null && !paroleEvent.getTexte().trim().equals("")) {
            lire(paroleEvent.getTexte().trim());
        }
    }

    @Override
    public void initialiser() {
        
    }

    @Override
    public void arreter() {
        
    }
    
    public static void main(String[] args) {
        
        System.setProperty("robot.dir", "/home/npeltier/Robot/Programme");
        
        final OrganeParoleEspeak organeParole = new OrganeParoleEspeak();
        
        final WebSpeechServer webSpeechServer = WebSpeechServer.getInstance();
        webSpeechServer.initialiser();
        
        organeParole.lire("Fin de l'initialisation");
        
     // Création du chat
//        String botname="amy";
//        String path=System.getProperty("robot.dir");
//        Bot bot = new Bot(botname, path);
//        final Chat chat = new Chat(bot);
//        
        WebSpeechServer.getInstance().addListener(new WebSpeechServerListener() {
            
            public void onSpeechResult(String speechResult) {
                System.out.println("Recu : " + speechResult);
//                WebSpeechServer.getInstance().lire("Bonjour tout le monde ! Comment ça va ?");
//                final String reponse = chat.multisentenceRespond(speechResult);
                organeParole.lire(speechResult);
//                WebSpeechServer.getInstance().lire(speechResult);
            }
        });
    }

}
