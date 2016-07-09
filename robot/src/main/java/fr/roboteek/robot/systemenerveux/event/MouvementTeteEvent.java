package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger la tête.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementTeteEvent extends RobotEvent {

    public static enum MOUVEMENTS_GAUCHE_DROITE {TOURNER_GAUCHE, TOURNER_DROITE, STOPPER};
    
    public static enum MOUVEMENTS_HAUT_BAS {TOURNER_HAUT, TOURNER_BAS, STOPPER};
    
    /** Mouvement "Gauche - Droite" à effectuer. */
    private MOUVEMENTS_GAUCHE_DROITE mouvementGaucheDroite;
    
    /** Mouvement "Haut - Bas" à effectuer. */
    private MOUVEMENTS_HAUT_BAS mouvementHauBas;

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
}
