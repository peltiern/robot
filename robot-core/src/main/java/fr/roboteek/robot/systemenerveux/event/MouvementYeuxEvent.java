package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger les yeux.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementYeuxEvent extends RobotEvent {

    public static final String EVENT_TYPE = "mouvement-yeux";

    public static enum MOUVEMENTS_OEIL {TOURNER_BAS, TOURNER_HAUT, ROLL, STOPPER}

    ;

    public static enum MOUVEMENTS_ROULIS {HORAIRE, ANTI_HORAIRE, STOPPER}

    ;

    public static final double POSITION_NEUTRE = 9999;

    /**
     * Mouvement de l'oeil gauche à effectuer.
     */
    private MOUVEMENTS_OEIL mouvementOeilGauche;

    /**
     * Position de l'oeil gauche (0 : en bas, 180 : en haut).
     */
    private double positionOeilGauche = POSITION_NEUTRE;

    /**
     * Vitesse du mouvement de l'oeil gauche.
     */
    private Double vitesseOeilGauche;

    /**
     * Accélération du mouvement de l'oeil gauche.
     */
    private Double accelerationOeilGauche;

    /**
     * Mouvement de l'oeil droit à effectuer.
     */
    private MOUVEMENTS_OEIL mouvementOeilDroit;

    /**
     * Position de l'oeil droit (0 : en bas, 180 : en haut).
     */
    private double positionOeilDroit = POSITION_NEUTRE;

    /**
     * Vitesse du mouvement de l'oeil droit.
     */
    private Double vitesseOeilDroit;

    /**
     * Accélération du mouvement de l'oeil droit.
     */
    private Double accelerationOeilDroit;

    /**
     * Mouvement "Roulis" à effectuer.
     */
    private MOUVEMENTS_ROULIS mouvementRoulis;

    /**
     * Position du roulis (-90 : en bas, 90 : en haut).
     */
    private double positionRoulis = POSITION_NEUTRE;

    /**
     * Vitesse du mouvement du roulis.
     */
    private Double vitesseRoulis;

    /**
     * Accélération du mouvement du roulis.
     */
    private Double accelerationRoulis;

    /**
     * Flag indiquant que le mouvement doit être synchrone.
     */
    private boolean synchrone = false;

    public MouvementYeuxEvent() {
        super(EVENT_TYPE);
    }

    public MouvementYeuxEvent(MOUVEMENTS_OEIL mouvementOeilGauche, double positionOeilGauche,
                              MOUVEMENTS_OEIL mouvementOeilDroit, double positionOeilDroit, MOUVEMENTS_ROULIS mouvementRoulis,
                              double positionRoulis) {
        this();
        this.mouvementOeilGauche = mouvementOeilGauche;
        this.positionOeilGauche = positionOeilGauche;
        this.mouvementOeilDroit = mouvementOeilDroit;
        this.positionOeilDroit = positionOeilDroit;
        this.mouvementRoulis = mouvementRoulis;
        this.positionRoulis = positionRoulis;
    }

    public MouvementYeuxEvent(MOUVEMENTS_OEIL mouvementOeilGauche, double positionOeilGauche, MOUVEMENTS_OEIL mouvementOeilDroit, double positionOeilDroit) {
        this(mouvementOeilGauche, positionOeilGauche, mouvementOeilDroit, positionOeilDroit, null, POSITION_NEUTRE);
    }

    public MouvementYeuxEvent(MOUVEMENTS_ROULIS mouvementRoulis, double positionRoulis) {
        this(null, POSITION_NEUTRE, null, POSITION_NEUTRE, mouvementRoulis, positionRoulis);
    }

    public MOUVEMENTS_OEIL getMouvementOeilGauche() {
        return mouvementOeilGauche;
    }

    public void setMouvementOeilGauche(MOUVEMENTS_OEIL mouvementOeilGauche) {
        this.mouvementOeilGauche = mouvementOeilGauche;
    }

    public double getPositionOeilGauche() {
        return positionOeilGauche;
    }

    public void setPositionOeilGauche(double positionOeilGauche) {
        this.positionOeilGauche = positionOeilGauche;
    }

    public MOUVEMENTS_OEIL getMouvementOeilDroit() {
        return mouvementOeilDroit;
    }

    public void setMouvementOeilDroit(MOUVEMENTS_OEIL mouvementOeilDroit) {
        this.mouvementOeilDroit = mouvementOeilDroit;
    }

    public double getPositionOeilDroit() {
        return positionOeilDroit;
    }

    public void setPositionOeilDroit(double positionOeilDroit) {
        this.positionOeilDroit = positionOeilDroit;
    }

    public MOUVEMENTS_ROULIS getMouvementRoulis() {
        return mouvementRoulis;
    }

    public void setMouvementRoulis(MOUVEMENTS_ROULIS mouvementRoulis) {
        this.mouvementRoulis = mouvementRoulis;
    }

    public double getPositionRoulis() {
        return positionRoulis;
    }

    public void setPositionRoulis(double positionRoulis) {
        this.positionRoulis = positionRoulis;
    }

    public boolean isSynchrone() {
        return synchrone;
    }

    public void setSynchrone(boolean synchrone) {
        this.synchrone = synchrone;
    }

    public Double getVitesseOeilGauche() {
        return vitesseOeilGauche;
    }

    public void setVitesseOeilGauche(Double vitesseOeilGauche) {
        this.vitesseOeilGauche = vitesseOeilGauche;
    }

    public Double getAccelerationOeilGauche() {
        return accelerationOeilGauche;
    }

    public void setAccelerationOeilGauche(Double accelerationOeilGauche) {
        this.accelerationOeilGauche = accelerationOeilGauche;
    }

    public Double getVitesseOeilDroit() {
        return vitesseOeilDroit;
    }

    public void setVitesseOeilDroit(Double vitesseOeilDroit) {
        this.vitesseOeilDroit = vitesseOeilDroit;
    }

    public Double getAccelerationOeilDroit() {
        return accelerationOeilDroit;
    }

    public void setAccelerationOeilDroit(Double accelerationOeilDroit) {
        this.accelerationOeilDroit = accelerationOeilDroit;
    }

    public Double getVitesseRoulis() {
        return vitesseRoulis;
    }

    public void setVitesseRoulis(Double vitesseRoulis) {
        this.vitesseRoulis = vitesseRoulis;
    }

    public Double getAccelerationRoulis() {
        return accelerationRoulis;
    }

    public void setAccelerationRoulis(Double accelerationRoulis) {
        this.accelerationRoulis = accelerationRoulis;
    }

    @Override
    public String toString() {
        return "MouvementYeuxEvent [mouvementOeilGauche=" + mouvementOeilGauche + ", positionOeilGauche="
                + positionOeilGauche + ", vitesseOeilGauche=" + vitesseOeilGauche + ", accelerationOeilGauche="
                + accelerationOeilGauche + ", mouvementOeilDroit=" + mouvementOeilDroit + ", positionOeilDroit="
                + positionOeilDroit + ", vitesseOeilDroit=" + vitesseOeilDroit + ", accelerationOeilDroit="
                + accelerationOeilDroit + ", mouvementRoulis=" + mouvementRoulis + ", positionRoulis=" + positionRoulis
                + ", vitesseRoulis=" + vitesseRoulis + ", accelerationRoulis=" + accelerationRoulis + ", synchrone="
                + synchrone + "]";
    }
}
