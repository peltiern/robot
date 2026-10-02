package fr.roboteek.robot.configuration.phidgets;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface PhidgetsConfig extends Config {

    @Key("phidgets.hub.serial.number")
    @DefaultValue("561050")
    int hubSerialNumber();

    @Key("phidgets.differential.driving.motor.left.port")
    @DefaultValue("1")
    int differentialDrivingLeftMotorPort();

    @Key("phidgets.differential.driving.motor.right.port")
    @DefaultValue("2")
    int differentialDrivingRightMotorPort();

    @Key("phidgets.differential.driving.motor.max.speed")
    @DefaultValue("0.7")
    double differentialDrivingMotorMaxSpeed();

    @Key("phidgets.differential.driving.motor.acceleration")
    @DefaultValue("1")
    double differentialDrivingMotorAcceleration();

    // Centrale inertielle MOT1102, branchée sur le même hub VINT que les chenilles.

    @Key("phidgets.spatial.port")
    @DefaultValue("3")
    int spatialPort();

    /** Période d'échantillonnage en millisecondes, entre 20 (minimum du MOT1102) et 1000. */
    @Key("phidgets.spatial.data.interval")
    @DefaultValue("50")
    int spatialDataInterval();

    /**
     * Algorithme de fusion embarqué : {@code IMU} (accéléromètre + gyroscope) ou {@code AHRS}
     * (avec en plus le magnétomètre, qui corrige la dérive du cap).
     * <p>
     * IMU par défaut, <b>par précaution et non sur mesure</b> : le magnétomètre lit aussi le champ
     * des moteurs voisins. Celui de leurs aimants est fixe et s'étalonne (Phidget Control Panel,
     * puis correction enregistrée dans la carte) ; celui de leur courant varie avec la vitesse et
     * ne s'étalonne pas. Son poids dépend de la distance entre la carte et les moteurs, qui n'a
     * pas encore été évaluée sur le robot. Pour trancher : carte étalonnée et robot immobile,
     * chenilles en marche puis à l'arrêt, regarder si la mesure {@code champMagnetique} de
     * {@code CapteurInertiel} bouge. Stable, AHRS est le meilleur choix ; sinon, rester en IMU,
     * où le cap dérive lentement mais sans à-coup.
     * <p>
     * Le roulis et le tangage tiennent sur la gravité : ils sont justes dans les deux cas.
     */
    @Key("phidgets.spatial.algorithm")
    @DefaultValue("IMU")
    String spatialAlgorithm();

    @Key("phidgets.neck.motor.pan.index")
    @DefaultValue("0")
    int neckLeftRightMotorIndex();

    @Key("phidgets.neck.motor.pan.position.init")
    @DefaultValue("85")
    double neckLeftRightMotorInitialPosition();

    @Key("phidgets.neck.motor.pan.position.min")
    @DefaultValue("20")
    double neckLeftRightMotorMinPosition();

    @Key("phidgets.neck.motor.pan.position.max")
    @DefaultValue("150")
    double neckLeftRightMotorMaxPosition();

    @Key("phidgets.neck.motor.pan.speed")
    @DefaultValue("100")
    double neckLeftRightMotorSpeed();

    @Key("phidgets.neck.motor.pan.acceleration")
    @DefaultValue("200")
    double neckLeftRightMotorAcceleration();

    @Key("phidgets.neck.motor.tilt.index")
    @DefaultValue("1")
    int neckTiltMotorIndex();

    @Key("phidgets.neck.motor.tilt.position.init")
    @DefaultValue("85")
    double neckTiltMotorInitialPosition();

    @Key("phidgets.neck.motor.tilt.position.min")
    @DefaultValue("35")
    double neckTiltMotorMinPosition();

    @Key("phidgets.neck.motor.tilt.position.max")
    @DefaultValue("110")
    double neckTiltMotorMaxPosition();

    @Key("phidgets.neck.motor.tilt.speed")
    @DefaultValue("100")
    double neckTiltMotorSpeed();

    @Key("phidgets.neck.motor.tilt.acceleration")
    @DefaultValue("200")
    double neckTiltMotorAcceleration();

    @Key("phidgets.neck.motor.up_down.index")
    @DefaultValue("3")
    int neckUpDownMotorIndex();

    @Key("phidgets.neck.motor.up_down.position.init")
    //@DefaultValue("85")
    double neckUpDownMotorInitialPosition();

    @Key("phidgets.neck.motor.up_down.position.min")
    //@DefaultValue("35")
    double neckUpDownMotorMinPosition();

    @Key("phidgets.neck.motor.up_down.position.max")
    //@DefaultValue("110")
    double neckUpDownMotorMaxPosition();

    @Key("phidgets.neck.motor.up_down.speed")
    //@DefaultValue("100")
    double neckUpDownMotorSpeed();

    @Key("phidgets.neck.motor.up_down.acceleration")
    //@DefaultValue("200")
    double neckUpDownMotorAcceleration();

    @Key("phidgets.eyes.motor.left.index")
    @DefaultValue("2")
    int eyeLeftMotorIndex();

    // Deux positions moteur par œil, et il ne faut pas les confondre — elles ont porté le même
    // nom jusqu'au 2026-09-07, et c'est ce qui a fait croire que le zéro des yeux était réglé.
    //
    //   .init  où le servo est amené au démarrage (reset()) et engagé par les outils hors ligne.
    //          Une POLITIQUE : c'est la posture de travail du robot, rien de plus.
    //   .zero  où le dessus de la coque est parallèle au plan de référence de la tête.
    //          Une MESURE : c'est l'origine des degrés d'œil de TransmissionOeil, donc celle des
    //          butées, de la position de repos et des animations.
    //
    // Les deux valeurs diffèrent d'un œil à l'autre : les servos sont montés en miroir et leurs
    // cannelures ne tombent pas au même endroit. Le zéro se mesure, il ne se déduit pas de l'autre.
    //
    // Les deux ne valent pas la même chose : le robot démarre les yeux 10° sous le niveau, un
    // regard un peu baissé demandé par Nicolas le 2026-09-08. C'est exactement ce que la séparation
    // permet — changer la posture de départ sans toucher à l'étalonnage, et inversement.

    @Key("phidgets.eyes.motor.left.position.init")
    @DefaultValue("95.90")
    double eyeLeftMotorInitialPosition();

    @Key("phidgets.eyes.motor.left.position.zero")
    @DefaultValue("91.06")
    double eyeLeftMotorZeroPosition();

    @Key("phidgets.eyes.left.speed")
    @DefaultValue("79")
    double eyeLeftSpeed();

    @Key("phidgets.eyes.left.acceleration")
    @DefaultValue("119")
    double eyeLeftAcceleration();

    @Key("phidgets.eyes.motor.right.index")
    @DefaultValue("3")
    int eyeRightMotorIndex();

    @Key("phidgets.eyes.motor.right.position.init")
    @DefaultValue("97.10")
    double eyeRightMotorInitialPosition();

    @Key("phidgets.eyes.motor.right.position.zero")
    @DefaultValue("101.94")
    double eyeRightMotorZeroPosition();

    @Key("phidgets.eyes.right.speed")
    @DefaultValue("79")
    double eyeRightSpeed();

    @Key("phidgets.eyes.right.acceleration")
    @DefaultValue("119")
    double eyeRightAcceleration();

    // Butées des yeux : en DEGRÉS D'ŒIL, pas en unités moteur, et POSITIF = bord extérieur
    // vers le haut, donc les deux coques se rapprochent. Elles ne sont plus réglées à la main
    // mais déduites de la mécanique : contact des coques quand la somme des deux angles atteint
    // 13,74, point mort de la tringlerie à -42,19. Voir le bloc « Eyes » de robot.properties.
    @Key("phidgets.eyes.position.min")
    @DefaultValue("-33")
    double eyePositionMin();

    @Key("phidgets.eyes.position.max")
    @DefaultValue("5.5")
    double eyePositionMax();

    // Positions de repos, rejointes AVANT le désengagement des servos à l'arrêt du robot
    // pour éviter que la tête et les yeux ne tombent d'un coup (position mécaniquement
    // stable, tête baissée). Optionnelles (type Double) : à défaut, la position
    // initiale/zéro est utilisée. Pour trouver les bonnes valeurs : placer la tête à la
    // manette puis lire les « Positions au moment de l'arrêt » dans les logs.

    @Key("phidgets.neck.motor.pan.position.rest")
    Double neckLeftRightMotorRestPosition();

    @Key("phidgets.neck.motor.tilt.position.rest")
    Double neckTiltMotorRestPosition();

    @Key("phidgets.neck.motor.up_down.position.rest")
    Double neckUpDownMotorRestPosition();

    @Key("phidgets.eyes.position.rest")
    Double eyeRestPosition();
}
