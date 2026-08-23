package fr.roboteek.robot.systemenerveux.event;

import java.util.List;

/**
 * Visages perçus par le capteur de vision à un instant donné, avec leur nom quand la
 * reconnaissance a abouti.
 * <p>
 * Cet évènement existe parce que le {@link VideoEvent} ne suffit pas : il n'est publié que
 * lorsqu'un client est abonné au flux vidéo. La reconnaissance, elle, tourne en permanence
 * pour piloter le comportement du robot — mais son résultat ne sortait jamais de l'organe
 * tablette éteinte. Ce qui sert à décider (aller saluer quelqu'un, le regarder) est donc
 * publié inconditionnellement, ici, et le flux vidéo reste ce qu'il est : de l'affichage.
 * <p>
 * Les dimensions de l'image accompagnent les boîtes englobantes : un centroïde en pixels
 * n'est exploitable (conversion en angle pour le cou) qu'au regard de la taille de l'image
 * d'où il vient.
 */
public class VisagePercuEvent extends RobotEvent {

    public static final String EVENT_TYPE = "visage-percu";

    /**
     * Visages présents dans le champ. Liste <b>vide</b> = le champ vient de se vider,
     * signal du départ de la personne.
     */
    private final List<VisagePercu> visages;

    /** Largeur de l'image analysée, en pixels. */
    private final int largeurImage;

    /** Hauteur de l'image analysée, en pixels. */
    private final int hauteurImage;

    public VisagePercuEvent(List<VisagePercu> visages, int largeurImage, int hauteurImage) {
        super(EVENT_TYPE);
        this.visages = visages;
        this.largeurImage = largeurImage;
        this.hauteurImage = hauteurImage;
    }

    public List<VisagePercu> getVisages() {
        return visages;
    }

    public int getLargeurImage() {
        return largeurImage;
    }

    public int getHauteurImage() {
        return hauteurImage;
    }

}
