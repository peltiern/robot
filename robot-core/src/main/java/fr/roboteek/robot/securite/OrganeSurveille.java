package fr.roboteek.robot.securite;

/**
 * Organe qui donne signe de vie : de quoi afficher son état, et couper les moteurs quand un organe
 * du mouvement se tait (voir {@link WatchDog}).
 * <p>
 * <b>Pourquoi pas {@code isRunning()}</b> : ce drapeau ment. Mis à {@code true} au démarrage, il
 * n'est jamais remis à {@code false} quand la boucle de l'organe meurt sur une exception, et un
 * organe mort se déclare « démarré » jusqu'à l'arrêt du robot. Le battement, lui, est une preuve
 * de vie <b>datée</b>, écrite par le travail réel de l'organe et non par son cycle de vie Spring.
 * <p>
 * <b>Le registre tire, l'organe ne pousse pas</b> : l'organe tient son horodatage à jour,
 * {@link RegistreSante} vient le lire. Aucun organe ne dépend donc du watchdog. Le battement est
 * une simple écriture de {@code long} volatile — à 60 Hz sur la manette, le bus d'évènements
 * serait hors de question.
 * <p>
 * {@code AbstractOrgane} en porte l'implémentation par défaut ; seule la manette, qui n'en hérite
 * pas, tient son propre horodatage.
 */
public interface OrganeSurveille {

    /** Identifiant stable, mêmes valeurs que {@code /api/organes} ({@code cou}, {@code yeux}, …). */
    String idOrgane();

    String libelleOrgane();

    NatureOrgane nature();

    /**
     * L'organe est-il censé battre en ce moment ? {@code false} = éteint volontairement (désactivé
     * par configuration, matériel absent, robot pas encore démarré). Un organe hors service n'est
     * jamais jugé en panne, et jamais un motif d'arrêt d'urgence.
     */
    boolean enService();

    /**
     * Horodatage du dernier battement ({@link System#currentTimeMillis()}), {@code 0} si l'organe
     * n'a jamais battu.
     */
    long dernierBattement();

    /**
     * Seuls les organes du mouvement sont surveillés par le {@link WatchDog}. Périmètre
     * volontairement étroit : un capteur muet mérite une ligne de journal, pas la coupure des
     * moteurs.
     */
    default boolean provoqueUnMouvement() {
        return false;
    }

    /**
     * Dernier garde-fou du watchdog : un thread mort alors que le robot est à l'arrêt ne justifie
     * pas un arrêt d'urgence.
     */
    default boolean enMouvement() {
        return false;
    }
}
