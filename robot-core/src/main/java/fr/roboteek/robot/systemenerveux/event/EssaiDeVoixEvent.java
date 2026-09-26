package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.organes.actionneurs.voix.Voix;

/**
 * Dire une phrase avec une voix qu'on essaie — modèle et réglages —, sans l'adopter : on écoute sur
 * le robot avant de choisir, comme dans le Studio.
 */
public class EssaiDeVoixEvent extends RobotEvent {

    public static final String EVENT_TYPE = "essaiDeVoix";

    private final String texte;

    private final Voix voix;

    public EssaiDeVoixEvent(String texte, Voix voix) {
        super(EVENT_TYPE);
        this.texte = texte;
        this.voix = voix;
    }

    public String getTexte() {
        return texte;
    }

    public Voix getVoix() {
        return voix;
    }
}
