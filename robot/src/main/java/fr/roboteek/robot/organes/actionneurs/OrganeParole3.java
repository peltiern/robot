package fr.roboteek.robot.organes.actionneurs;

import org.apache.log4j.Logger;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.server.WebSpeechServer;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import net.engio.mbassy.listener.Handler;

/**
 * Organe de la parole permettant la synthèse vocale.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class OrganeParole3 extends AbstractOrgane {
    
    /** Serveur Speech. */
    private WebSpeechServer speechWebServer;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(OrganeParole3.class);

    /** Constructeur. */
    public OrganeParole3() {
        super();
        speechWebServer = WebSpeechServer.getInstance();
    }

    /**
     * Lit un texte.
     * @param texte le texte à dire
     */
    public void lire(String texte) {
        if(texte != null && !texte.isEmpty()) {
            logger.debug("Lecture :\t" + texte);
            // Lecture du texte
            speechWebServer.lire(texte);
            logger.debug("Fin lecture :\t" + texte);
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

}
