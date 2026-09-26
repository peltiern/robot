package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;

/**
 * Dire une phrase avec une voix qu'on essaie, sans l'adopter : on écoute sur le robot avant de
 * choisir, comme dans le Studio.
 */
public class EssaiDeVoixEvent extends RobotEvent {

    public static final String EVENT_TYPE = "essaiDeVoix";

    private final String texte;

    private final ReglagesVoix reglages;

    public EssaiDeVoixEvent(String texte, ReglagesVoix reglages) {
        super(EVENT_TYPE);
        this.texte = texte;
        this.reglages = reglages;
    }

    public String getTexte() {
        return texte;
    }

    public ReglagesVoix getReglages() {
        return reglages;
    }
}
