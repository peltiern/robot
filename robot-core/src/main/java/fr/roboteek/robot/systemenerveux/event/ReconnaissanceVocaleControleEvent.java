package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement de contrôle de la reconnaissance vocale.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ReconnaissanceVocaleControleEvent extends RobotEvent {

    public static final String EVENT_TYPE = "reconnaissance-vocale-controle";

    /**
     * Constante de contrôle de la reconnaissance vocale.
     */
    public static enum CONTROLE {DEMARRER, METTRE_EN_PAUSE}

    ;

    private CONTROLE controle;

    public ReconnaissanceVocaleControleEvent() {
        super(EVENT_TYPE);
        // TODO Auto-generated constructor stub
    }

    /**
     * Récupère la valeur de controle.
     *
     * @return la valeur de controle
     */
    public CONTROLE getControle() {
        return controle;
    }

    /**
     * Définit la valeur de controle.
     *
     * @param controle la nouvelle valeur de controle
     */
    public void setControle(CONTROLE controle) {
        this.controle = controle;
    }

}
