package fr.roboteek.robot.memoire.courtterme;

/**
 * Ce que la caméra rend d'un visage à un instant : de quoi le reconnaître, et de quoi le montrer.
 * Les deux voyagent ensemble parce qu'ils sont extraits ensemble, pendant que l'organe de vision
 * tient le {@code Mat} — les séparer obligerait à repasser sur l'image.
 *
 * @param vignette portrait recadré en JPEG, {@code null} si le visage touchait le bord
 */
public record PriseDeVisage(float[] empreinte, byte[] vignette) {
}
