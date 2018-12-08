package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger la tête.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementTeteEvent extends RobotEvent {
	
	public static final String EVENT_TYPE = "MouvementTete";

    public static enum MOUVEMENTS_GAUCHE_DROITE {TOURNER_GAUCHE, TOURNER_DROITE, STOPPER};
    
    public static enum MOUVEMENTS_HAUT_BAS {TOURNER_HAUT, TOURNER_BAS, STOPPER};
    
    public static enum MOUVEMENTS_ROULIS {HORAIRE, ANTI_HORAIRE, STOPPER};
    
    /** Mouvement "Gauche - Droite" à effectuer. */
    private MOUVEMENTS_GAUCHE_DROITE mouvementGaucheDroite;
    
    /** Position "Gauche - Droite" (0 : à gauche, 180 : à droite). */
    private double positionGaucheDroite = -1;
    
    /** Mouvement "Haut - Bas" à effectuer. */
    private MOUVEMENTS_HAUT_BAS mouvementHauBas;
    
    /** Position "Haut - Bas" (0 : en bas, 180 : en haut). */
    private double positionHautBas = -1;
    
    /** Mouvement "Roulis" à effectuer. */
    private MOUVEMENTS_ROULIS mouvementRoulis;
    
    /** Position du roulis (-90 : en bas, 90 : en haut). */
    private double positionRoulis = 0;
    
	public MouvementTeteEvent() {
		super(EVENT_TYPE);
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

	@Override
	public String toString() {
		return "MouvementTeteEvent [mouvementGaucheDroite=" + mouvementGaucheDroite + ", positionGaucheDroite="
				+ positionGaucheDroite + ", mouvementHauBas=" + mouvementHauBas + ", positionHautBas=" + positionHautBas
				+ ", mouvementRoulis=" + mouvementRoulis + ", positionRoulis=" + positionRoulis + ", toString()="
				+ super.toString() + "]";
	}


}
