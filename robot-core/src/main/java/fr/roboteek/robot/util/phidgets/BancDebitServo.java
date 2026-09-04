package fr.roboteek.robot.util.phidgets;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.actionneurs.transmission.Transmission;
import fr.roboteek.robot.organes.actionneurs.transmission.TransmissionOeil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Banc de mesure du débit de consignes tenable sur les servos Phidgets.
 * <p>
 * Il décide d'un choix d'architecture pour les animations. Deux régimes sont possibles :
 * <ul>
 *   <li><b>pose</b> — une consigne par image-clé, la douceur venant de la rampe du contrôleur ;</li>
 *   <li><b>trajectoire</b> — la courbe est calculée en Java et envoyée par échantillons rapprochés,
 *       la rampe étant réglée raide pour ne pas la contrarier.</li>
 * </ul>
 * Le régime trajectoire suppose que le hub encaisse la cadence voulue pour tous les axes à la fois.
 * Rien dans la documentation ne le dit ; ça se mesure. Le banc envoie un aller-retour sinusoïdal à
 * cadence imposée et relève ce que l'écriture a réellement coûté.
 * <p>
 * <b>Verdict du 2026-08-29</b> : 18 ms par consigne de position, soit environ 55 écritures par
 * seconde pour l'ensemble du hub. Le coût est par écriture et non par tour, indépendant du nombre
 * d'axes menés de front. Le mode {@code async} a été retiré après essai : au-delà de 64 commandes
 * en attente, la bibliothèque jette les suivantes sans rien dire.
 * <p>
 * <b>La question qu'ouvre le mode {@code mono}</b> : ces 55 écritures par seconde sont-elles le
 * budget du lien USB, commun à tout, ou celui d'un canal ? La réponse décide de ce qu'il faudra
 * acheter le jour où le robot aura des bras. Si le lien limite, un second contrôleur n'apportera
 * rien et il faudra baisser la cadence ; si c'est le canal, répartir les servos sur plusieurs
 * contrôleurs paiera. Le mode écrit autant de fois par tour, mais toutes sur un seul canal : à
 * comparer avec {@code tous position}, qui écrit le même nombre de fois réparties sur cinq.
 * <p>
 * <b>Les butées et les index sont lus dans la configuration du robot</b>, jamais saisis à la main :
 * une butée retapée de travers, et le banc force un servo contre sa butée pendant dix secondes.
 * <p>
 * <b>Le robot doit être arrêté</b> : ses organes tiennent les mêmes canaux Phidget.
 * <pre>
 * java -Dloader.main=fr.roboteek.robot.util.phidgets.BancDebitServo -cp robot-core.jar \
 *      org.springframework.boot.loader.launch.PropertiesLauncher &lt;axe&gt; &lt;hertz&gt; &lt;secondes&gt;
 * </pre>
 * {@code <axe>} vaut {@code pan}, {@code tilt}, {@code updown}, {@code oeil-gauche},
 * {@code oeil-droit}, ou {@code tous} — c'est {@code tous} qui répond à la vraie question, les
 * cinq axes partageant un seul hub.
 */
public final class BancDebitServo {

    /** Marge gardée aux butées : le banc mesure un débit, il n'a pas à éprouver la mécanique. */
    private static final double MARGE_BUTEE = 3;

    /** Période du va-et-vient, en secondes. Assez lente pour que le servo suive vraiment. */
    private static final double PERIODE_BALAYAGE = 4;

    /**
     * Amplitude maximale du balayage, en degrés de part et d'autre du centre.
     * <p>
     * Ce qu'on mesure, c'est le coût d'une écriture, et il ne dépend pas de la distance parcourue :
     * il suffit que la position <b>change</b> à chaque échantillon. Sans ce plafond, le panoramique
     * balaierait ses 114° à près de 90 °/s pendant toute la mesure — une tête qui fouette pour un
     * chiffre qu'on obtient tout aussi bien en douceur.
     */
    private static final double AMPLITUDE_MAX = 10;

