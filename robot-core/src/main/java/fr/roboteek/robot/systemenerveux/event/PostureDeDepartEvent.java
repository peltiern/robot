package fr.roboteek.robot.systemenerveux.event;

/**
 * Demande à tous les axes de rejoindre leur <b>posture de départ</b>, celle où le robot se place
 * au démarrage.
 * <p>
 * Chaque organe sait où est la sienne ; l'émetteur n'a pas à la connaître. C'est tout l'objet de
 * cet évènement : la manette envoyait jusqu'au 2026-09-08 des positions <b>zéro</b> écrites en dur,
 * avec des vitesses écrites en dur elles aussi. Ça marchait tant que le zéro était la position de
 * départ — ce qui a cessé d'être vrai pour les yeux, dont le zéro est désormais la coque de niveau.
 * Le bouton ramenait donc les yeux 10° au-dessus de leur posture de repos, et à une vitesse qui
 * n'était plus celle de la configuration.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class PostureDeDepartEvent extends RobotEvent {

    public static final String EVENT_TYPE = "posture-de-depart";

    public PostureDeDepartEvent() {
        super(EVENT_TYPE);
    }
}
