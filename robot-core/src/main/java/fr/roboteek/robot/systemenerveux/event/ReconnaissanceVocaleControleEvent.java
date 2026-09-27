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

    /**
     * Qui met l'écoute en pause : elle ne reprend que lorsque plus aucune source ne la retient (voir
     * {@code PausesDeLEcoute}). La réflexion de la conversation et la phrase partagent
     * {@link #PAROLE} : la reprise en fin de phrase libère les deux.
     */
    public enum SOURCE {PAROLE, SON}

    private CONTROLE controle;

    /** {@link SOURCE#PAROLE} par défaut : c'était la seule avant que les sons n'aient la leur. */
    private SOURCE source = SOURCE.PAROLE;

    public ReconnaissanceVocaleControleEvent() {
        super(EVENT_TYPE);
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

    public SOURCE getSource() {
        return source;
    }

    public void setSource(SOURCE source) {
        this.source = source;
    }

}
