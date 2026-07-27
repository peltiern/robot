package fr.roboteek.robot.spring.server.controller.dto;

/**
 * Représentation REST d'un degré de liberté pilotable d'un organe (articulation).
 * <p>
 * Réunit tout ce qu'un client générique doit connaître pour afficher un contrôle borné,
 * sans rien coder en dur : son libellé, son unité, son {@code orientation} physique, sa plage
 * autorisée ({@code min}/{@code max}, dérivée des butées moteur de la configuration) et sa
 * {@code position} courante réelle. Butées et position sont exprimées dans le même repère
 * « logique » que les évènements de mouvement (degrés autour du neutre 0).
 *
 * @param id          identifiant stable ({@code pan}, {@code oeilGauche}, …)
 * @param libelle     libellé lisible destiné à l'affichage
 * @param unite       unité des valeurs ({@code deg})
 * @param min         butée basse autorisée
 * @param max         butée haute autorisée
 * @param orientation orientation physique du mouvement (choix du widget côté client)
 * @param position    position courante, ou {@code null} si l'organe n'est pas démarré
 */
public record Articulation(
        String id,
        String libelle,
        String unite,
        double min,
        double max,
        Orientation orientation,
        Double position) {
}
