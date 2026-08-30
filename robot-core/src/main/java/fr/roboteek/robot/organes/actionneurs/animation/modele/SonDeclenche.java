package fr.roboteek.robot.organes.actionneurs.animation.modele;

import fr.roboteek.robot.organes.actionneurs.RobotSound;

/**
 * Un son lancé à un instant de l'animation.
 * <p>
 * <b>La place est réservée, rien ne s'en sert encore</b> (décision du 2026-08-29 : « on verra plus
 * tard ce qu'on en fait »). Elle est posée maintenant parce qu'un format de fichier se change mal
 * une fois des animations écrites, et parce que l'ancien modèle portait déjà un son par étape.
 * <p>
 * Deux questions restent entières le jour où on s'en servira : le capteur vocal est mis en pause
 * pendant qu'un son est joué, ce qui abîme la reconnaissance ; et un son a une durée propre, que
 * la timeline ne connaît pas.
 *
 * @param instant instant depuis le début de l'animation, en millisecondes
 * @param son     le son à jouer
 */
public record SonDeclenche(long instant, RobotSound son) {
}
