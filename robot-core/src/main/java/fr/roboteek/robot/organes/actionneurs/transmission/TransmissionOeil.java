package fr.roboteek.robot.organes.actionneurs.transmission;

/**
 * La tringlerie d'un œil de Wall-E : un quadrilatère articulé, dont le rapport n'est pas constant.
 * <p>
 * <b>Le servo n'est pas sur l'axe de l'œil.</b> Les deux coques pivotent sur un arbre commun ; le
 * servo est vissé <i>dans</i> la coque, à 87,6 mm de cet arbre, et pousse une bielle ancrée sur la
 * platine fixe. L'angle du bras est donc imposé dans le repère de la coque, et l'angle de la coque
 * est ce qui reste pour que la bielle garde sa longueur.
 * <p>
 * Conséquence : <b>un degré de servo ne fait pas un degré d'œil</b>, et le rapport varie de 1,25
 * au neutre à 3,70 en butée basse. Une rampe régulière commandée en degrés de servo produit donc
 * un œil qui accélère d'un facteur trois en descendant — le genre de défaut qu'on met un mois à
 * nommer quand on regarde une timeline en se demandant pourquoi le geste n'est pas celui qu'on a
 * dessiné.
 * <p>
 * Toutes les cotes viennent du relevé du 2026-08-31 sur {@code Wall-E Eye.stl}, refait en 40 s par
 * {@code robot-core/3d/mesures/releve_stl.py} ; la fiche complète est dans
 * {@code robot-core/3d/mesures/MESURES.md}. <b>Le bras de servo porte quatre trous</b>, à 8, 16,
 * 24 et 32 mm ; c'est celui de 32 qui est monté. Le déplacer change tout le rapport et rend cette
 * classe fausse — c'est le seul réglage matériel de la tringlerie.
 */
public final class TransmissionOeil implements Transmission {

    /** Ancrage de la bielle, sur la platine fixe. Repère centré sur l'axe des coques. */
    private static final double ANCRAGE_X = 13.000;
    private static final double ANCRAGE_Z = -25.999;

    /** Axe de sortie du servo, porté par la coque. */
    private static final double SERVO_X = 86.134;
    private static final double SERVO_Z = -16.082;

    private static final double BRAS = 31.999;
    private static final double BIELLE = 84.731;

    /** Orientation du bras de servo au neutre. */
    private static final double BRAS_AU_NEUTRE = Math.toRadians(-73.653);

    private static final double BATI = Math.hypot(ANCRAGE_X, ANCRAGE_Z);
    private static final double ANGLE_BATI = Math.atan2(ANCRAGE_Z, ANCRAGE_X);

    /** Décalage qui ramène l'œil à 0 quand le servo est à 0 : la fermeture n'est pas nulle là. */
    private static final double FERMETURE_AU_NEUTRE = fermeture(0);

    /**
     * Angles de servo où les <b>deux coques se touchent</b>. Vérifiés en cherchant, sur le contour
     * des 90 points du relevé, quand une coque franchit le plan médian — les deux partageant le
     * même arbre, c'est exactement là qu'elles se rencontrent.
     * <p>
     * <b>Cette classe ne les applique pas</b>, et c'est délibéré : ce sont des limites de
     * politique, pas de géométrie. Leur place est dans {@code robot.properties}, avec les autres
     * butées. La position de <b>repos</b> est d'ailleurs volontairement au-delà — c'est l'appui
     * mécanique stable sur lequel les yeux se posent à l'arrêt — et une transmission qui écrêterait
     * ici l'empêcherait d'être atteinte.
     */
    public static final double SERVO_CONTACT_DES_COQUES_BAS = -5.599;
    public static final double SERVO_CONTACT_DES_COQUES_HAUT = 21.301;

    /**
     * Position du bras où le quadrilatère n'a plus de solution : la bielle et le bras y sont
     * alignés, et le mécanisme n'a plus aucune autorité. C'est la <b>seule</b> limite que cette
     * classe fasse respecter, parce qu'au-delà il n'y a pas d'angle d'œil à rendre.
     * {@link #SERVO_CONTACT_DES_COQUES_HAUT} tombe juste avant — la géométrie protège le
     * mécanisme, ce n'est probablement pas un hasard.
     */
    public static final double SERVO_POINT_MORT = 22.331;

    /**
     * Borne basse de la recherche inverse. Très au-delà de tout ce que la mécanique atteint (l'œil
     * y serait à 67°) : la loi reste définie de ce côté, il n'y a donc rien à interdire, seulement
     * un intervalle à encadrer.
     */
    private static final double SERVO_TRES_BAS = -60;

