package fr.roboteek.robot.organes.actionneurs.animation.modele;

/**
 * Un son du Studio, lancé à un instant de l'animation.
 * <p>
 * Le son est désigné par son <b>nom</b> dans la bibliothèque du Studio, et non embarqué : le même
 * bruitage sert à plusieurs animations, et le retoucher dans le Studio doit profiter à toutes.
 * La contrepartie est qu'un son supprimé depuis laisse un nom orphelin — le lecteur le saute en le
 * disant, l'éditeur le signale.
 * <p>
 * La place était réservée depuis le 2026-08-29 avec une énumération de quatre échantillons, jamais
 * remplie : passer au nom ne change donc pas la version du format, aucune animation enregistrée
 * ne portant de son.
 *
 * @param instant instant depuis le début de l'animation, en millisecondes
 * @param son     nom du son dans la bibliothèque du Studio
 */
public record SonDeclenche(long instant, String son) {
}
