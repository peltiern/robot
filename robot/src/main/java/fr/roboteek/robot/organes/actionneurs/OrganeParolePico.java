package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.server.WebSpeechServer;
import fr.roboteek.robot.server.WebSpeechServerListener;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import net.engio.mbassy.listener.Handler;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class OrganeParolePico extends AbstractOrgane {

    private String fichierSyntheseVocale;

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(OrganeParolePico.class);

    /**
     * Constructeur.
     */
    public OrganeParolePico() {
        super();
        fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "pico" + File.separator + "synthese.sh";
    }

    /**
     * Lit un texte.
     *
     * @param texte le texte à dire
     */
    public void lire(String texte) {
        if (texte != null && !texte.isEmpty()) {
            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(CONTROLE.METTRE_EN_PAUSE);
            RobotEventBus.getInstance().publishAsync(eventPause);

            System.out.println("Lecture :\t" + texte);
            String[] params = {fichierSyntheseVocale, texte};
            try {
                Process p = Runtime.getRuntime().exec(params);
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
     *
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

        final OrganeParolePico organeParole = new OrganeParolePico();

        organeParole.lire("Le nouveau robot est génial.");
    }

}
