package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger les yeux.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementYeuxEvent extends RobotEvent {
	
	public static final String EVENT_TYPE = "MouvementYeux";

    public static enum MOUVEMENTS_OEIL {TOURNER_BAS, TOURNER_HAUT, STOPPER};
    
    /** Mouvement de l'oeil gauche à effectuer. */
    private MOUVEMENTS_OEIL mouvementOeilGauche;
    
    /** Position  de l'oeil gauche (0 : en bas, 180 : en haut). */
    private double positionOeilGauche = -1;
    
    /** Mouvement de l'oeil droit à effectuer. */
    private MOUVEMENTS_OEIL mouvementOeilDroit;
    
    /** Position  de l'oeil droit (0 : en bas, 180 : en haut). */
    private double positionOeilDroit = -1;
    
	public MouvementYeuxEvent() {
		super(EVENT_TYPE);
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

	@Override
	public String toString() {
		return "MouvementYeuxEvent [mouvementOeilGauche=" + mouvementOeilGauche + ", positionOeilGauche="
				+ positionOeilGauche + ", mouvementOeilDroit=" + mouvementOeilDroit + ", positionOeilDroit="
				+ positionOeilDroit + ", toString()=" + super.toString() + "]";
	}
}
