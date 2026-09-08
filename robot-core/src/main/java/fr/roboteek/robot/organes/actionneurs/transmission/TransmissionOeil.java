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
     * butées.
     * <p>
     * Le contact vaut pour la <b>somme</b> des deux angles : les deux coques partagent un arbre et
     * se rencontrent quand {@code angleGauche + angleDroit} atteint 13,74°. Un œil seul, l'autre de
     * niveau, va donc jusqu'à 13,74. C'est ce qui a servi à étalonner le zéro le 2026-09-07, et
     * c'est le seul repère dur du mécanisme — aucun instrument n'est nécessaire pour le trouver.
     */
    public static final double SERVO_MINI_AU_CONTACT = -5.599;
    public static final double SERVO_MAXI_AU_CONTACT = 21.301;

    /**
     * Position du bras où le quadrilatère n'a plus de solution : la bielle et le bras y sont
     * alignés, et le mécanisme n'a plus aucune autorité. C'est la <b>seule</b> limite que cette
     * classe fasse respecter, parce qu'au-delà il n'y a pas d'angle d'œil à rendre.
     * {@link #SERVO_MAXI_AU_CONTACT} tombe juste avant — la géométrie protège le
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
     *                             moteur, signé. Porte deux choses à la fois : le <b>signe</b>, les
     *                             deux servos étant montés en miroir, et l'<b>échelle</b>, qui ne
     *                             vaut pas 1 — une unité de position Phidgets fait 1,26° de servo
     *                             réel, cf. {@code PhidgetsServoMotor.DEGRES_SERVO_PAR_UNITE}.
     *                             Cité par son nom et non par un {@code @link} : cette classe est
     *                             de la géométrie pure et ne connaît aucun matériel.
     */
    public TransmissionOeil(double zeroMoteur, double degresServoParUniteMoteur) {
        this.versLeServo = Transmission.affine(zeroMoteur, degresServoParUniteMoteur);
    }

    /**
     * <b>L'angle d'organe est celui du relevé</b>, sans négation : positif = bord extérieur vers le
     * haut, les deux coques se rapprochant l'une de l'autre.
     * <p>
     * Il y avait ici une négation, posée le 2026-09-04 pour que les curseurs du HUD ne soient pas
     * retournés. Elle mettait les 35° de course longue du côté positif, là où la mécanique n'en
     * offre que 6,87 — le javadoc de l'époque notait déjà que « le robot donne l'impression
     * inverse » sans en tirer les conséquences. Le 2026-09-07, Nicolas a amené les deux yeux au
     * contact : ils se touchent en montant la commande, pas en la baissant. La négation était donc
     * fausse, et les butées se trouvaient posées du mauvais côté du mécanisme — d'où un maximum
     * configuré 14° au-delà du contact, contre lequel les servos forçaient à chaque animation.
     * <p>
     * Le sens perçu ne change pas pour autant : une commande positive relevait déjà le bord
     * extérieur avant la correction, et le fait toujours. Ce qui change, c'est que la
     * non-linéarité 1,25 → 3,70 n'est plus appliquée en miroir.
     */
    @Override
    public double versMoteur(double angleOrgane) {
        return versLeServo.versMoteur(servoDepuisOeil(angleOrgane));
    }

    @Override
    public double depuisMoteur(double positionMoteur) {
        return oeilDepuisServo(versLeServo.depuisMoteur(positionMoteur));
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