    /**
     * Vitesse et accélération du trajet qui amène le servo de sa position de repos à sa position
     * de départ, avant la mesure. Volontairement lentes : c'est un déplacement de mise en place,
     * pas une mesure, et il part d'une position que le contrôleur croit déjà atteinte.
     */
    private static final double VITESSE_APPROCHE = 20;

    private static final double ACCELERATION_APPROCHE = 50;

    /**
     * Nombre d'écritures par tour en mode {@code mono}. Cinq, pour se comparer terme à terme au
     * run {@code tous position}, qui en fait autant réparties sur cinq canaux.
     */
    private static final int ECRITURES_MONO_CANAL = 5;

    /**
     * En mode {@code mono}, écart minimal entre deux écritures successives. Aux deux extrémités du
     * balayage la vitesse s'annule : sans ce plancher, les cinq écritures d'un tour y porteraient
     * la même valeur, et rien ne dit que la bibliothèque ne les écarterait pas comme redondantes —
     * on mesurerait alors un débit flatteur sur des écritures qui ne sont jamais parties.
     */
    private static final double PAS_MINIMAL_MONO = 0.05;

    /**
     * Sens de balayage des trois axes du cou. La position logique vaut {@code init - position
     * moteur} (cf. {@code Cou}) : faire décroître la position moteur relève la tête et l'écarte
     * du corps, ce qui est le seul sens sûr sur un banc qui tourne sans surveillance.
     */
    private static final int VERS_LE_HAUT = -1;

    /**
     * Un axe tel que le banc doit le piloter, une fois la configuration lue.
     *
     * @param positionDepart     position de départ du robot, autour de laquelle balayer
     * @param positionEngagement position <b>physique</b> probable du servo, celle où le dernier
     *                           arrêt du robot a laissé la tête — voir {@link #ajouter}
     * @param positionBasse  butée basse, marge prise
     * @param positionHaute  butée haute, marge prise
     */
    private record AxeMesure(String nom, int canal, double positionDepart, double positionEngagement,
                             double positionBasse, double positionHaute, int sens) {

        /**
         * Le balayage part de la <b>position de départ du robot</b> et ne va que d'un seul côté.
         * <p>
         * Il était centré sur elle, et c'était déjà mieux que de viser le milieu de la course —
         * le monter/descendre part à 140 pour une course de 80 à 150, son milieu est 25° sous la
         * posture normale et la tête y frotte le corps. Mais le centrer ne suffisait pas : sur
         * cet axe et sur l'inclinaison, la tête repose <b>déjà</b> presque sur le corps au repos,
         * si bien que la moitié descendante du va-et-vient frottait encore. D'où le sens imposé :
         * pour les trois axes du cou, la position moteur décroissante relève la tête
         * ({@code position logique = init - position moteur}, cf. {@code Cou}), donc l'éloigne du
         * corps. Les yeux ne peuvent rien toucher : ils prennent leur côté le plus large.
         */
        double centre() {
            return positionDepart;
        }

        /** Bornée par la seule butée du côté balayé. */
        double amplitude() {
            double marge = sens < 0 ? positionDepart - positionBasse : positionHaute - positionDepart;
            return Math.min(AMPLITUDE_MAX, marge);
        }

        /** Extrémité du balayage, la position de départ étant l'autre. */
        double extremite() {
            return positionDepart + sens * amplitude();
        }
    }

