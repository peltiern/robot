/**
 * Convertit l'horodatage d'un `RobotEvent` en millisecondes epoch.
 *
 * Le robot sérialise ses `LocalDateTime` en ISO-8601 *local*, sans fuseau et avec une précision
 * nanoseconde — par exemple `2026-07-30T18:24:03.049612332`. D'où deux précautions :
 *
 * - JavaScript interprète une date-heure sans décalage comme une heure *locale*. C'est le
 *   comportement voulu ici, robot et navigateur étant sur le même réseau donc le même fuseau.
 *   Un robot situé dans un autre fuseau afficherait des heures décalées ; il faudrait alors que
 *   le backend émette un `OffsetDateTime` plutôt qu'un `LocalDateTime`.
 * - La spécification ECMAScript ne prévoit que trois décimales de seconde. V8 tolère les neuf
 *   émises et tronque, mais rien ne le garantit sur les autres moteurs : on tronque nous-mêmes
 *   pour obtenir le même résultat partout.
 *
 * @param dateTime champ `dateTime` de l'évènement, tel que reçu (absent ou illisible : toléré)
 * @param reception horodatage de réception, utilisé en repli
 * @returns l'horodatage d'émission en millisecondes epoch, ou celui de réception à défaut
 */
export function horodatageRobotEnMs(dateTime: string | undefined, reception: number): number {
  if (!dateTime) return reception
  const troisDecimales = dateTime.replace(/(\.\d{3})\d+$/, '$1')
  const ms = Date.parse(troisDecimales)
  return Number.isNaN(ms) ? reception : ms
}
