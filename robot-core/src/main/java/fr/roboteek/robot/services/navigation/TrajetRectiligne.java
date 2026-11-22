package fr.roboteek.robot.services.navigation;

/**
 * Un trajet en ligne droite d'une distance donnée, arrêté sur les encodeurs : à chaque relevé, dit
 * quelle consigne donner à chaque chenille, ou que c'est fini.
 * <p>
 * Calcul pur, sans moteur ni horloge : l'organe lui passe le chemin fait par chaque chenille depuis
 * le départ et l'heure du relevé.
 * <p>
 * <b>Pourquoi corriger l'écart entre les chenilles.</b> À consigne égale, la droite tourne 4 à 8 %
 * plus vite que la gauche (mesuré le 2026-11-22) : sans correction, le robot finit 8° à gauche sur
 * 1,3 m. En ralentissant celle qui a pris de l'avance, il est arrivé à 1° sur 90 cm. C'est l'écart
 * de chemin qu'on annule, pas le cap : sans gyroscope, c'est tout ce que les encodeurs savent.
 */
public final class TrajetRectiligne {

    /** Distance restante sous laquelle on ralentit, pour ne pas dépasser la cible sur l'élan. */
    static final double APPROCHE_MM = 150;
    static final double VITESSE_APPROCHE = 0.25;

    /** Correction : 50 mm d'écart entre les chenilles retirent 0,1 à la plus avancée. */
    static final double CORRECTION_PAR_MM = 0.002;
    static final double CORRECTION_MAX = 0.15;

    /** Une seconde sans avancer d'un millimètre : chenille bloquée, ou encodeur qui ne compte plus. */
    static final long IMMOBILE_MAX_MS = 1000;

    /** Durée maximale : 5 s de marge, plus le trajet à 5 cm/s, la moitié de la vitesse observée. */
    static final long DUREE_MARGE_MS = 5000;
    static final double DUREE_MS_PAR_MM = 20;

    public enum Fin {
        ATTEINT,
        /** Le trajet dure trop : quelque chose freine le robot, ou les encodeurs mentent. */
        DUREE_DEPASSEE,
        IMMOBILE
    }

    /** Consignes de -1 à 1, positives en marche avant ; {@code fin} non nulle quand il faut s'arrêter. */
    public record Consigne(double gauche, double droite, Fin fin) {
        public boolean estFinie() {
            return fin != null;
        }
    }

    private final double distanceMm;
    private final double vitesse;
    /** 1 en marche avant, -1 en arrière : tout le calcul se fait dans le sens du trajet. */
    private final double sens;
    private final long dureeMaxMs;

    private Long debutMs;
    private double meilleurAvancementMm;
    private long dernierProgresMs;

    /**
     * @param distanceMm distance à parcourir, négative pour reculer
     * @param vitesse    vitesse de croisière, de 0 à 1
     */
    public TrajetRectiligne(double distanceMm, double vitesse) {
        this.distanceMm = Math.abs(distanceMm);
        this.vitesse = Math.min(1, Math.abs(vitesse));
        this.sens = Math.signum(distanceMm);
        this.dureeMaxMs = DUREE_MARGE_MS + (long) (this.distanceMm * DUREE_MS_PAR_MM);
    }

    /**
     * @param gaucheMm     chemin fait par la chenille gauche depuis le départ, positif en avant
     * @param droiteMm     idem pour la droite
     * @param maintenantMs heure du relevé ; le premier appel fixe le départ
     */
    public Consigne suivante(double gaucheMm, double droiteMm, long maintenantMs) {
        if (debutMs == null) {
            debutMs = maintenantMs;
            dernierProgresMs = maintenantMs;
        }
        double gauche = sens * gaucheMm;
        double droite = sens * droiteMm;
        double avancement = (gauche + droite) / 2;

        if (sens == 0 || avancement >= distanceMm) {
            return arret(Fin.ATTEINT);
        }
        if (maintenantMs - debutMs > dureeMaxMs) {
            return arret(Fin.DUREE_DEPASSEE);
        }
        if (avancement > meilleurAvancementMm + 1) {
            meilleurAvancementMm = avancement;
            dernierProgresMs = maintenantMs;
        } else if (maintenantMs - dernierProgresMs > IMMOBILE_MAX_MS) {
            return arret(Fin.IMMOBILE);
        }

        double base = distanceMm - avancement < APPROCHE_MM ? Math.min(vitesse, VITESSE_APPROCHE) : vitesse;
        double correction = Math.clamp((droite - gauche) * CORRECTION_PAR_MM, -CORRECTION_MAX, CORRECTION_MAX);
        return new Consigne(sens * Math.clamp(base + correction, 0, 1),
                sens * Math.clamp(base - correction, 0, 1), null);
    }

    public double distanceMm() {
        return sens * distanceMm;
    }

    private static Consigne arret(Fin fin) {
        return new Consigne(0, 0, fin);
    }
}