    private BancDebitServo() {
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 3 || args.length > 5) {
            System.err.println("Usage : BancDebitServo <axe> <hertz> <secondes> [mode] [position-reelle]");
            System.err.println("  <axe>  : pan | tilt | updown | oeil-gauche | oeil-droit | tous");
            System.err.println("  <mode> : simulation  annonce le balayage prévu, sans toucher aux servos");
            System.err.println("           complet     vitesse + position à chaque échantillon (défaut)");
            System.err.println("           position    vitesse réglée une fois, position seule ensuite");
            System.err.println("           mono        " + ECRITURES_MONO_CANAL + " positions par tour sur UN seul canal");
            System.err.println("  <position-reelle> : position PHYSIQUE observée du bras, en degrés moteur.");
            System.err.println("      À donner quand la tête n'est pas là où la configuration le croit ;");
            System.err.println("      le banc y engage le servo, puis rejoint la position de départ en douceur.");
            System.err.println("      Un seul axe à la fois — une valeur ne peut pas valoir pour cinq.");
            System.err.println("Exemple (les cinq axes, 30 Hz, 10 s) : tous 30 10 position");
            System.err.println("Le lien limite-t-il, ou le canal ? Comparer : tous 30 10 position");
            System.err.println("                                    et : pan 30 10 mono");
            System.exit(1);
        }
        String axeDemande = args[0];
        double hertz = Double.parseDouble(args[1]);
        double secondes = Double.parseDouble(args[2]);
        // >= 4 et non == 4 : avec un cinquième argument, un test d'égalité laisserait passer le
        // mode en « complet » sans rien dire — « simulation » ignoré, et le banc part pour de vrai.
        String mode = args.length >= 4 ? args[3] : "complet";
        if (!List.of("simulation", "complet", "position", "mono").contains(mode)) {
            System.err.println("Mode inconnu : " + mode);
            System.exit(1);
        }
        boolean simulation = "simulation".equals(mode);
        Double positionReelle = args.length == 5 ? Double.parseDouble(args[4]) : null;

        Map<String, AxeMesure> axes = axesDeLaConfiguration(Configurations.phidgetsConfig());
        List<AxeMesure> aMesurer = new ArrayList<>();
        if ("tous".equals(axeDemande)) {
            aMesurer.addAll(axes.values());
        } else if (axes.containsKey(axeDemande)) {
            aMesurer.add(axes.get(axeDemande));
        } else {
            System.err.println("Axe inconnu : " + axeDemande + " — connus : " + axes.keySet());
            System.exit(1);
        }

        if (positionReelle != null) {
            if (aMesurer.size() != 1) {
                System.err.println("Une position réelle ne vaut que pour un axe : donner pan, tilt, updown ou un oeil.");
                System.exit(1);
            }
            // Ce que dit l'oeil l'emporte sur ce que dit le fichier : la configuration décrit la
            // cible visée avant le désengagement, pas forcément le point où la tête s'est posée.
            AxeMesure axe = aMesurer.getFirst();
            AxeMesure corrige = new AxeMesure(axe.nom(), axe.canal(), axe.positionDepart(), positionReelle,
                    axe.positionBasse(), axe.positionHaute(), axe.sens());
            aMesurer.set(0, corrige);
            axes.put(corrige.nom(), corrige);
        }

        if ("mono".equals(mode) && aMesurer.size() != 1) {
            // Tout l'intérêt du mode est de concentrer les écritures sur un canal ; les répartir
            // reviendrait à refaire le mode « position » en plus lent.
            System.err.println("Le mode mono se passe sur un seul axe : donner pan, tilt, updown ou un oeil.");
            System.exit(1);
        }

        for (AxeMesure axe : aMesurer) {
            if (axe.amplitude() <= 0) {
                System.err.printf("Axe %s : sa position de départ (%.1f) ne laisse aucune course entre %.1f et %.1f.%n",
                        axe.nom(), axe.positionDepart(), axe.positionBasse(), axe.positionHaute());
                System.exit(1);
            }
        }

        // TOUS les axes sont engagés et amenés à la posture de travail, même ceux qu'on ne mesure
        // pas. Ce n'est pas du zèle : mesurer le seul panoramique laissait l'inclinaison et le
        // monter/descendre désengagés, donc la tête posée sur le corps — et le panoramique la
        // faisait tourner en appui dessus. C'est l'inclinaison, engagée, qui la soulève de 77 à 67.
        // Les axes seulement tenus ne reçoivent aucune écriture pendant la mesure, donc ne pèsent
        // rien sur le débit ; ils remettent au passage les runs sur un pied d'égalité, le même
        // nombre de canaux étant engagé qu'on en mesure un ou cinq.
        List<AxeMesure> aTenir = new ArrayList<>(axes.values());