    private final Transmission versLeServo;

    /**
     * @param zeroMoteur           position moteur où l'œil est à 0
     * @param degresServoParUniteMoteur degrés de servo <b>mécaniques</b> pour une unité de position
     *                             moteur, signé. Porte deux choses à la fois : les deux servos sont
     *                             montés en miroir, et le sens du repère mécanique du relevé
     *                             (servo positif = bord extérieur vers le bas) reste à confronter
     *                             au robot.
     */
    public TransmissionOeil(double zeroMoteur, double degresServoParUniteMoteur) {
        this.versLeServo = Transmission.affine(zeroMoteur, degresServoParUniteMoteur);
    }

    /**
     * <b>Le signe de l'angle d'organe</b> est l'opposé de celui du relevé.
     * <p>
     * Le relevé compte positif le bord extérieur vers le haut ; ici l'angle croît dans le sens où
     * la commande relative croissait déjà — celui qui, à +20, donne au robot l'air alerte. Ce n'est
     * pas de la coquetterie : les curseurs du HUD sont verticaux et lisent leurs bornes dans la
     * configuration. Garder le signe du relevé les retournerait, et c'est exactement le défaut
     * qu'on vient de corriger sur le panoramique.
     * <p>
     * Reste un point non résolu, sans conséquence sur le code : la géométrie place les 35° de
     * course du côté que le relevé nomme « bord extérieur vers le bas », alors que le robot donne
     * l'impression inverse. Aucune ligne ne dépend de ce mot — seulement les commentaires.
     */
    @Override
    public double versMoteur(double angleOrgane) {
        return versLeServo.versMoteur(servoDepuisOeil(-angleOrgane));
    }

    @Override
    public double depuisMoteur(double positionMoteur) {
        return -oeilDepuisServo(versLeServo.depuisMoteur(positionMoteur));
    }

    /**
     * Angle d'œil pour un angle de servo, par la fermeture du quadrilatère. Forme close.
     * <p>
     * <b>Dans le repère du relevé</b> : positif = bord extérieur vers le haut. Au-delà du point
     * mort la fermeture n'a plus de solution ; on rend alors l'angle du point mort, parce qu'un
     * {@code NaN} ferait voyager le défaut jusqu'à une consigne moteur silencieusement absurde.
     */
    public static double oeilDepuisServo(double angleServo) {
        double borne = Math.min(angleServo, SERVO_POINT_MORT);
        return Math.toDegrees(fermeture(borne) - FERMETURE_AU_NEUTRE);
    }

    /**
     * Angle de servo pour un angle d'œil, par dichotomie.
     * <p>
     * <b>Dans le repère du relevé</b>, comme {@link #oeilDepuisServo}. Pas de forme close dans ce
     * sens, et pas besoin : la fonction est strictement décroissante, donc quarante bissections
     * suffisent à descendre sous le millionième de degré — très en deçà de ce que le contrôleur
     * sait commander.
     */
    public static double servoDepuisOeil(double angleOeil) {
        double bas = SERVO_TRES_BAS;
        double haut = SERVO_POINT_MORT;
        if (angleOeil >= oeilDepuisServo(bas)) {
            return bas;
        }
        if (angleOeil <= oeilDepuisServo(haut)) {
            return haut;
        }
        for (int i = 0; i < 40; i++) {
            double milieu = (bas + haut) / 2;
            if (oeilDepuisServo(milieu) > angleOeil) {
                bas = milieu;
            } else {
                haut = milieu;
            }
        }
        return (bas + haut) / 2;
    }

    /**
     * Fermeture du quadrilatère O–S–P–D : angle absolu de la coque quand le bras est à
     * {@code angleServo} du neutre. Le décalage à l'origine est retiré par
     * {@link #oeilDepuisServo}.
     */
    private static double fermeture(double angleServo) {
        double orientation = BRAS_AU_NEUTRE + Math.toRadians(angleServo);
        double rotuleX = SERVO_X + BRAS * Math.cos(orientation);
        double rotuleZ = SERVO_Z + BRAS * Math.sin(orientation);
        double distance = Math.hypot(rotuleX, rotuleZ);
        double angleRotule = Math.atan2(rotuleZ, rotuleX);
        double cosinus = (distance * distance + BATI * BATI - BIELLE * BIELLE) / (2 * distance * BATI);
        return ANGLE_BATI - angleRotule + Math.acos(Math.clamp(cosinus, -1, 1));
    }
}
