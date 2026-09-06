package fr.roboteek.robot.util.webcam;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.util.phidgets.PostureDeTravail;
import fr.roboteek.robot.util.phidgets.PostureDeTravail.Axe;
import nu.pattern.OpenCV;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Étalonnage de la caméra du robot au damier, en une commande : la prise de vues, puis le calcul.
 * <p>
 * <b>Ce qu'on vient chercher, et pourquoi ça ne se devine pas.</b> {@code Regard} convertit un écart
 * en pixels en un écart en degrés à travers une <i>focale en pixels</i>, qu'il recalcule à chaque
 * image depuis {@code robot.regard.camera.champ.horizontal.degres}. Cette clé vaut 60,0 parce
 * qu'elle a été lue sur une fiche produit ; deux mesures indépendantes du 2026-08-15 — une par axe
 * du cou, sur deux mécaniques différentes — laissent penser qu'elle est fausse d'un facteur 1,35.
 * Tant qu'elle n'est pas mesurée, corriger les rapports de transmission du cou ferait passer le gain
 * de la boucle de regard au-dessus de 1, et la tête oscillerait sans fin.
 * <p>
 * <b>Trois résultats, pas un.</b> Le damier ne rend pas seulement la focale :
 * <ul>
 *   <li>{@code fx}, {@code fy} — la focale, d'où se déduit le champ à écrire dans la configuration ;</li>
 *   <li>les coefficients de <b>distorsion</b> — ils disent si le modèle sténopé de {@code Regard}
 *       tient, ou si l'objectif déforme assez pour qu'un visage vu au bord soit mal placé ;</li>
 *   <li>le <b>point principal</b> {@code cx}, {@code cy} — celui qu'on n'aurait jamais pensé à aller
 *       chercher. Le code suppose qu'il est au centre de l'image ({@code largeurImage / 2.0}). S'il
 *       ne l'est pas, le robot vise durablement à côté, la zone morte absorbe le biais, et rien ne
 *       le révèle jamais.</li>
 * </ul>
 * <p>
 * <b>Le robot doit être arrêté</b> : son organe de vision tient la webcam, et V4L2 ne la partage
 * pas. L'outil engage alors lui-même les cinq axes et les tient à la posture de travail le temps de
 * la prise (voir {@link PostureDeTravail}), puis repose la tête avant de lâcher — sans quoi la
 * caméra regarderait le sol pendant toute la séance. La contrepartie est qu'on capture <b>avant</b> tout encodage JPEG — les vraies 640×480 que la
 * vision analyse, sans les artefacts de compression qui font baver les coins du damier et ruinent la
 * détection sous-pixel.
 * <p>
 * <b>Prendre les vues seul.</b> Impossible de tenir le damier et de déclencher : l'outil déclenche
 * lui-même. Il garde une image quand le damier est trouvé, <i>net</i>, et <i>assez différent</i> de
 * tout ce qui est déjà gardé — sans ce dernier filtre on repart avec vingt fois la même pose, et une
 * calibration qui ne converge vers rien. Chaque prise sonne la cloche du terminal et affiche les
 * cases de l'image encore vides : c'est la variété des poses qui fait la qualité, pas le nombre.
 * <p>
 * <b>Aller dans les coins.</b> La distorsion ne vit qu'aux bords. Une série d'images bien centrées
 * donne une calibration d'apparence excellente — l'erreur de reprojection sera basse — qui ne
 * corrige rien là où ça compte.
 * <pre>
 * java -Dloader.main=fr.roboteek.robot.util.webcam.EtalonnageCamera -cp robot-core.jar \
 *      org.springframework.boot.loader.launch.PropertiesLauncher [colonnes] [lignes] [cote_mm] [nombre]
 * </pre>
 * {@code colonnes} et {@code lignes} sont les <b>coins internes</b> — les croisements où quatre
 * cases se touchent — et non les cases : un damier de 7×10 cases donne 6×9. Défauts :
 * {@code 6 9 50 20}, le damier du robot. <b>L'orientation, elle, n'a pas à être juste</b> :
 * l'outil essaie les deux sens et retient celui qui répond. Le côté des cases ne sert qu'à donner
 * une échelle métrique aux poses ; la focale en pixels, elle, sort juste sans lui.
 * <p>
 * Le mode {@code --depuis <dossier>} refait le calcul sur des images déjà prises, sans robot ni
 * webcam — pour rejouer un étalonnage, ou le faire tourner ailleurs sur des images rapatriées.
 * <p>
 * Le mode {@code --axes} ne touche pas au damier : il mesure, une fois la focale connue, combien de
 * degrés vaut une unité de commande sur chaque axe du cou. Voir {@link #mesurerLesAxes()}.
 * <p>
 * <b>Vérifié le 2026-09-05</b> sur quinze images de synthèse projetées depuis une caméra connue
 * ({@code fx = fy = 540}, {@code cx = 322}, {@code cy = 236}, sans distorsion) : l'outil rend
 * {@code fx = 539,61} et une distorsion nulle, erreur de reprojection 0,031 px. C'est ce jeu
 * d'essai qui a révélé le défaut de fenêtre d'affinage corrigé dans {@link #demiFenetre}.
 */
public final class EtalonnageCamera {

    private static final int LARGEUR = 640;
    private static final int HAUTEUR = 480;

    /**
     * Variance du laplacien en deçà de laquelle l'image est rejetée. Mesurée <b>sur le damier
     * seul</b> et non sur l'image entière : un damier flou devant un fond net passerait le seuil, et
     * c'est exactement ce qui se produit quand on déplace la cible à la main.
     */
    private static final double NETTETE_MINIMALE = 45.0;

    /**
     * Déplacement moyen d'un coin, en pixels, en deçà duquel la pose est jugée déjà vue. Assez bas
     * pour ne pas décourager l'opérateur, assez haut pour refuser la dérive lente d'une main qui
     * croit tenir le damier immobile.
     */
    private static final double ECART_MINIMAL_PIXELS = 30.0;

    /**
     * Baisse d'erreur de reprojection en deçà de laquelle libérer k3 ne se justifie pas. Un degré
     * de liberté de plus fait toujours baisser l'erreur ; il faut qu'il la fasse baisser
     * <b>beaucoup</b> pour qu'on croie qu'il décrit la courbure de l'objectif et non le bruit.
     */
    private static final double GAIN_QUI_JUSTIFIE_K3 = 0.8;

    private static final TermCriteria AFFINAGE =
            new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001);

    private EtalonnageCamera() {
    }

    public static void main(String[] args) throws Exception {
        OpenCV.loadLocally();

        if (args.length >= 1 && "--axes".equals(args[0])) {
            mesurerLesAxes();
            return;
        }

        boolean rejeu = args.length >= 2 && "--depuis".equals(args[0]);
        String[] chiffres = rejeu ? Arrays.copyOfRange(args, 2, args.length) : args;

        int colonnes = chiffres.length >= 1 ? Integer.parseInt(chiffres[0]) : 6;
        int lignes = chiffres.length >= 2 ? Integer.parseInt(chiffres[1]) : 9;
        double coteMm = chiffres.length >= 3 ? Double.parseDouble(chiffres[2]) : 50.0;
        int nombreVoulu = chiffres.length >= 4 ? Integer.parseInt(chiffres[3]) : 20;

        Size demande = new Size(colonnes, lignes);
        System.out.printf("Damier attendu : %d x %d coins internes (soit %d x %d cases), "
                        + "cases de %.0f mm%n",
                colonnes, lignes, colonnes + 1, lignes + 1, coteMm);

        Prise prise;
        if (rejeu) {
            prise = relireLesImages(Path.of(args[1]), demande);
        } else {
            // Un robot arrêté a le nez piqué : l'inclinaison du cou repose dix unités plus bas que
            // sa posture de travail, et la caméra regarde le sol. Rien de ce qu'on mesure ici n'en
            // dépend — la focale et la distorsion sont des propriétés de l'objectif, pas de la
            // direction visée — mais présenter un damier à un robot qui ne vous voit pas est
            // inutilement pénible. Le try-with-resources n'est pas une commodité : c'est lui qui
            // garantit que la tête est reposée avant le désengagement, même si la prise échoue.
            try (PostureDeTravail posture = PostureDeTravail.prendre()) {
                System.out.println("Robot en posture de travail : il regarde devant lui.");
                prise = prendreLesVues(demande, nombreVoulu);
            }
        }

        if (prise.vues().size() < 5) {
            System.out.println("Moins de 5 vues exploitables : l'étalonnage n'aurait aucun sens.");
            System.out.println("Si le damier n'a jamais été vu, c'est le compte de coins internes "
                    + "qui est faux : ce sont les croisements où quatre cases se touchent, pas les cases.");
            return;
        }
        calculer(prise.vues(), prise.grille(), coteMm);
    }

    /**
     * Les vues retenues et la grille qui a effectivement répondu.
     * <p>
     * La grille n'est pas forcément celle qu'on a demandée : {@code findChessboardCorners} cherche
     * une grille orientée, et un damier tenu en portrait quand on a annoncé un paysage n'est
     * <b>jamais</b> trouvé — l'écran reste muet et rien ne dit pourquoi. L'outil essaie donc les deux
     * sens sur la première image, puis s'en tient à celui qui a répondu : le damier ne change pas de
     * forme en cours de séance, et mélanger les deux fausserait les points modèles.
     */
    private record Prise(Size grille, List<MatOfPoint2f> vues) {
    }

    /**
     * Ce que l'étalonnage au damier a appris de la caméra, et qui doit survivre à la séance.
     * <p>
     * Vit dans {@code $ROBOT_HOME/etalonnage/camera.properties}. Sans ce fichier, la mesure des axes
     * travaillerait sur l'image brute d'un <b>fisheye recadré</b> : un déplacement de 90 px près du
     * bord n'y vaut pas le même angle qu'au centre, et la dispersion qui en résulte se prend pour de
     * la non-linéarité mécanique. L'essai du 2026-09-06 rendait ainsi de 1,05 à 2,38 degrés par
     * unité sur le même axe. On peut s'en passer — le mode dégradé le dit haut et fort — mais le
     * résultat ne vaut alors que près du centre de l'image.
     */
    private record Camera(double fx, double fy, double cx, double cy, double[] distorsion) {

        private static final Path FICHIER = Path.of(System.getenv(Constantes.ENV_VAR_ROBOT_HOME),
                "etalonnage", "camera.properties");

        /**
         * L'ordre d'OpenCV, et il n'est pas celui qu'on devine : les deux termes TANGENTIELS
         * s'intercalent entre le deuxième et le troisième terme radial. Les nommer k1..k5 marcherait
         * — on relit dans l'ordre où l'on écrit — mais le fichier mentirait à qui l'ouvre.
         */
        private static final String[] NOMS_DISTORSION = {"k1", "k2", "p1", "p2", "k3"};

        /** Lue du fichier, ou déduite du seul champ déclaré — sans distorsion, donc. */
        static Camera lire() throws IOException {
            if (!Files.exists(FICHIER)) {
                double champ = Configurations.robotConfig().champHorizontalCameraDegres();
                double focale = (LARGEUR / 2.0) / Math.tan(Math.toRadians(champ / 2.0));
                return new Camera(focale, focale, LARGEUR / 2.0, HAUTEUR / 2.0, null);
            }
            Properties lu = new Properties();
            try (var flux = Files.newInputStream(FICHIER)) {
                lu.load(flux);
            }
            double[] k = new double[NOMS_DISTORSION.length];
            for (int i = 0; i < k.length; i++) {
                k[i] = Double.parseDouble(lu.getProperty(NOMS_DISTORSION[i], "0"));
            }
            return new Camera(Double.parseDouble(lu.getProperty("fx")), Double.parseDouble(lu.getProperty("fy")),
                    Double.parseDouble(lu.getProperty("cx")), Double.parseDouble(lu.getProperty("cy")), k);
        }

        static void ecrire(Mat matrice, Mat distorsion) throws IOException {
            double[] k = new MatOfDouble(distorsion).toArray();
            StringBuilder texte = new StringBuilder("# Ecrit par util.webcam.EtalonnageCamera. Relu par son mode --axes,\n"
                    + "# qui redresse l'image avant de mesurer : sans ces coefficients, la distorsion\n"
                    + "# du fisheye recadre se prend pour de la non-linearite mecanique.\n");
            texte.append("fx=").append(matrice.get(0, 0)[0]).append('\n');
            texte.append("fy=").append(matrice.get(1, 1)[0]).append('\n');
            texte.append("cx=").append(matrice.get(0, 2)[0]).append('\n');
            texte.append("cy=").append(matrice.get(1, 2)[0]).append('\n');
            for (int i = 0; i < NOMS_DISTORSION.length; i++) {
                texte.append(NOMS_DISTORSION[i]).append('=').append(i < k.length ? k[i] : 0).append('\n');
            }
            Files.createDirectories(FICHIER.getParent());
            Files.writeString(FICHIER, texte.toString());
            System.out.println("Étalonnage écrit dans " + FICHIER);
        }

        boolean etalonnee() {
            return distorsion != null;
        }

        double focale(boolean horizontal) {
            return horizontal ? fx : fy;
        }

        String description() {
            return etalonnee()
                    ? String.format(Locale.ROOT, "Caméra étalonnée : fx %.1f, fy %.1f, centre (%.1f ; %.1f), "
                            + "k1 %.4f — les images seront redressées.", fx, fy, cx, cy, distorsion[0])
                    : String.format(Locale.ROOT, "PAS D'ETALONNAGE (%s absent) : focale %.1f px déduite du "
                            + "champ déclaré, distorsion IGNOREE.%nLe résultat ne vaudra que près du centre "
                            + "de l'image. Lancer d'abord l'étalonnage au damier.", FICHIER, fx);
        }

        /** Redresse l'image, ou la rend telle quelle si la distorsion n'est pas connue. */
        Mat redresser(Mat image) {
            if (!etalonnee()) {
                return image;
            }
            Mat matrice = Mat.zeros(3, 3, CvType.CV_64F);
            matrice.put(0, 0, fx);
            matrice.put(1, 1, fy);
            matrice.put(0, 2, cx);
            matrice.put(1, 2, cy);
            matrice.put(2, 2, 1);
            Mat redressee = new Mat();
            Calib3d.undistort(image, redressee, matrice, new MatOfDouble(distorsion));
            return redressee;
        }
    }

    // --- La mesure des axes -----------------------------------------------------------------

    /** Décalages visés dans l'image, en pixels : assez grands pour être précis, assez petits pour
     * que les deux images se recouvrent largement. La corrélation de phase se dégrade vite quand le
     * recouvrement tombe : à 224 px sur une image haute de 480, elle a rendu zéro sans rien dire
     * (essai du 2026-09-06 sur l'inclinaison, course -6). */
    private static final double[] DECALAGES_VISES = {35, 60, 90};

    /** Course du sondage initial, avant de savoir ce que vaut l'axe. Volontairement petite. */
    private static final double COURSE_DE_SONDAGE = 1.5;

    /**
     * En deçà, le pic de corrélation n'est pas assez marqué et la course est écartée de la droite.
     * <p>
     * Relevé le 2026-09-06 : le panoramique, qui déplace l'image proprement, tenait entre 0,21 et
     * 0,62 ; l'inclinaison rendait 0,07 pour un déplacement pourtant franc de 46 px. Un axe qui
     * <b>bascule</b> la tête ne produit pas une translation pure — la caméra n'est pas sur l'axe de
     * rotation, et la perspective change — donc son pic est naturellement plus mou. Le seuil
     * n'écarte que ce qui est franchement douteux ; c'est la cohérence des six courses entre elles
     * qui reste le vrai juge.
     */
    private static final double CONFIANCE_MINIMALE = 0.04;

    /** En deçà, l'axe ne fait pas tourner la caméra du tout, et il n'y a rien à mesurer. */
    private static final double PIXELS_PAR_UNITE_MINIMAL = 0.5;

    /**
     * Mesure combien de degrés vaut une unité de commande, sur chaque axe du cou.
     * <p>
     * <b>Pourquoi ce mode existe.</b> Le rapport de chaque axe était tiré de fiches produit — trois
     * fiches, trois chiffres qui ne se recoupaient pas — ou déduit du suivi de visage, ce qui ne
     * marche pas : la cible bouge, et une ligne de log de {@code Regard} rapporte l'écart mesuré
     * <i>avant</i> que la correction précédente ait atterri. Le 2026-09-05, le même log a produit
     * 0,26, 3,4 puis 5,4 degrés par unité sur le même axe selon les paires de lignes choisies.
     * <p>
     * <b>La méthode.</b> Une image de référence, un déplacement de course connue, une seconde image.
     * La corrélation de phase donne le décalage au sous-pixel près, la focale étalonnée le convertit
     * en angle. La pente de l'angle en fonction de la course est le rapport cherché.
     * <p>
     * <b>Quatre précautions, toutes payées le 2026-09-06</b>, où la première version rendait entre
     * 1,05 et 2,38 degrés par unité sur le même axe selon la course :
     * <ul>
     *   <li><b>L'image est redressée</b> avant comparaison. L'objectif est un fisheye recadré, k1
     *       vaut −0,385 : un déplacement de 90 px près du bord ne vaut pas le même angle qu'au
     *       centre, et mélanger les deux fabrique de la dispersion pure. Les coefficients viennent
     *       de {@code $ROBOT_HOME/etalonnage/camera.properties}, écrit par l'étalonnage au damier.</li>
     *   <li><b>Une fenêtre de Hann</b> pondère les bords. Sans elle, la discontinuité au bord du
     *       cadre domine le spectre et le pic de corrélation dérive.</li>
     *   <li><b>Les courses sont choisies après un sondage</b>, pour que le décalage tombe entre 35 et
     *       90 px quel que soit le rapport de l'axe — au lieu de courses fixes qui saturent sur un
     *       axe vif et ne mesurent rien sur un axe mou.</li>
     *   <li><b>La référence est reprise avant chaque course</b>, et la confiance du pic est
     *       vérifiée. Une corrélation dégénérée rend un décalage nul, qui passait pour une mesure
     *       valide et tirait la droite vers zéro.</li>
     * </ul>
     * <p>
     * <b>Les yeux sont exclus.</b> Ils portent la caméra, mais leur rotation ne se lit pas comme une
     * translation dans l'image, et leur transmission est déjà connue par le relevé du STL.
     */
    private static void mesurerLesAxes() throws Exception {
        Camera camera = Camera.lire();
        System.out.println(camera.description());

        VideoCapture capture = WebcamUtils.getWebcamCaptureParNom(Configurations.robotConfig().webcamName());
        if (capture == null || !capture.isOpened()) {
            System.out.println("Pas de webcam trouvée. Le robot est-il bien arrêté ?");
            return;
        }
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, LARGEUR);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR);

        Mat fenetre = new Mat();
        Imgproc.createHanningWindow(fenetre, new Size(LARGEUR, HAUTEUR), CvType.CV_32F);

        try (PostureDeTravail posture = PostureDeTravail.prendre()) {
            System.out.println("Robot en posture de travail. NE BOUGE RIEN devant lui : c'est le");
            System.out.println("décor qui sert de repère, un décor qui change fausse tout.");
            for (Axe axe : posture.axes()) {
                if (!axe.nom().startsWith("oeil")) {
                    mesurerUnAxe(posture, axe, capture, camera, fenetre);
                }
            }
        } finally {
            capture.release();
        }
    }

    private static void mesurerUnAxe(PostureDeTravail posture, Axe axe, VideoCapture capture,
                                     Camera camera, Mat fenetre) throws InterruptedException {
        System.out.printf("%n--- %s (canal %d, travail %.1f, butées %.1f à %.1f) ---%n",
                axe.nom(), axe.canal(), axe.travail(), axe.min(), axe.max());
        boolean horizontal = "pan".equals(axe.nom());

        // Sondage : on ne sait pas encore ce que vaut l'axe, donc on ne peut pas choisir les
        // courses. Une petite course dit combien de pixels vaut une unité, et tout le reste en
        // découle. Sans lui, une course fixe sature sur un axe vif et ne mesure rien sur un mou.
        Mesure sondage = unAllerRetour(posture, axe, camera, capture, fenetre, COURSE_DE_SONDAGE, horizontal);
        double pixelsParUnite = Math.abs(sondage.pixels()) / COURSE_DE_SONDAGE;
        System.out.printf(Locale.ROOT, "sondage : %.1f px pour %.1f unité, soit ~%.1f px/unité (confiance %.2f)%n",
                sondage.pixels(), COURSE_DE_SONDAGE, pixelsParUnite, sondage.confiance());

        // Deux échecs très différents, qu'une seule phrase confondait le 2026-09-06 : un axe qui ne
        // déplace RIEN (le monter/descendre : 0,0 px, et une confiance de 0,98 qui affirme qu'il n'y
        // a bien aucun mouvement) n'est pas un axe dont la corrélation doute (l'inclinaison : 46 px
        // de déplacement franc, mais un pic mou). Le premier est un résultat, le second une
        // difficulté de mesure -- et l'abandon de l'axe ne se justifie que dans le premier cas.
        if (pixelsParUnite < PIXELS_PAR_UNITE_MINIMAL) {
            System.out.println("CET AXE NE FAIT PAS TOURNER LA CAMERA. Ce n'est pas un échec de mesure :");
            System.out.println("la corrélation est sûre d'elle et ne voit aucun déplacement. Un axe qui");
            System.out.println("translate la tête sans la pivoter donne exactement ça.");
            return;
        }
        if (sondage.confiance() < CONFIANCE_MINIMALE) {
            System.out.println("Pic de corrélation mou au sondage. On mesure quand même : c'est la");
            System.out.println("cohérence des six courses qui tranchera, pas cette seule valeur.");
        }
        System.out.printf("%8s %10s %10s %10s %10s %10s%n",
                "course", "dx (px)", "dy (px)", "confiance", "angle", "deg/unite");

        double sommeProduits = 0;
        double sommeCarres = 0;
        List<Double> courses = new ArrayList<>();
        for (double vise : DECALAGES_VISES) {
            courses.add(vise / pixelsParUnite);
            courses.add(-vise / pixelsParUnite);
        }

        for (double course : courses) {
            Mesure m = unAllerRetour(posture, axe, camera, capture, fenetre, course, horizontal);
            double angle = Math.toDegrees(Math.atan(m.pixels() / camera.focale(horizontal)));
            String alerte = "";
            if (Math.abs(m.courseReelle() - course) > 0.05) {
                alerte = "  <-- borné à la butée";
            } else if (m.confiance() < CONFIANCE_MINIMALE) {
                alerte = "  <-- pic de corrélation douteux, écarté";
            } else {
                sommeProduits += m.courseReelle() * angle;
                sommeCarres += m.courseReelle() * m.courseReelle();
            }
            System.out.printf(Locale.ROOT, "%8.2f %10.1f %10.1f %10.2f %9.2f° %10.3f%s%n",
                    m.courseReelle(), m.dx(), m.dy(), m.confiance(), angle,
                    m.courseReelle() == 0 ? 0 : angle / m.courseReelle(), alerte);
        }

        if (sommeCarres == 0) {
            System.out.println("Aucune course exploitable sur cet axe.");
            return;
        }
        // Droite passant par l'origine : une course nulle ne fait aucun angle, l'ordonnée à
        // l'origine n'aurait aucun sens physique et ne servirait qu'à absorber du bruit.
        double pente = Math.abs(sommeProduits / sommeCarres);
        System.out.printf(Locale.ROOT, "PENTE : %.3f degres par unite%n", pente);
        System.out.printf(Locale.ROOT,
                "  commande.par.degre.vu pour un gain de boucle de 0,9 : %.3f%n", 0.9 / pente);
        System.out.printf(Locale.ROOT,
                "  la plus petite consigne acceptée (1 unite) corrige %.1f degres%n", pente);
    }

    /** Ce qu'un aller-retour a rendu. */
    private record Mesure(double courseReelle, double dx, double dy, double pixels, double confiance) {
    }

    /**
     * Un aller-retour : référence à la posture de travail, déplacement, seconde image, retour.
     * <p>
     * La référence est reprise <b>à chaque fois</b>, et non une seule au départ. Le servo n'a pas de
     * retour de position : rien ne garantit qu'il revient exactement où il était, et le jeu de la
     * transmission suffit à décaler la référence de course en course. Une dérive lente passerait
     * alors pour une non-linéarité de l'axe.
     */
    private static Mesure unAllerRetour(PostureDeTravail posture, Axe axe, Camera camera,
                                        VideoCapture capture, Mat fenetre, double course,
                                        boolean horizontal) throws InterruptedException {
        posture.aller(axe, axe.travail());
        Thread.sleep((long) (PostureDeTravail.secondesDeTrajet(course) * 1000));
        Mat reference = camera.redresser(imageStable(capture));

        double cible = axe.borner(axe.travail() + course);
        posture.aller(axe, cible);
        Thread.sleep((long) (PostureDeTravail.secondesDeTrajet(course) * 1000));
        Mat vue = camera.redresser(imageStable(capture));

        double[] confiance = new double[1];
        Point decalage = Imgproc.phaseCorrelate(reference, vue, fenetre, confiance);
        return new Mesure(cible - axe.travail(), decalage.x, decalage.y,
                horizontal ? decalage.x : decalage.y, confiance[0]);
    }

    /**
     * Une image prise <b>après</b> avoir vidé le tampon de la webcam.
     * <p>
     * Sans ça on relit une image d'avant le mouvement : V4L2 garde quelques trames d'avance, et
     * c'est exactement le piège qui rendait les mesures tirées des logs de {@code Regard}
     * incohérentes. Rendue en niveaux de gris flottants, ce qu'exige la corrélation de phase.
     */
    private static Mat imageStable(VideoCapture capture) {
        Mat image = new Mat();
        for (int i = 0; i < 6; i++) {
            capture.read(image);
        }
        Mat gris = new Mat();
        Imgproc.cvtColor(image, gris, Imgproc.COLOR_BGR2GRAY);
        Mat flottant = new Mat();
        gris.convertTo(flottant, CvType.CV_32F);
        return flottant;
    }

    // --- La prise de vues -------------------------------------------------------------------

    private static Prise prendreLesVues(Size demande, int nombreVoulu) throws IOException {
        VideoCapture capture = WebcamUtils.getWebcamCaptureParNom(Configurations.robotConfig().webcamName());
        if (capture == null || !capture.isOpened()) {
            System.out.println("Pas de webcam trouvée. Le robot est-il bien arrêté ?");
            return new Prise(demande, List.of());
        }
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, LARGEUR);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR);

        Path dossier = Path.of(System.getenv(Constantes.ENV_VAR_ROBOT_HOME), "etalonnage",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        Files.createDirectories(dossier);
        System.out.println("Images gardées dans " + dossier);
        System.out.println("Entrée pour arrêter avant la fin. Incline le damier, et va dans les coins.");

        boolean[] arret = {false};
        Thread ecoute = new Thread(() -> {
            try {
                System.in.read();
            } catch (IOException ignore) {
                // On ne lit cette entrée que pour permettre d'arrêter tôt : rien à rattraper.
            }
            arret[0] = true;
        });
        ecoute.setDaemon(true);
        ecoute.start();

        List<MatOfPoint2f> gardees = new ArrayList<>();
        Size grille = null;
        long[] dernierMot = {0};
        boolean[] casesVues = new boolean[9];
        Mat image = new Mat();
        Mat gris = new Mat();

        while (!arret[0] && gardees.size() < nombreVoulu) {
            if (!capture.read(image) || image.empty()) {
                continue;
            }
            Imgproc.cvtColor(image, gris, Imgproc.COLOR_BGR2GRAY);
            if (grille == null) {
                grille = orientationQuiRepond(gris, demande);
                if (grille != null && grille != demande) {
                    System.out.printf("Damier trouvé en %d x %d : c'est cette orientation qui est retenue.%n",
                            (int) grille.width, (int) grille.height);
                }
            }
            MatOfPoint2f coins = grille == null ? null : trouverLeDamier(gris, grille);
            if (coins == null) {
                dire(dernierMot, "damier non vu — entier dans le cadre, et bien éclairé ?");
                continue;
            }
            double nettete = nettete(gris, coins);
            if (nettete < NETTETE_MINIMALE) {
                dire(dernierMot, String.format(Locale.ROOT,
                        "flou (netteté %.0f, il en faut %.0f) — immobilise le damier", nettete, NETTETE_MINIMALE));
                continue;
            }
            if (dejaVue(coins, gardees)) {
                dire(dernierMot, "pose déjà vue — déplace ou incline le damier");
                continue;
            }

            Imgcodecs.imwrite(dossier.resolve(String.format("vue-%02d.png", gardees.size() + 1)).toString(), image);
            gardees.add(coins);
            for (Point coin : lesQuatreCoins(coins, grille)) {
                casesVues[caseDe(coin)] = true;
            }

            System.out.print((char) 7);
            System.out.printf("%2d/%d gardée (netteté %.0f) — %s%n",
                    gardees.size(), nombreVoulu, nettete, cequiManque(casesVues));
            System.out.flush();
        }
        capture.release();
        return new Prise(grille == null ? demande : grille, gardees);
    }

    /**
     * Cherche le damier dans le sens annoncé, puis dans l'autre. Rend la grille qui a répondu, ou
     * {@code null} si aucune des deux n'a rien vu.
     */
    private static Size orientationQuiRepond(Mat gris, Size demande) {
        if (trouverLeDamier(gris, demande) != null) {
            return demande;
        }
        Size retournee = new Size(demande.height, demande.width);
        return trouverLeDamier(gris, retournee) != null ? retournee : null;
    }

    /**
     * Dit pourquoi l'image en cours est refusée, au plus une fois par seconde et demie. Sans ce
     * retour, l'opérateur est devant le robot les mains prises et ne voit qu'un terminal muet : il
     * ne peut pas savoir s'il doit s'immobiliser, se déplacer, ou allumer la lumière.
     */
    private static void dire(long[] dernierMot, String message) {
        long maintenant = System.currentTimeMillis();
        if (maintenant - dernierMot[0] < 1500) {
            return;
        }
        dernierMot[0] = maintenant;
        System.out.println("   ... " + message);
    }

    private static Prise relireLesImages(Path dossier, Size demande) throws IOException {
        List<MatOfPoint2f> vues = new ArrayList<>();
        Size grille = null;
        try (var flux = Files.list(dossier)) {
            List<Path> fichiers = flux
                    .filter(chemin -> chemin.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path fichier : fichiers) {
                Mat image = Imgcodecs.imread(fichier.toString(), Imgcodecs.IMREAD_GRAYSCALE);
                if (!image.empty() && grille == null) {
                    grille = orientationQuiRepond(image, demande);
                }
                MatOfPoint2f coins = image.empty() || grille == null
                        ? null : trouverLeDamier(image, grille);
                System.out.printf("%-20s %s%n", fichier.getFileName(),
                        coins == null ? "damier non trouvé" : "ok");
                if (coins != null) {
                    vues.add(coins);
                }
            }
        }
        return new Prise(grille == null ? demande : grille, vues);
    }

    // --- Le calcul --------------------------------------------------------------------------

    private static void calculer(List<MatOfPoint2f> vues, Size grille, double coteMm) {
        MatOfPoint3f modele = damierTheorique(grille, coteMm);
        List<Mat> objets = new ArrayList<>();
        List<Mat> images = new ArrayList<>();
        for (MatOfPoint2f vue : vues) {
            objets.add(modele);
            images.add(vue);
        }

        // Deux ajustements du même jeu d'images, et c'est la mesure qui décide lequel on garde.
        // Le troisième terme radial (k3) n'a de sens que sur un objectif franchement déformant ;
        // laissé libre sur un objectif sage, il absorbe du bruit et le paye sur la focale.
        // Comme il ajoute un degré de liberté, il fait TOUJOURS baisser l'erreur de reprojection :
        // la garder plus basse ne prouve donc rien, et choisir sur ce seul critère reviendrait à
        // toujours prendre k3. C'est l'AMPLEUR de la baisse qui distingue les deux cas.
        Mat figee = Mat.eye(3, 3, CvType.CV_64F);
        Mat distorsionFigee = new Mat();
        double rmsFige = Calib3d.calibrateCamera(objets, images, new Size(LARGEUR, HAUTEUR),
                figee, distorsionFigee, new ArrayList<>(), new ArrayList<>(), Calib3d.CALIB_FIX_K3);

        Mat libre = Mat.eye(3, 3, CvType.CV_64F);
        Mat distorsionLibre = new Mat();
        double rmsLibre = Calib3d.calibrateCamera(objets, images, new Size(LARGEUR, HAUTEUR),
                libre, distorsionLibre, new ArrayList<>(), new ArrayList<>(), 0);

        // Le 2026-09-06 a montré les deux régimes sur le même objectif : sur des images prises à
        // l'aveugle, libérer k3 ne changeait rien (0,426 px dans les deux cas) et le figer était
        // le bon choix ; sur des images prises en voyant l'écran, il faisait tomber l'erreur de
        // 0,286 à 0,135 px — le terme décrivait alors une vraie courbure, et le figer poussait la
        // focale de 462 à 475, soit 3 % de faux.
        boolean k3Utile = rmsLibre < GAIN_QUI_JUSTIFIE_K3 * rmsFige;
        Mat matrice = k3Utile ? libre : figee;
        Mat distorsion = k3Utile ? distorsionLibre : distorsionFigee;
        double rms = k3Utile ? rmsLibre : rmsFige;

        double fx = matrice.get(0, 0)[0];
        double fy = matrice.get(1, 1)[0];
        double cx = matrice.get(0, 2)[0];
        double cy = matrice.get(1, 2)[0];

        double champH = 2 * Math.toDegrees(Math.atan(LARGEUR / (2 * fx)));
        double champV = 2 * Math.toDegrees(Math.atan(HAUTEUR / (2 * fy)));

        double desaccord = 100 * Math.abs(libre.get(0, 0)[0] - figee.get(0, 0)[0]) / fx;

        System.out.println();
        System.out.printf(Locale.ROOT, "modèle retenu : %s%n", k3Utile
                ? "k3 LIBRE — le libérer fait tomber l'erreur de " + String.format(Locale.ROOT, "%.3f à %.3f px", rmsFige, rmsLibre)
                        + ", il décrit une vraie courbure"
                : "k3 figé — le libérer ne gagne rien (" + String.format(Locale.ROOT, "%.3f contre %.3f px", rmsLibre, rmsFige)
                        + "), il n'absorberait que du bruit");
        System.out.printf(Locale.ROOT, "   fx : %.2f px avec k3 figé, %.2f px avec k3 libre — %.1f %% d'écart%n",
                figee.get(0, 0)[0], libre.get(0, 0)[0], desaccord);
        System.out.printf("%d vues, erreur de reprojection %.3f px%s%n", vues.size(), rms,
                rms > 1.0 ? "   <-- au-dessus d'un pixel : à refaire, les poses manquent de variété" : "");
        System.out.printf("focale         fx = %.2f px   fy = %.2f px   (écart %.2f %%)%n",
                fx, fy, 100 * Math.abs(fx - fy) / fx);
        System.out.printf("point principal cx = %.2f px   cy = %.2f px%n", cx, cy);
        System.out.printf("champ          horizontal %.2f°   vertical %.2f°%n", champH, champV);

        StringBuilder coefficients = new StringBuilder();
        double[] k = new MatOfDouble(distorsion).toArray();
        for (double coefficient : k) {
            coefficients.append(String.format(Locale.ROOT, "%.4f  ", coefficient));
        }
        System.out.println("distorsion     " + coefficients);
        System.out.println();

        System.out.println("À écrire dans robot.properties (rechargé à chaud, sans redémarrage) :");
        System.out.printf(Locale.ROOT, "   robot.regard.camera.champ.horizontal.degres=%.2f%n", champH);
        System.out.printf(Locale.ROOT, "   (la valeur en place est %.1f)%n",
                Configurations.robotConfig().champHorizontalCameraDegres());
        System.out.println();

        // Les trois hypothèses que Regard tient pour acquises, et que seul le damier peut contredire.
        double biaisX = Math.toDegrees(Math.atan((cx - LARGEUR / 2.0) / fx));
        double biaisY = Math.toDegrees(Math.atan((cy - HAUTEUR / 2.0) / fy));
        System.out.printf(Locale.ROOT, "Décentrage : %+.1f px / %+.2f° en x, %+.1f px / %+.2f° en y.%n",
                cx - LARGEUR / 2.0, biaisX, cy - HAUTEUR / 2.0, biaisY);
        System.out.println(Math.max(Math.abs(biaisX), Math.abs(biaisY)) > 1.0
                ? "   Au-delà d'un degré : Regard suppose le centre de l'image, il vise à côté en permanence."
                : "   Sous le degré : l'hypothèse du centre tenue par Regard est acceptable.");
        System.out.println(Math.abs(fx - fy) / fx > 0.01
                ? "Pixels non carrés : Regard n'utilise qu'une focale pour les deux axes, à revoir."
                : "Pixels carrés : une seule focale pour les deux axes, comme le suppose Regard.");
        System.out.println(k.length > 0 && Math.abs(k[0]) > 0.15
                ? "Distorsion forte : le modèle sténopé de Regard est à questionner sur les bords."
                : "Distorsion modérée : le modèle sténopé de Regard tient.");

        // Gardé sur le robot, pas seulement affiché : c'est le mode --axes qui s'en sert pour
        // redresser l'image avant de mesurer, et un étalonnage qui ne survit pas à la séance
        // oblige à tout refaire à chaque question.
        try {
            Camera.ecrire(matrice, distorsion);
        } catch (IOException e) {
            System.err.println("Étalonnage non sauvegardé : " + e.getMessage());
        }
    }

    // --- Outils -----------------------------------------------------------------------------

    private static MatOfPoint2f trouverLeDamier(Mat gris, Size grille) {
        MatOfPoint2f coins = new MatOfPoint2f();
        boolean trouve = Calib3d.findChessboardCorners(gris, grille, coins,
                Calib3d.CALIB_CB_ADAPTIVE_THRESH | Calib3d.CALIB_CB_NORMALIZE_IMAGE | Calib3d.CALIB_CB_FAST_CHECK);
        if (!trouve) {
            return null;
        }
        int fenetre = demiFenetre(coins, grille);
        Imgproc.cornerSubPix(gris, coins, new Size(fenetre, fenetre), new Size(-1, -1), AFFINAGE);
        return coins;
    }

    /**
     * Demi-fenêtre d'affinage sous-pixel, déduite de l'écartement réel des coins.
     * <p>
     * Elle ne peut pas être fixe. Une fenêtre plus large que la moitié d'une case déborde sur le
     * coin voisin, et l'affinage tire chaque coin vers son voisin au lieu de le préciser : sur un
     * damier occupant un cinquième de l'image, une fenêtre de 11 px a fait passer l'erreur de
     * reprojection de 0,05 à 1,5 px — mesuré le 2026-09-05 sur des images de synthèse dont les
     * coins étaient connus à 0,04 px près. Le défaut est d'autant plus traître qu'il ne se voit
     * pas : la détection réussit, les images ont l'air bonnes, et seule la focale est fausse.
     */
    private static int demiFenetre(MatOfPoint2f coins, Size grille) {
        Point[] points = coins.toArray();
        int colonnes = (int) grille.width;
        double ecartMinimal = Double.MAX_VALUE;
        for (int i = 0; i < points.length; i++) {
            if ((i + 1) % colonnes != 0) {
                ecartMinimal = Math.min(ecartMinimal,
                        Math.hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y));
            }
            if (i + colonnes < points.length) {
                ecartMinimal = Math.min(ecartMinimal, Math.hypot(
                        points[i + colonnes].x - points[i].x, points[i + colonnes].y - points[i].y));
            }
        }
        return Math.max(2, Math.min(11, (int) (ecartMinimal / 2) - 1));
    }

    private static MatOfPoint3f damierTheorique(Size grille, double coteMm) {
        List<Point3> points = new ArrayList<>();
        for (int ligne = 0; ligne < (int) grille.height; ligne++) {
            for (int colonne = 0; colonne < (int) grille.width; colonne++) {
                points.add(new Point3(colonne * coteMm, ligne * coteMm, 0));
            }
        }
        return new MatOfPoint3f(points.toArray(new Point3[0]));
    }

    /** Variance du laplacien, calculée sur la seule emprise du damier. */
    private static double nettete(Mat gris, MatOfPoint2f coins) {
        Mat laplacien = new Mat();
        Imgproc.Laplacian(gris.submat(emprise(coins)), laplacien, CvType.CV_64F);
        MatOfDouble moyenne = new MatOfDouble();
        MatOfDouble ecartType = new MatOfDouble();
        Core.meanStdDev(laplacien, moyenne, ecartType);
        double sigma = ecartType.toArray()[0];
        return sigma * sigma;
    }

    private static Rect emprise(MatOfPoint2f coins) {
        double xMin = Double.MAX_VALUE;
        double xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        for (Point point : coins.toArray()) {
            xMin = Math.min(xMin, point.x);
            xMax = Math.max(xMax, point.x);
            yMin = Math.min(yMin, point.y);
            yMax = Math.max(yMax, point.y);
        }
        int x = (int) Math.max(0, xMin);
        int y = (int) Math.max(0, yMin);
        int largeur = (int) Math.min(LARGEUR - x, xMax - xMin + 1);
        int hauteur = (int) Math.min(HAUTEUR - y, yMax - yMin + 1);
        return new Rect(x, y, Math.max(1, largeur), Math.max(1, hauteur));
    }

    private static boolean dejaVue(MatOfPoint2f candidate, List<MatOfPoint2f> gardees) {
        Point[] nouveaux = candidate.toArray();
        for (MatOfPoint2f gardee : gardees) {
            Point[] anciens = gardee.toArray();
            double somme = 0;
            for (int i = 0; i < nouveaux.length; i++) {
                somme += Math.hypot(nouveaux[i].x - anciens[i].x, nouveaux[i].y - anciens[i].y);
            }
            if (somme / nouveaux.length < ECART_MINIMAL_PIXELS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Les quatre coins de la grille détectée.
     * <p>
     * C'est eux qui disent quelles zones de l'image portent de l'information, et non le centre du
     * damier : sur un damier de 350×500 mm qui remplit la moitié du cadre, le centre ne peut
     * <b>jamais</b> atteindre les cases d'angle sans que la cible sorte de l'image — l'outil
     * réclamait alors une pose impossible, et l'opérateur courait après (essais du 2026-09-05 :
     * « manque haut-droite, bas-droite » jusqu'au bout des vingt prises). Ses coins, eux, y vont.
     */
    private static Point[] lesQuatreCoins(MatOfPoint2f coins, Size grille) {
        Point[] points = coins.toArray();
        int colonnes = (int) grille.width;
        int lignes = (int) grille.height;
        return new Point[]{points[0], points[colonnes - 1],
                points[colonnes * (lignes - 1)], points[colonnes * lignes - 1]};
    }

    private static int caseDe(Point centre) {
        int colonne = Math.min(2, (int) (3 * centre.x / LARGEUR));
        int ligne = Math.min(2, (int) (3 * centre.y / HAUTEUR));
        return ligne * 3 + colonne;
    }

    private static String cequiManque(boolean[] casesVues) {
        String[] noms = {"haut-gauche", "haut", "haut-droite", "gauche", "centre", "droite",
                "bas-gauche", "bas", "bas-droite"};
        StringBuilder manquantes = new StringBuilder();
        for (int i = 0; i < casesVues.length; i++) {
            if (!casesVues[i]) {
                manquantes.append(manquantes.isEmpty() ? "" : ", ").append(noms[i]);
            }
        }
        return manquantes.isEmpty() ? "toutes les cases sont couvertes" : "manque : " + manquantes;
    }
}