        System.out.printf("Banc : %d axe(s) mesuré(s) sur %d tenu(s), %.0f Hz pendant %.0f s, mode %s%n",
                aMesurer.size(), aTenir.size(), hertz, secondes, mode);
        for (AxeMesure axe : aTenir) {
            if (aMesurer.contains(axe)) {
                System.out.printf("  %-12s canal %d, engagé à %.1f°, balayage %.1f° → %.1f° et retour (butées %.1f° à %.1f°)%n",
                        axe.nom(), axe.canal(), axe.positionEngagement(), axe.positionDepart(), axe.extremite(),
                        axe.positionBasse(), axe.positionHaute());
            } else {
                System.out.printf("  %-12s canal %d, engagé à %.1f°, tenu à %.1f° (non mesuré)%n",
                        axe.nom(), axe.canal(), axe.positionEngagement(), axe.positionDepart());
            }
        }
        if (simulation) {
            System.out.println();
            System.out.println("Simulation : aucun servo n'a été ouvert. Vérifier que chaque balayage");
            System.out.println("reste dans la posture normale du robot, et que la position d'engagement");
            System.out.println("est bien là où le dernier arrêt a laissé la tête.");
            System.exit(0);
        }

        // Vitesse et accélération volontairement généreuses : c'est le débit d'écriture qu'on
        // mesure, pas la capacité du servo à suivre. Elles sont écrêtées aux bornes du matériel.
        Map<AxeMesure, PhidgetsServoMotor> moteurs = new LinkedHashMap<>();
        for (AxeMesure axe : aTenir) {
            moteurs.put(axe, new PhidgetsServoMotor(axe.canal(), axe.centre(),
                    axe.positionBasse(), axe.positionHaute(), 1000, 10000, axe.positionEngagement()));
        }
        for (Map.Entry<AxeMesure, PhidgetsServoMotor> entree : moteurs.entrySet()) {
            if (!attendreAttache(entree.getValue())) {
                System.err.println("Axe " + entree.getKey().nom() + " (canal " + entree.getKey().canal()
                        + ") non attaché au bout de 5 s. Le robot tourne-t-il encore ?");
                System.exit(1);
            }
            entree.getValue().setSpeedRampingState(true);
        }
        // Rejoindre la position de départ, en douceur, AVANT de mesurer quoi que ce soit.
        // Il n'y avait ici qu'une attente de deux secondes et un commentaire qui prétendait que
        // le trajet se faisait : rien ne le commandait. C'est le premier échantillon du balayage
        // qui l'accomplissait, à la vitesse de mesure — 10° d'un coup sur l'inclinaison, qui
        // repose à 77 pour une posture de travail à 67, et 7° sur chaque oeil.
        double approcheLaPlusLongue = 0;
        for (Map.Entry<AxeMesure, PhidgetsServoMotor> entree : moteurs.entrySet()) {
            AxeMesure axe = entree.getKey();
            entree.getValue().setPositionCible(axe.centre(), VITESSE_APPROCHE, ACCELERATION_APPROCHE, false);
            approcheLaPlusLongue = Math.max(approcheLaPlusLongue,
                    Math.abs(axe.centre() - axe.positionEngagement()));
        }
        // Attente calculée sur le trajet le plus long, plutôt qu'un délai fixe pris au jugé.
        Thread.sleep(500 + (long) (1000 * approcheLaPlusLongue / VITESSE_APPROCHE));

        if (!"complet".equals(mode)) {
            // Vitesse réglée une fois, après l'approche : dans ces modes, seule la position est
            // réécrite ensuite. Vitesse haute, sinon le servo n'aurait pas le temps de rallier
            // chaque échantillon et la mesure porterait sur autre chose que le débit d'écriture.
            aMesurer.forEach(axe -> moteurs.get(axe).setVitesse(500d));
        }

        int nombreTicks = (int) (secondes * hertz);
        long periodeNanos = (long) (1_000_000_000L / hertz);
        // Un tick = une consigne par axe, exactement ce que le lecteur d'animation enverra.
        long[] dureesTick = new long[nombreTicks];
        Map<AxeMesure, Double> positionsPrecedentes = new LinkedHashMap<>();
        aMesurer.forEach(axe -> positionsPrecedentes.put(axe, axe.centre()));

