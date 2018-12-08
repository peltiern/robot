package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_ROULIS;

/**
 * Evènement pour bouger les yeux.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementYeuxEvent extends RobotEvent {
	
	public static final String EVENT_TYPE = "MouvementYeux";

    public static enum MOUVEMENTS_OEIL {TOURNER_BAS, TOURNER_HAUT, ROLL, STOPPER};
    
    public static enum MOUVEMENTS_ROULIS {HORAIRE, ANTI_HORAIRE, STOPPER};
    
    /** Mouvement de l'oeil gauche à effectuer. */
    private MOUVEMENTS_OEIL mouvementOeilGauche;
    
    /** Position  de l'oeil gauche (0 : en bas, 180 : en haut). */
    private double positionOeilGauche = -1;
    
    /** Mouvement de l'oeil droit à effectuer. */
    private MOUVEMENTS_OEIL mouvementOeilDroit;
    
    /** Position  de l'oeil droit (0 : en bas, 180 : en haut). */
    private double positionOeilDroit = -1;
    
    /** Mouvement "Roulis" à effectuer. */
    private MOUVEMENTS_ROULIS mouvementRoulis;
    
    /** Position du roulis (-90 : en bas, 90 : en haut). */
    private double positionRoulis = 0;
    
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

	@Override
	public String toString() {
		return "MouvementYeuxEvent [mouvementOeilGauche=" + mouvementOeilGauche + ", positionOeilGauche="
				+ positionOeilGauche + ", mouvementOeilDroit=" + mouvementOeilDroit + ", positionOeilDroit="
				+ positionOeilDroit + ", mouvementRoulis=" + mouvementRoulis + ", positionRoulis=" + positionRoulis
				+ ", toString()=" + super.toString() + "]";
	}


}
