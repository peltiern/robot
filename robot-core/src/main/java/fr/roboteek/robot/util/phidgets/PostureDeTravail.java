package fr.roboteek.robot.util.phidgets;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.actionneurs.transmission.Transmission;
import fr.roboteek.robot.organes.actionneurs.transmission.TransmissionOeil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Tient le robot dans sa posture de travail le temps d'un outil hors ligne, puis le repose.
 * <p>
 * <b>À quoi ça sert.</b> Un outil qui a besoin de la webcam exige que le robot soit arrêté — et un
 * robot arrêté a le nez piqué : {@code tilt} repose à 77 pour une posture de travail à 67, soit une
 * dizaine d'unités d'inclinaison, et les yeux reposent 8,5° plus bas que leur zéro. La caméra
 * regarde alors le sol à un mètre devant elle. Rien de ce qu'on vient mesurer n'en dépend, mais
 * travailler devant un robot qui ne vous voit pas est inutilement pénible.
 * <p>
 * <b>Les cinq axes sont engagés, pas seulement celui qui bouge.</b> Ce n'est pas du zèle : la tête
 * désengagée repose sur le corps, et c'est l'inclinaison — une fois engagée — qui la soulève de 77
 * à 67. Engager le seul axe qu'on veut déplacer laisserait les autres en appui.
 * <p>
 * <b>Le piège, payé une fois le 2026-08-30 sur {@code BancDebitServo}.</b> Position de départ et
 * position de repos ne sont pas la même chose. Il faut <i>engager</i> le servo là où le dernier
 * arrêt du robot l'a laissé — sa position de repos — puis le <i>conduire</i> à la posture de
 * travail. À l'engagement, le contrôleur ignore où se trouve physiquement le bras : engager
 * directement à la position de travail fait sauter le servo à la vitesse mécanique du matériel,
 * sans aucune rampe. C'est ce que {@code Cou} et {@code Yeux} évitent en passant leur position de
 * repos au constructeur de {@link PhidgetsServoMotor}.
 * <p>
 * <b>Et la posture est rendue avant de lâcher.</b> {@link #close()} ramène chaque axe à sa position
 * de repos avant de désengager, pour la même raison que {@code Cou.arreter()} : c'est là que la
 * mécanique tient toute seule, et c'est là que le prochain engagement s'attendra à la trouver.
 * Désengager ailleurs ferait tomber la tête.
 * <p>
 * Usage — le {@code try}-with-resources n'est pas une commodité, c'est ce qui garantit que le robot
 * est reposé même si l'outil échoue :
 * <pre>
 * try (PostureDeTravail posture = PostureDeTravail.prendre()) {
 *     // le robot regarde devant lui
 * }
 * </pre>
 */
public final class PostureDeTravail implements AutoCloseable {

    /**
     * Vitesse et accélération du trajet de mise en place, volontairement lentes : c'est un
     * déplacement de confort, pas une mesure, et il part d'une position que le contrôleur croit
     * déjà atteinte.
     */
    private static final double VITESSE = 20;

    private static final double ACCELERATION = 50;

    /** Temps laissé aux servos pour rejoindre une posture avant qu'on passe à la suite. */
    private static final long TRAJET_MS = 2500;

    /**
     * Un axe engagé, avec sa posture de travail et ses butées.
     *
     * @param travail position de la posture de travail du robot
     * @param repos   position ou le dernier arrêt l'a laissé, et où il sera redéposé
     */
    public record Axe(String nom, int canal, double travail, double repos, double min, double max) {

        /** Borne une position aux butées, marge comprise : rien ici n'a à éprouver la mécanique. */
        public double borner(double position) {
            return Math.max(min + MARGE_BUTEE, Math.min(max - MARGE_BUTEE, position));
        }
    }

    /** Marge gardée aux butées quand un outil commande un axe lui-même. */
    private static final double MARGE_BUTEE = 1;

    private final Map<Axe, PhidgetsServoMotor> moteurs = new LinkedHashMap<>();

    private PostureDeTravail(List<Axe> axes) throws InterruptedException {
        for (Axe axe : axes) {
            moteurs.put(axe, new PhidgetsServoMotor(axe.canal(), axe.travail(),
                    axe.min(), axe.max(), VITESSE, ACCELERATION, axe.repos()));
        }
        for (Map.Entry<Axe, PhidgetsServoMotor> entree : moteurs.entrySet()) {
            if (!attendreAttache(entree.getValue())) {
                fermerLesMoteurs();
                throw new IllegalStateException("Axe " + entree.getKey().nom() + " (canal "
                        + entree.getKey().canal() + ") non attaché au bout de 5 s. Le robot tourne-t-il encore ?");
            }
            entree.getValue().setSpeedRampingState(true);
        }
        moteurs.forEach((axe, moteur) -> moteur.setPositionCible(axe.travail(), VITESSE, ACCELERATION, false));
        Thread.sleep(TRAJET_MS);
    }

    /**
     * Engage les axes configurés et les amène à la posture de travail du robot.
     *
     * @throws IllegalStateException si un servo ne s'attache pas — signe le plus courant que le
     *                               robot tourne encore et tient les canaux
     */
    public static PostureDeTravail prendre() throws InterruptedException {
        return new PostureDeTravail(axesDeLaConfiguration(Configurations.phidgetsConfig()));
    }

    @Override
    public void close() throws InterruptedException {
        moteurs.forEach((axe, moteur) -> moteur.setPositionCible(axe.repos(), VITESSE, ACCELERATION, false));
        Thread.sleep(TRAJET_MS);
        fermerLesMoteurs();
    }

    /** Les axes effectivement engagés, dans l'ordre pan, tilt, updown, puis les deux yeux. */
    public List<Axe> axes() {
        return List.copyOf(moteurs.keySet());
    }

    /**
     * Conduit un axe à une position, bornée à ses butées. Ne rend pas la main à l'arrivée : le
     * servo n'a pas de retour de position, l'appelant attend le temps du trajet.
     *
     * @see #secondesDeTrajet(double)
     */
    public void aller(Axe axe, double position) {
        moteurs.get(axe).setPositionCible(axe.borner(position), VITESSE, ACCELERATION, false);
    }

    /** Temps du trajet pour une course donnée, rampe comprise, avec de la marge. */
    public static double secondesDeTrajet(double unites) {
        return Math.abs(unites) / VITESSE + 0.8;
    }

    private void fermerLesMoteurs() {
        moteurs.values().forEach(moteur -> {
            moteur.stop();
            moteur.setEngaged(false);
            moteur.close();
        });
        moteurs.clear();
    }

    private static List<Axe> axesDeLaConfiguration(PhidgetsConfig configuration) {
        List<Axe> axes = new ArrayList<>();
        ajouter(axes, "pan", configuration::neckLeftRightMotorIndex,
                configuration::neckLeftRightMotorInitialPosition,
                repos(configuration::neckLeftRightMotorRestPosition, configuration::neckLeftRightMotorInitialPosition),
                configuration::neckLeftRightMotorMinPosition, configuration::neckLeftRightMotorMaxPosition);
        ajouter(axes, "tilt", configuration::neckTiltMotorIndex,
                configuration::neckTiltMotorInitialPosition,
                repos(configuration::neckTiltMotorRestPosition, configuration::neckTiltMotorInitialPosition),
                configuration::neckTiltMotorMinPosition, configuration::neckTiltMotorMaxPosition);
        ajouter(axes, "updown", configuration::neckUpDownMotorIndex,
                configuration::neckUpDownMotorInitialPosition,
                repos(configuration::neckUpDownMotorRestPosition, configuration::neckUpDownMotorInitialPosition),
                configuration::neckUpDownMotorMinPosition, configuration::neckUpDownMotorMaxPosition);

        // Les yeux se commandent en degrés d'œil et la tringlerie n'est pas linéaire : la conversion
        // passe par TransmissionOeil, comme dans Yeux. Les deux servos sont montés en miroir, d'où
        // deux transmissions de sens opposés — et des butées qui s'échangent d'un œil à l'autre.
        double angleMin = configuration.eyePositionMin();
        double angleMax = configuration.eyePositionMax();
        DoubleSupplier reposAngle = repos(configuration::eyeRestPosition, () -> 0);
        double departGauche = configuration.eyeLeftMotorInitialPosition();
        Transmission oeilGauche = new TransmissionOeil(configuration.eyeLeftMotorZeroPosition(), -1);
        ajouter(axes, "oeil-gauche", configuration::eyeLeftMotorIndex,
                () -> departGauche, () -> oeilGauche.versMoteur(reposAngle.getAsDouble()),
                () -> oeilGauche.versMoteur(angleMax), () -> oeilGauche.versMoteur(angleMin));
        double departDroit = configuration.eyeRightMotorInitialPosition();
        Transmission oeilDroit = new TransmissionOeil(configuration.eyeRightMotorZeroPosition(), +1);
        ajouter(axes, "oeil-droit", configuration::eyeRightMotorIndex,
                () -> departDroit, () -> oeilDroit.versMoteur(reposAngle.getAsDouble()),
                () -> oeilDroit.versMoteur(angleMin), () -> oeilDroit.versMoteur(angleMax));
        return axes;
    }

    /**
     * Position de repos configurée, ou position de départ à défaut — les clés {@code .rest} sont
     * optionnelles, et {@code Cou} comme {@code Yeux} retombent sur la position initiale.
     */
    private static DoubleSupplier repos(Supplier<Double> configuree, DoubleSupplier parDefaut) {
        return () -> {
            Double valeur = configuree.get();
            return valeur != null ? valeur : parDefaut.getAsDouble();
        };
    }

    /**
     * Ajoute un axe. Les valeurs sont lues paresseusement : l'axe monter/descendre n'a pas de
     * valeur par défaut dans la configuration et, sur un robot où il n'est pas réglé, le lire lève.
     * Il est alors écarté plutôt que d'empêcher les autres de prendre leur posture.
     */
    private static void ajouter(List<Axe> axes, String nom, IntSupplier canal,
                                DoubleSupplier travail, DoubleSupplier repos,
                                DoubleSupplier min, DoubleSupplier max) {
        try {
            axes.add(new Axe(nom, canal.getAsInt(), travail.getAsDouble(), repos.getAsDouble(),
                    min.getAsDouble(), max.getAsDouble()));
        } catch (RuntimeException e) {
            System.err.println("Axe " + nom + " non configuré, laissé au repos");
        }
    }

    /** Le servo s'attache de façon asynchrone ; rien ne sert de le commander avant. */
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
}