        long debut = System.nanoTime();
        for (int i = 0; i < nombreTicks; i++) {
            double instantSecondes = i / hertz;
            long avant = System.nanoTime();
            for (AxeMesure axe : aMesurer) {
                // (1 - cos)/2 plutôt qu'un sinus : le balayage part de la position de départ
                // et n'en franchit jamais le côté opposé, celui où la tête toucherait le corps.
                double avancement = (1 - Math.cos(2 * Math.PI * instantSecondes / PERIODE_BALAYAGE)) / 2;
                double position = axe.centre() + axe.sens() * axe.amplitude() * avancement;
                PhidgetsServoMotor moteur = moteurs.get(axe);
                switch (mode) {
                    // Vitesse recalculée pour arriver pile au prochain échantillon : deux écritures
                    // par axe, la vitesse changeant à chaque fois.
                    case "complet" -> {
                        double vitesse = Math.abs(position - positionsPrecedentes.get(axe)) * hertz;
                        moteur.setPositionCible(position, Math.max(vitesse, 1), null, false);
                    }
                    // Une seule écriture par axe : la vitesse a été réglée une fois pour toutes.
                    case "position" -> moteur.setPositionCible(position);
                    // Le même nombre d'écritures qu'un tour à cinq axes, mais toutes sur celui-ci.
                    case "mono" -> ecrireSurUnSeulCanal(moteur, positionsPrecedentes.get(axe), position);
                    default -> throw new IllegalStateException(mode);
                }
                positionsPrecedentes.put(axe, position);
            }
            dureesTick[i] = System.nanoTime() - avant;

            long prochain = debut + (long) (i + 1) * periodeNanos;
            long attente = prochain - System.nanoTime();
            if (attente > 0) {
                Thread.sleep(attente / 1_000_000L, (int) (attente % 1_000_000L));
            }
        }
        long duree = System.nanoTime() - debut;

        // Retour à la position de repos, et non à celle de départ : c'est là que le servo va être
        // désengagé, donc là qu'il faut que la mécanique tienne toute seule — même raison que
        // dans Cou.arreter(). Et c'est là que le prochain engagement s'attendra à le trouver.
        moteurs.forEach((axe, moteur) -> moteur.setPositionCible(axe.positionEngagement(), null, null, false));
        Thread.sleep(1500);
        moteurs.values().forEach(moteur -> {
            moteur.stop();
            moteur.setEngaged(false);
            moteur.close();
        });

