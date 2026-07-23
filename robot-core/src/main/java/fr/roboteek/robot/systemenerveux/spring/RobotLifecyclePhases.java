package fr.roboteek.robot.systemenerveux.spring;

/**
 * Phases de démarrage/arrêt des organes du robot (au sens {@link org.springframework.context.SmartLifecycle}).
 * <p>
 * Une phase basse démarre en premier et s'arrête en dernier.
 * Les moteurs ont la phase la plus haute : ils démarrent en dernier et s'arrêtent en premier,
 * ce qui garantit qu'aucun mouvement n'est possible tant que le reste du robot n'est pas prêt,
 * et qu'ils sont coupés avant tout le reste lors de l'arrêt.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public final class RobotLifecyclePhases {

    /** Infrastructure (bus d'évènements, services techniques). */
    public static final int INFRASTRUCTURE = 0;

    /** Capteurs (micro, vision, ...). */
    public static final int CAPTEURS = 100;

    /** Organes sans moteur (sons, parole, ...). */
    public static final int ORGANES = 200;

    /** Organes à moteurs (cou, yeux, chenilles) : derniers démarrés, premiers arrêtés. */
    public static final int MOTEURS = 300;

    private RobotLifecyclePhases() {
    }
}
