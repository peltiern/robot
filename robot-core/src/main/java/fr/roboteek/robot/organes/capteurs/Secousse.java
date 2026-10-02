package fr.roboteek.robot.organes.capteurs;

/**
 * Ce qui a secoué le robot depuis le dernier relevé : le plus grand écart, en g, entre
 * l'accélération mesurée et la gravité seule.
 * <p>
 * L'écart à 1 g et non les trois axes : immobile, l'accéléromètre lit la gravité, quelle que soit
 * l'inclinaison. Seule la norme est indépendante de la posture ; elle s'écarte de 1 g quand
 * quelque chose pousse le robot (choc, cahot, démarrage brusque) ou quand plus rien ne le porte
 * (chute libre, norme proche de 0).
 * <p>
 * Le <b>maximum</b> et non la dernière valeur : la carte mesure plus vite que la télémétrie ne
 * part, et un choc ne dure qu'un échantillon ou deux. Publier la dernière valeur le ferait
 * manquer une fois sur deux.
 * <p>
 * Écrit par le thread Phidget, relevé par celui de la télémétrie : d'où la synchronisation.
 */
public class Secousse {

    /**
     * Au-delà, ce n'est pas une mesure : l'accéléromètre plafonne à 8 g, et Phidget22 signale une
     * donnée indisponible par une valeur démesurée plutôt que par une exception.
     */
    private static final double ACCELERATION_MAX_PLAUSIBLE_G = 100;

    private double maximum;

    private boolean mesuree;

    /** Prend en compte un échantillon de l'accéléromètre, en g sur les trois axes. */
    public synchronized void noter(double[] acceleration) {
        double norme = Math.sqrt(acceleration[0] * acceleration[0]
                + acceleration[1] * acceleration[1]
                + acceleration[2] * acceleration[2]);
        if (!Double.isFinite(norme) || norme > ACCELERATION_MAX_PLAUSIBLE_G) {
            return;
        }
        double ecart = Math.abs(norme - 1);
        maximum = mesuree ? Math.max(maximum, ecart) : ecart;
        mesuree = true;
    }

    /**
     * La plus forte secousse depuis le relevé précédent, puis on repart de zéro. {@code null} si
     * aucun échantillon n'est arrivé entre-temps : « rien mesuré » n'est pas « rien senti ».
     */
    public synchronized Double relever() {
        if (!mesuree) {
            return null;
        }
        double releve = maximum;
        maximum = 0;
        mesuree = false;
        return releve;
    }
}
