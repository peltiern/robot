package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger les yeux.
 * Les champs de position sont null quand l'axe n'est pas concerné par l'événement.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementYeuxEvent extends RobotEvent {

    public static final String EVENT_TYPE = "mouvement-yeux";

    public enum MOUVEMENTS_OEIL {TOURNER_BAS, TOURNER_HAUT, ROLL, STOPPER}

    public enum MOUVEMENTS_ROULIS {HORAIRE, ANTI_HORAIRE, STOPPER}

    /** Mouvement continu de l'oeil gauche. */
    private MOUVEMENTS_OEIL mouvementOeilGauche;

    /** Position absolue de l'oeil gauche en degrés. Null = pas de commande de position. */
    private Double positionOeilGauche;

    private Double vitesseOeilGauche;
    private Double accelerationOeilGauche;

    /** Mouvement continu de l'oeil droit. */
    private MOUVEMENTS_OEIL mouvementOeilDroit;

    /** Position absolue de l'oeil droit en degrés. Null = pas de commande de position. */
    private Double positionOeilDroit;

    private Double vitesseOeilDroit;
    private Double accelerationOeilDroit;

    /** Mouvement de roulis. */
    private MOUVEMENTS_ROULIS mouvementRoulis;

    /** Angle de roulis en degrés. Null = pas de commande de roulis. */
    private Double positionRoulis;

    private Double vitesseRoulis;
    private Double accelerationRoulis;

    /** Flag indiquant que le mouvement doit être synchrone. */
    private boolean synchrone = false;

    public MouvementYeuxEvent() {
        super(EVENT_TYPE);
    }

    public MouvementYeuxEvent(MOUVEMENTS_OEIL mouvementOeilGauche, Double positionOeilGauche,
                               MOUVEMENTS_OEIL mouvementOeilDroit, Double positionOeilDroit,
                               MOUVEMENTS_ROULIS mouvementRoulis, Double positionRoulis) {
        this();
        this.mouvementOeilGauche = mouvementOeilGauche;
        this.positionOeilGauche = positionOeilGauche;
        this.mouvementOeilDroit = mouvementOeilDroit;
        this.positionOeilDroit = positionOeilDroit;
        this.mouvementRoulis = mouvementRoulis;
        this.positionRoulis = positionRoulis;
    }

    public MouvementYeuxEvent(MOUVEMENTS_OEIL mouvementOeilGauche, Double positionOeilGauche,
                               MOUVEMENTS_OEIL mouvementOeilDroit, Double positionOeilDroit) {
        this(mouvementOeilGauche, positionOeilGauche, mouvementOeilDroit, positionOeilDroit, null, null);
    }

    public MouvementYeuxEvent(MOUVEMENTS_ROULIS mouvementRoulis, Double positionRoulis) {
        this(null, null, null, null, mouvementRoulis, positionRoulis);
    }

    public MOUVEMENTS_OEIL getMouvementOeilGauche() { return mouvementOeilGauche; }
    public void setMouvementOeilGauche(MOUVEMENTS_OEIL mouvementOeilGauche) { this.mouvementOeilGauche = mouvementOeilGauche; }

    public Double getPositionOeilGauche() { return positionOeilGauche; }
    public void setPositionOeilGauche(Double positionOeilGauche) { this.positionOeilGauche = positionOeilGauche; }

    public Double getVitesseOeilGauche() { return vitesseOeilGauche; }
    public void setVitesseOeilGauche(Double vitesseOeilGauche) { this.vitesseOeilGauche = vitesseOeilGauche; }

    public Double getAccelerationOeilGauche() { return accelerationOeilGauche; }
    public void setAccelerationOeilGauche(Double accelerationOeilGauche) { this.accelerationOeilGauche = accelerationOeilGauche; }

    public MOUVEMENTS_OEIL getMouvementOeilDroit() { return mouvementOeilDroit; }
    public void setMouvementOeilDroit(MOUVEMENTS_OEIL mouvementOeilDroit) { this.mouvementOeilDroit = mouvementOeilDroit; }

    public Double getPositionOeilDroit() { return positionOeilDroit; }
    public void setPositionOeilDroit(Double positionOeilDroit) { this.positionOeilDroit = positionOeilDroit; }

    public Double getVitesseOeilDroit() { return vitesseOeilDroit; }
    public void setVitesseOeilDroit(Double vitesseOeilDroit) { this.vitesseOeilDroit = vitesseOeilDroit; }

    public Double getAccelerationOeilDroit() { return accelerationOeilDroit; }
    public void setAccelerationOeilDroit(Double accelerationOeilDroit) { this.accelerationOeilDroit = accelerationOeilDroit; }

    public MOUVEMENTS_ROULIS getMouvementRoulis() { return mouvementRoulis; }
    public void setMouvementRoulis(MOUVEMENTS_ROULIS mouvementRoulis) { this.mouvementRoulis = mouvementRoulis; }

    public Double getPositionRoulis() { return positionRoulis; }
    public void setPositionRoulis(Double positionRoulis) { this.positionRoulis = positionRoulis; }

    public Double getVitesseRoulis() { return vitesseRoulis; }
    public void setVitesseRoulis(Double vitesseRoulis) { this.vitesseRoulis = vitesseRoulis; }

    public Double getAccelerationRoulis() { return accelerationRoulis; }
    public void setAccelerationRoulis(Double accelerationRoulis) { this.accelerationRoulis = accelerationRoulis; }

    public boolean isSynchrone() { return synchrone; }
    public void setSynchrone(boolean synchrone) { this.synchrone = synchrone; }

    @Override
    public String toString() {
        return "MouvementYeuxEvent{" +
                "mouvementOeilGauche=" + mouvementOeilGauche +
                ", positionOeilGauche=" + positionOeilGauche +
                ", vitesseOeilGauche=" + vitesseOeilGauche +
                ", mouvementOeilDroit=" + mouvementOeilDroit +
                ", positionOeilDroit=" + positionOeilDroit +
                ", vitesseOeilDroit=" + vitesseOeilDroit +
                ", mouvementRoulis=" + mouvementRoulis +
                ", positionRoulis=" + positionRoulis +
                ", synchrone=" + synchrone + "}";
    }
}
