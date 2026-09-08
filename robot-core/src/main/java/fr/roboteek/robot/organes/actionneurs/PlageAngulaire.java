package fr.roboteek.robot.organes.actionneurs;

/**
 * Course autorisée d'un axe, en <b>degrés d'organe</b>.
 * <p>
 * L'ordre des bornes moteur ne survit pas à la transmission : un rapport négatif échange le minimum
 * et le maximum, et deux des trois axes du cou en ont un. {@link #entre} range les deux valeurs
 * plutôt que de supposer laquelle est laquelle — supposition qui avait fait envoyer au HUD des
 * bornes inversées sur l'œil droit sans que rien ne le dise.
 *
 * @param min borne basse, en degrés d'organe
 * @param max borne haute, en degrés d'organe
 */
public record PlageAngulaire(double min, double max) {

    /** La plage couverte par deux bornes, quel que soit leur ordre. */
    public static PlageAngulaire entre(double uneBorne, double lAutre) {
        return new PlageAngulaire(Math.min(uneBorne, lAutre), Math.max(uneBorne, lAutre));
    }
}
