package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour faire tourner les roue.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementRoueEvent extends RobotEvent {

    public static final String EVENT_TYPE = "mouvement-roue";

    public static enum MOUVEMENTS_ROUE {AVANCER, RECULER, PIVOTER_GAUCHE, PIVOTER_DROIT, DIFFERENTIEL, STOPPER}

    ;

    public static final double VITESSE_NEUTRE = 99999;

    public static final double ACCELERATION_NEUTRE = 99999;

    /**
     * Mouvement des roues à effectuer.
     */
    private MOUVEMENTS_ROUE mouvementRoue;

    /**
     * Vitesse globale des roues (pour avancer, reculer, pivoter).
     */
    private Double vitesseGlobale;

    /**
     * Accélération globale (pour avancer, reculer, pivoter).
     */
    private Double accelerationGlobale;

    /**
     * Vitesse de la roue gauche (pour conduite différentielle).
     */
    private Double vitesseRoueGauche;

    /**
     * Accélération de la roue gauche (pour conduite différentielle).
     */
    private Double accelerationRoueGauche;

    /**
     * Vitesse de la roue droite (pour conduite différentielle).
     */
    private Double vitesseRoueDroite;

    /**
     * Accélération de la roue droite (pour conduite différentielle).
     */
    private Double accelerationRoueDroite;

    public MouvementRoueEvent() {
        super(EVENT_TYPE);
    }

    public MouvementRoueEvent(MOUVEMENTS_ROUE mouvementRoue, Double vitesseGlobale, Double accelerationGlobale) {
        this();
        this.mouvementRoue = mouvementRoue;
        this.vitesseGlobale = vitesseGlobale;
        this.accelerationGlobale = accelerationGlobale;
        vitesseRoueGauche = vitesseGlobale;
        accelerationRoueGauche = accelerationGlobale;
        vitesseRoueDroite = vitesseGlobale;
        accelerationRoueDroite = accelerationGlobale;
    }

    /**
     * Constructeur à utiliser pour un mouvement différentiel.
     */
    public MouvementRoueEvent(MOUVEMENTS_ROUE mouvementRoue, Double vitesseRoueGauche, Double accelerationRoueGauche, Double vitesseRoueDroite, Double accelerationRoueDroite) {
        this();
        this.mouvementRoue = mouvementRoue;
        this.vitesseRoueGauche = vitesseRoueGauche;
        this.accelerationRoueGauche = accelerationRoueGauche;
        this.vitesseRoueDroite = vitesseRoueDroite;
        this.accelerationRoueDroite = accelerationRoueDroite;
    }

    public MOUVEMENTS_ROUE getMouvementRoue() {
        return mouvementRoue;
    }

    public void setMouvementRoue(MOUVEMENTS_ROUE mouvementRoue) {
        this.mouvementRoue = mouvementRoue;
    }

    public Double getVitesseGlobale() {
        return vitesseGlobale;
    }

    public void setVitesseGlobale(Double vitesseGlobale) {
        this.vitesseGlobale = vitesseGlobale;
    }

    public Double getAccelerationGlobale() {
        return accelerationGlobale;
    }

    public void setAccelerationGlobale(Double accelerationGlobale) {
        this.accelerationGlobale = accelerationGlobale;
    }

    public Double getVitesseRoueGauche() {
        return vitesseRoueGauche;
    }

    public void setVitesseRoueGauche(Double vitesseRoueGauche) {
        this.vitesseRoueGauche = vitesseRoueGauche;
    }

    public Double getAccelerationRoueGauche() {
        return accelerationRoueGauche;
    }

    public void setAccelerationRoueGauche(Double accelerationRoueGauche) {
        this.accelerationRoueGauche = accelerationRoueGauche;
    }

    public Double getVitesseRoueDroite() {
        return vitesseRoueDroite;
    }

    public void setVitesseRoueDroite(Double vitesseRoueDroite) {
        this.vitesseRoueDroite = vitesseRoueDroite;
    }

    public Double getAccelerationRoueDroite() {
        return accelerationRoueDroite;
    }

    public void setAccelerationRoueDroite(Double accelerationRoueDroite) {
        this.accelerationRoueDroite = accelerationRoueDroite;
    }


    @Override
    public String toString() {
        return "MouvementRoueEvent{" +
                "mouvementRoue=" + mouvementRoue +
                ", vitesseGlobale=" + vitesseGlobale +
                ", accelerationGlobale=" + accelerationGlobale +
                ", vitesseRoueGauche=" + vitesseRoueGauche +
                ", accelerationRoueGauche=" + accelerationRoueGauche +
                ", vitesseRoueDroite=" + vitesseRoueDroite +
                ", accelerationRoueDroite=" + accelerationRoueDroite +
                '}';
    }
}
