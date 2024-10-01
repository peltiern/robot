package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour lire du texte.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ParoleEvent extends RobotEvent {

    public static final String EVENT_TYPE = "parole";

    /**
     * Texte à dire.
     */
    private String texte;

    /**
     * Pour test.
     */
    private boolean pourTest = false;

    public ParoleEvent() {
        super(EVENT_TYPE);
    }

    /**
     * Récupère la valeur de texte.
     *
     * @return la valeur de texte
     */
    public String getTexte() {
        return texte;
    }

    /**
     * Définit la valeur de texte.
     *
     * @param texte la nouvelle valeur de texte
     */
    public void setTexte(String texte) {
        this.texte = texte;
    }

    public boolean isPourTest() {
        return pourTest;
    }

    public void setPourTest(boolean pourTest) {
        this.pourTest = pourTest;
    }

    @Override
    public String toString() {
        return "ParoleEvent{" +
                "texte='" + texte + '\'' +
                ", pourTest=" + pourTest +
                "} " + super.toString();
    }
}
