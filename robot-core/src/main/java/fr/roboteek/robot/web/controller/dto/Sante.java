package fr.roboteek.robot.web.controller.dto;

import fr.roboteek.robot.securite.EtatSante;

/**
 * Santé d'un organe, telle qu'exposée avec ses capacités par {@code /api/organes}.
 * <p>
 * Même relevé que celui sur lequel raisonne le watchdog : les pastilles de l'interface et la
 * décision de couper les moteurs viennent de la même donnée, ce qui rend un déclenchement lisible
 * après coup — la pastille était déjà passée au rouge.
 *
 * @param etat      vivant, muet, ou éteint volontairement
 * @param ageMillis âge du dernier battement, ou {@code null} si l'organe n'a jamais donné signe de
 *                  vie (éteint, ou tout juste démarré) — afficher un âge n'aurait alors aucun sens
 * @param surveille vrai si le watchdog peut couper les moteurs à cause de cet organe : ce
 *                  n'est pas le cas des capteurs, dont le silence ne se paie que d'une pastille
 */
public record Sante(EtatSante etat, Long ageMillis, boolean surveille) {
}
