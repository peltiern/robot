package fr.roboteek.robot.securite;

/**
 * Organe qui donne signe de vie. Implémenté par les organes dont on veut savoir s'ils vivent
 * encore — pour l'afficher (pastilles d'état) et, pour ceux qui touchent au mouvement, pour couper
 * les moteurs quand ils se taisent (voir {@link WatchDog}).
 * <p>
 * <b>Pourquoi pas {@code isRunning()}.</b> C'est précisément le drapeau qui ment : il est mis à
 * {@code true} au démarrage et n'est jamais remis à {@code false} quand la boucle de l'organe meurt
 * sur une exception. Un organe mort se déclare donc « démarré » jusqu'à l'arrêt du robot. D'où le
 * battement : une preuve de vie <b>datée</b>, écrite par le travail réel de l'organe (un tour de
 * boucle abouti, une lecture de position qui a répondu) et pas par son cycle de vie Spring.
 * <p>
 * <b>Le registre tire, l'organe ne pousse pas.</b> Un organe n'appelle personne : il se contente de
 * tenir à jour son horodatage, que {@link RegistreSante} vient lire. Aucun organe ne dépend donc du
 * watchdog ni du registre — même principe que pour l'arrêt d'urgence, où les organes ne
 * connaissent que l'évènement. Le battement lui-même est une simple écriture de {@code long}
 * volatile : à 60 Hz sur la manette, passer par le bus d'évènements serait hors de question.
 * <p>
 * L'implémentation par défaut du battement est dans {@code AbstractOrgane} ; seuls les organes qui
 * n'en héritent pas (la manette) portent leur propre horodatage.
 */
public interface OrganeSurveille {

    /**
     * Identifiant stable, même convention et mêmes valeurs que la ressource REST
     * {@code /api/organes} ({@code cou}, {@code yeux}, {@code chenilles}, …).
     */
    String idOrgane();

    /** Libellé lisible, destiné à l'affichage. */
    String libelleOrgane();

    /**
     * Agit sur le monde, ou l'observe ? L'interface s'en sert pour séparer les deux familles,
     * qu'on ne lit pas de la même façon : un actionneur muet inquiète, un capteur muet informe.
     */
    NatureOrgane nature();

    /**
     * L'organe est-il censé battre en ce moment ? {@code false} = éteint volontairement (organe
     * désactivé par configuration, matériel absent, robot pas encore démarré ou en cours d'arrêt).
     * Un organe hors service n'est jamais jugé en panne, et n'est jamais un motif d'arrêt d'urgence.
     */
    boolean enService();

    /**
     * Horodatage du dernier battement ({@link System#currentTimeMillis()}), ou {@code 0} si
     * l'organe n'a encore jamais battu.
     */
    long dernierBattement();

    /**
     * L'organe provoque-t-il ou exécute-t-il un mouvement ? Seuls ceux-là sont surveillés par le
     * {@link WatchDog} — le périmètre est volontairement étroit : un capteur muet mérite une
     * ligne de journal, pas la coupure des moteurs.
     */
    default boolean provoqueUnMouvement() {
        return false;
    }

    /**
     * Quelque chose bouge-t-il effectivement du fait de cet organe ? Dernier garde-fou du
     * watchdog : un thread mort alors que le robot est à l'arrêt ne justifie pas un arrêt d'urgence.
     */
    default boolean enMouvement() {
        return false;
    }
}
