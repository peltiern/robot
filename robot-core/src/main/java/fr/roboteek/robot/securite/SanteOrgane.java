package fr.roboteek.robot.securite;

/**
 * Santé constatée d'un organe à un instant donné : un instantané calculé par
 * {@link RegistreSante}, jamais stocké.
 *
 * @param id        identifiant de l'organe, le même que dans {@code /api/organes}
 * @param ageMillis âge du dernier battement, {@code null} si l'organe est éteint ou n'a jamais
 *                  battu — un âge n'a alors rien à dire
 * @param surveille vrai si le {@link WatchDog} peut couper les moteurs à cause de cet organe
 */
public record SanteOrgane(String id, String libelle, NatureOrgane nature, EtatSante etat,
                          Long ageMillis, boolean surveille) {
}
