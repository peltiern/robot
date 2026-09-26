package fr.roboteek.robot.organes.actionneurs.voix;

/**
 * Une voix complète : la voix de base que Piper prend, et la coloration que sox lui donne.
 *
 * @param modele   le fichier du modèle, {@code null} pour celui de {@code robot.properties}
 * @param locuteur la voix dans un modèle qui en a plusieurs, {@code null} sinon
 * @param reglages la coloration
 */
public record Voix(String modele, Integer locuteur, ReglagesVoix reglages) {

    /** Le modèle de {@code robot.properties}, avec la coloration Wall-E du 2026-08-16. */
    public static final Voix ORIGINE = new Voix(null, null, ReglagesVoix.ORIGINE);

    /** Les réglages ramenés à ce que sox accepte ; sans réglages, ceux d'origine. */
    public Voix bornee() {
        return new Voix(modele, locuteur, reglages == null ? ReglagesVoix.ORIGINE : reglages.bornes());
    }
}
