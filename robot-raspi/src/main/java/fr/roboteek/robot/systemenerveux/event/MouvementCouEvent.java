package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger la tête.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementCouEvent extends RobotEvent {

    public static final String EVENT_TYPE = "mouvement-cou";

    public static enum MOUVEMENTS_GAUCHE_DROITE {TOURNER_GAUCHE, TOURNER_DROITE, STOPPER}

    ;

    public static enum MOUVEMENTS_HAUT_BAS {TOURNER_HAUT, TOURNER_BAS, STOPPER}

    ;

    public static enum MOUVEMENTS_ROULIS {HORAIRE, ANTI_HORAIRE, STOPPER}

    ;

    public static final double POSITION_NEUTRE = 99999;

    public static final double ANGLE_NEUTRE = 99999;

    public static final double VITESSE_NEUTRE = 99999;

    public static final double ACCELERATION_NEUTRE = 99999;

    /**
     * Mouvement "Gauche - Droite" à effectuer.
     */
    private MOUVEMENTS_GAUCHE_DROITE mouvementGaucheDroite;

    /**
     * Position "Gauche - Droite" (0 : à gauche, 180 : à droite).
     */
    private double positionGaucheDroite = POSITION_NEUTRE;

    /**
     * Angle "Gauche - Droite".
     */
    private double angleGaucheDroite = ANGLE_NEUTRE;

    /**
     * Vitesse du mouvement "Gauche - Droite".
     */
    private Double vitesseGaucheDroite;

    /**
     * Accélération du mouvement "Gauche - Droite".
     */
    private Double accelerationGaucheDroite;

    /**
     * Mouvement "Haut - Bas" à effectuer.
     */
    private MOUVEMENTS_HAUT_BAS mouvementHauBas;

    /**
     * Position "Haut - Bas" (0 : en bas, 180 : en haut).
     */
    private double positionHautBas = POSITION_NEUTRE;

    /**
     * Angle "Haut - Bas".
     */
    private double angleHautBas = ANGLE_NEUTRE;

    /**
     * Vitesse du mouvement "Haut - Bas".
     */
    private Double vitesseHautBas;

    /**
     * Accélération du mouvement "Haut - Bas".
     */
    private Double accelerationHautBas;

    /**
     * Mouvement "Roulis" à effectuer.
     */
    private MOUVEMENTS_ROULIS mouvementRoulis;

    /**
     * Position du roulis (-90 : en bas, 90 : en haut).
     */
    private double positionRoulis = POSITION_NEUTRE;

    /**
     * Vitesse du mouvement "Roulis".
     */
    private Double vitesseRoulis;

    /**
     * Accélération du mouvement "Roulis".
     */
    private Double accelerationRoulis;

    /**
     * Flag indiquant que le mouvement doit être synchrone.
     */
    private boolean synchrone = false;

    public MouvementCouEvent() {
        super(EVENT_TYPE);
    }

    public MouvementCouEvent(MOUVEMENTS_GAUCHE_DROITE mouvementGaucheDroite,
                             double positionGaucheDroite, MOUVEMENTS_HAUT_BAS mouvementHauBas, double positionHautBas,
                             MOUVEMENTS_ROULIS mouvementRoulis, double positionRoulis) {
        this();
        this.mouvementGaucheDroite = mouvementGaucheDroite;
        this.positionGaucheDroite = positionGaucheDroite;
        this.mouvementHauBas = mouvementHauBas;
        this.positionHautBas = positionHautBas;
        this.mouvementRoulis = mouvementRoulis;
        this.positionRoulis = positionRoulis;
    }

    public MouvementCouEvent(MOUVEMENTS_GAUCHE_DROITE mouvementGaucheDroite, double positionGaucheDroite) {
        this(mouvementGaucheDroite, positionGaucheDroite, null, POSITION_NEUTRE, null, POSITION_NEUTRE);
    }

    public MouvementCouEvent(MOUVEMENTS_HAUT_BAS mouvementHauBas, double positionHautBas) {
        this(null, POSITION_NEUTRE, mouvementHauBas, positionHautBas, null, POSITION_NEUTRE);
    }

    public MouvementCouEvent(MOUVEMENTS_ROULIS mouvementRoulis, double positionRoulis) {
        this(null, POSITION_NEUTRE, null, POSITION_NEUTRE, mouvementRoulis, positionRoulis);
    }

    public MOUVEMENTS_GAUCHE_DROITE getMouvementGaucheDroite() {
        return mouvementGaucheDroite;
    }

    public void setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE mouvementGaucheDroite) {
        this.mouvementGaucheDroite = mouvementGaucheDroite;
    }

    public MOUVEMENTS_HAUT_BAS getMouvementHauBas() {
        return mouvementHauBas;
    }

    public void setMouvementHauBas(MOUVEMENTS_HAUT_BAS mouvementHauBas) {
        this.mouvementHauBas = mouvementHauBas;
    }

    public double getPositionGaucheDroite() {
        return positionGaucheDroite;
    }

    public void setPositionGaucheDroite(double positionGaucheDroite) {
        this.positionGaucheDroite = positionGaucheDroite;
    }

    public double getPositionHautBas() {
        return positionHautBas;
    }

    public void setPositionHautBas(double positionHautBas) {
        this.positionHautBas = positionHautBas;
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

    public Double getVitesseGaucheDroite() {
        return vitesseGaucheDroite;
    }

    public void setVitesseGaucheDroite(Double vitesseGaucheDroite) {
        this.vitesseGaucheDroite = vitesseGaucheDroite;
    }

    public Double getAccelerationGaucheDroite() {
        return accelerationGaucheDroite;
    }

    public void setAccelerationGaucheDroite(Double accelerationGaucheDroite) {
        this.accelerationGaucheDroite = accelerationGaucheDroite;
    }

    public Double getVitesseHautBas() {
        return vitesseHautBas;
    }

    public void setVitesseHautBas(Double vitesseHautBas) {
        this.vitesseHautBas = vitesseHautBas;
    }

    public Double getAccelerationHautBas() {
        return accelerationHautBas;
    }

    public void setAccelerationHautBas(Double accelerationHautBas) {
        this.accelerationHautBas = accelerationHautBas;
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

    public double getAngleGaucheDroite() {
        return angleGaucheDroite;
    }

    public void setAngleGaucheDroite(double angleGaucheDroite) {
        this.angleGaucheDroite = angleGaucheDroite;
    }

    public double getAngleHautBas() {
        return angleHautBas;
    }

    public void setAngleHautBas(double angleHautBas) {
        this.angleHautBas = angleHautBas;
    }

    @Override
    public String toString() {
        return "MouvementCouEvent{" +
                "mouvementGaucheDroite=" + mouvementGaucheDroite +
                ", positionGaucheDroite=" + positionGaucheDroite +
                ", angleGaucheDroite=" + angleGaucheDroite +
                ", vitesseGaucheDroite=" + vitesseGaucheDroite +
                ", accelerationGaucheDroite=" + accelerationGaucheDroite +
                ", mouvementHauBas=" + mouvementHauBas +
                ", positionHautBas=" + positionHautBas +
                ", angleHautBas=" + angleHautBas +
                ", vitesseHautBas=" + vitesseHautBas +
                ", accelerationHautBas=" + accelerationHautBas +
                ", mouvementRoulis=" + mouvementRoulis +
                ", positionRoulis=" + positionRoulis +
                ", vitesseRoulis=" + vitesseRoulis +
                ", accelerationRoulis=" + accelerationRoulis +
                ", synchrone=" + synchrone +
                '}';
    }
}
