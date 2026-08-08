package fr.roboteek.robot.securite;

/**
 * Santé constatée d'un organe à un instant donné : un instantané calculé par
 * {@link RegistreSante}, jamais stocké.
 *
 * @param id         identifiant de l'organe (même que {@code /api/organes})
 * @param libelle    libellé lisible
 * @param nature     actionneur ou capteur, pour que l'interface sépare les deux familles
 * @param etat       vivant, muet ou éteint volontairement
 * @param ageMillis  âge du dernier battement, ou {@code null} si l'organe est éteint ou n'a
 *                   encore jamais battu — un âge n'a alors aucun sens à afficher
 * @param surveille  vrai si le {@link WatchDog} peut couper les moteurs à cause de cet organe
 */
public record SanteOrgane(String id, String libelle, NatureOrgane nature, EtatSante etat,
                          Long ageMillis, boolean surveille) {
}