        // Ce qu'on compare d'un run à l'autre, ce n'est pas le nombre d'axes mais le nombre
        // d'écritures par tour : c'est lui qui porte le coût, l'axe n'y est qu'un prétexte.
        int ecrituresParTour = switch (mode) {
            case "complet" -> 2 * aMesurer.size();
            case "mono" -> ECRITURES_MONO_CANAL;
            default -> aMesurer.size();
        };
        afficherLeVerdict(dureesTick, hertz, duree, aMesurer.size(), ecrituresParTour);
        System.exit(0);
    }

    private static void afficherLeVerdict(long[] dureesTick, double hertz, long duree,
                                          int nombreAxes, int ecrituresParTour) {
        long[] triees = dureesTick.clone();
        Arrays.sort(triees);
        double budgetMs = 1000.0 / hertz;
        double medianeMs = milli(triees[triees.length / 2]);

        System.out.println();
        System.out.printf("Cadence demandée      : %.1f Hz (%.1f ms par tick)%n", hertz, budgetMs);
        System.out.printf("Cadence tenue         : %.1f Hz (%d ticks en %.2f s)%n",
                dureesTick.length / (duree / 1e9), dureesTick.length, duree / 1e9);
        System.out.printf("Tick médian           : %.2f ms  (%d axe(s), %d écriture(s) par tour)%n",
                medianeMs, nombreAxes, ecrituresParTour);
        System.out.printf("Coût par écriture     : %.2f ms  (soit %.0f écritures par seconde)%n",
                medianeMs / ecrituresParTour, 1000 * ecrituresParTour / medianeMs);
        System.out.printf("Tick 95e centile      : %.2f ms%n", milli(triees[(int) (triees.length * 0.95)]));
        System.out.printf("Tick maximal          : %.2f ms%n", milli(triees[triees.length - 1]));
        System.out.println();

        long ticksTropLongs = Arrays.stream(dureesTick).filter(d -> milli(d) > budgetMs).count();
        System.out.printf("Ticks dépassant leur budget : %d sur %d (%.1f %%)%n",
                ticksTropLongs, dureesTick.length, 100.0 * ticksTropLongs / dureesTick.length);
        if (medianeMs < budgetMs / 2) {
            System.out.println("VERDICT : large. Cette cadence passe, et il reste de la marge.");
        } else if (medianeMs < budgetMs) {
            System.out.println("VERDICT : ça passe, mais sans marge — essayer une cadence plus basse.");
        } else {
            System.out.println("VERDICT : le hub ne suit pas à cette cadence. Refaire plus lentement.");
        }
    }

    /**
     * Les axes tels que le robot les connaît. Les positions sont des positions <b>moteur</b> ;
     * pour les yeux, la conversion depuis le repère relatif est celle de {@code Yeux} — et elle
     * est inversée d'un œil à l'autre, les deux servos étant montés en miroir.
     */
    private static Map<String, AxeMesure> axesDeLaConfiguration(PhidgetsConfig configuration) {
        Map<String, AxeMesure> axes = new LinkedHashMap<>();
        ajouter(axes, "pan", configuration::neckLeftRightMotorIndex, VERS_LE_HAUT,
                configuration::neckLeftRightMotorInitialPosition,
                repos(configuration::neckLeftRightMotorRestPosition, configuration::neckLeftRightMotorInitialPosition),
                configuration::neckLeftRightMotorMinPosition, configuration::neckLeftRightMotorMaxPosition);
        ajouter(axes, "tilt", configuration::neckTiltMotorIndex, VERS_LE_HAUT,
                configuration::neckTiltMotorInitialPosition,
                repos(configuration::neckTiltMotorRestPosition, configuration::neckTiltMotorInitialPosition),
                configuration::neckTiltMotorMinPosition, configuration::neckTiltMotorMaxPosition);
        ajouter(axes, "updown", configuration::neckUpDownMotorIndex, VERS_LE_HAUT,
                configuration::neckUpDownMotorInitialPosition,
                repos(configuration::neckUpDownMotorRestPosition, configuration::neckUpDownMotorInitialPosition),
                configuration::neckUpDownMotorMinPosition, configuration::neckUpDownMotorMaxPosition);

        // Les yeux se règlent en degrés d'oeil, et la tringlerie n'est pas linéaire : la conversion
        // passe par TransmissionOeil, comme dans Yeux. Les deux servos sont montés en miroir, d'où
        // les deux transmissions de sens opposés. Position de départ : le zéro.
        double angleMin = configuration.eyePositionMin();
        double angleMax = configuration.eyePositionMax();
        DoubleSupplier reposAngle = repos(configuration::eyeRestPosition, () -> 0);
        double zeroGauche = configuration.eyeLeftMotorPositionZero();
        Transmission oeilGauche = new TransmissionOeil(zeroGauche, -1);
        ajouter(axes, "oeil-gauche", configuration::eyeLeftMotorIndex, -1,
                () -> zeroGauche, () -> oeilGauche.versMoteur(reposAngle.getAsDouble()),
                () -> oeilGauche.versMoteur(angleMax), () -> oeilGauche.versMoteur(angleMin));
        double zeroDroit = configuration.eyeRightMotorPositionZero();
        Transmission oeilDroit = new TransmissionOeil(zeroDroit, +1);
        ajouter(axes, "oeil-droit", configuration::eyeRightMotorIndex, +1,
                () -> zeroDroit, () -> oeilDroit.versMoteur(reposAngle.getAsDouble()),
                () -> oeilDroit.versMoteur(angleMin), () -> oeilDroit.versMoteur(angleMax));
        return axes;
    }

    /**
     * Position de repos configurée, ou la position de départ à défaut — les clés {@code .rest}
     * sont optionnelles, et {@code Cou} comme {@code Yeux} retombent sur la position initiale.
     */
    private static DoubleSupplier repos(Supplier<Double> configuree, DoubleSupplier parDefaut) {
        return () -> {
            Double valeur = configuree.get();
            return valeur != null ? valeur : parDefaut.getAsDouble();
        };
    }

    /**
     * Ajoute un axe, marges prises. Les valeurs sont lues paresseusement : l'axe monter/descendre
     * n'a pas de valeur par défaut dans la configuration et, sur un robot où il n'est pas réglé,
     * le lire lève. Il est alors écarté du banc plutôt que d'empêcher de mesurer les autres.
     * <p>
     * <b>Position de départ et position de repos ne sont pas la même chose</b>, et confondre les
     * deux coûterait un servo. Le banc <i>balaie</i> autour de la position de départ — la posture
     * de travail du robot — mais il doit <i>engager</i> le servo à sa position de repos, celle où
     * le dernier arrêt a laissé la tête ({@code Cou.arreter()} l'y amène avant de désengager).
     * Engager ailleurs ferait sauter le servo à la vitesse mécanique du matériel : à l'engagement
     * le contrôleur ignore où se trouve physiquement le bras, donc aucune rampe ne s'applique.
     * C'est exactement ce que {@code Cou} et {@code Yeux} évitent en passant leur position de
     * repos au constructeur, et ce que le banc omettait.
     * <p>
     * L'oubli n'était pas théorique : le 2026-08-30, le banc engageait l'inclinaison à 67 alors
     * que le robot la laisse à 77 au repos, et chaque œil à son zéro alors qu'ils reposent 7° plus
     * bas. Trois servos qui sautaient de 7 à 10° à la vitesse du matériel au démarrage de chaque
     * mesure. Ne pas se fier au {@code robot.properties} du poste de développement pour juger si
     * les clés {@code .rest} sont réglées : celui du robot ne lui est pas identique.
     */
    private static void ajouter(Map<String, AxeMesure> axes, String nom, IntSupplier canal, int sens,
                                DoubleSupplier depart, DoubleSupplier repos,
                                DoubleSupplier min, DoubleSupplier max) {
        try {
            axes.put(nom, new AxeMesure(nom, canal.getAsInt(), depart.getAsDouble(), repos.getAsDouble(),
                    min.getAsDouble() + MARGE_BUTEE, max.getAsDouble() - MARGE_BUTEE, sens));
        } catch (RuntimeException e) {
            System.err.println("Axe " + nom + " non configuré, écarté du banc");
        }
    }

    /**
     * Découpe le trajet du tour en {@link #ECRITURES_MONO_CANAL} consignes envoyées au même canal.
     * <p>
     * Découper plutôt que répéter la même valeur : chaque écriture porte ainsi une position
     * réellement différente, comme dans un tour à cinq axes où chacune s'adresse à un canal
     * différent. Le mouvement reste exactement le balayage prévu — c'est le même trajet, mesuré
     * plus finement. Aux extrémités, où la vitesse s'annule, un pas minimal alterné garde les
     * écritures distinctes ; il vaut 0,05°, bien en deçà de ce qu'un servo RC sait résoudre, donc
     * invisible sur la mécanique.
     */
    private static void ecrireSurUnSeulCanal(PhidgetsServoMotor moteur, double depuis, double vers) {
        double pas = (vers - depuis) / ECRITURES_MONO_CANAL;
        for (int k = 1; k <= ECRITURES_MONO_CANAL; k++) {
            double position = Math.abs(pas) >= PAS_MINIMAL_MONO
                    ? depuis + k * pas
                    : vers + (k % 2 == 0 ? PAS_MINIMAL_MONO : -PAS_MINIMAL_MONO);
            moteur.setPositionCible(position);
        }
    }

    /** Le servo s'attache de façon asynchrone ; rien ne sert de mesurer avant. */
    private static boolean attendreAttache(PhidgetsServoMotor moteur) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            if (moteur.getVitesseMaxMoteur() != null) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }

    private static double milli(long nanos) {
        return nanos / 1e6;
    }
}
