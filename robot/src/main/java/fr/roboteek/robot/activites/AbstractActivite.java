package fr.roboteek.robot.activites;

import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.math.geometry.point.Point2d;

import fr.roboteek.robot.decisionnel.Contexte;
import fr.roboteek.robot.memoire.ReconnaissanceFaciale;
import fr.roboteek.robot.systemenerveux.event.MouvementTeteEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementTeteEvent.MOUVEMENTS_GAUCHE_DROITE;
import fr.roboteek.robot.systemenerveux.event.MouvementTeteEvent.MOUVEMENTS_HAUT_BAS;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.VisagesEvent;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;

/**
 * Classe abstraite représentant une activité.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class AbstractActivite {

    private static int LARGEUR_WEBCAM = 640;
    private static int HAUTEUR_WEBCAM = 480;

    private static int LARGEUR_ZONE_TRACKING = 180;
    private static int HAUTEUR_ZONE_TRACKING = 180;

    private static int X1 = (LARGEUR_WEBCAM - LARGEUR_ZONE_TRACKING) / 2;
    private static int X2 = X1 + LARGEUR_ZONE_TRACKING;
    private static int Y1 = (HAUTEUR_WEBCAM - HAUTEUR_ZONE_TRACKING) / 2;
    private static int Y2 = Y1 + HAUTEUR_ZONE_TRACKING;

    /** Système nerveux (bus d'évènements). */
    protected MBassador<RobotEvent> systemeNerveux;

    /** Contexte du robot. */
    protected Contexte contexte;

    /** Reconnaissance faciale. */
    // TODO Utiliser plutôt la mémoire pour accéder à la reconnaissance
    protected ReconnaissanceFaciale reconnaissanceFaciale;

    /**
     * Constructeur.
     * @param systemeNerveux le système nerveux
     */
    public AbstractActivite(MBassador<RobotEvent> systemeNerveux, Contexte contexte, ReconnaissanceFaciale reconnaissanceFaciale) {
        this.systemeNerveux = systemeNerveux;
        this.contexte = contexte;
        this.reconnaissanceFaciale = reconnaissanceFaciale;
    }

    /**
     * Initialise l'activité avant activation des écouteurs d'évènements.
     * Permet, par exemple, de charger des données, initialiser des valeurs, ...
     */
    public abstract void initialiser();

    /**
     * Intercepte les évènements de détection de visages.
     * @param visagesEvent évènement de détection de visages
     */
    @Handler
    public abstract void handleVisagesEvent(VisagesEvent visagesEvent);

    /**
     * Intercepte les évènements de reconnaissance vocale.
     * @param reconnaissanceVocaleEvent évènement de reconnaissance vocale
     */
    @Handler
    public abstract void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent);

    /**
     * Arrête l'activité.
     * Permet au robot d'arrêter proprement l'activité (sauvegarde de données, ...)
     */
    public abstract void arreter();

    /**
     * Envoie un évènement pour dire du texte.
     * @param texte le texte à dire
     */
    public void dire(String texte) {
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(texte);
        systemeNerveux.publish(paroleEvent);
    }

    /**
     * Suit le visage le plus grand qui est détecté. 
     * @param visagesEvent évènement de détection de visages
     * @return le visage le suivi (le plus grand détecté)
     */
    public DetectedFace suivreVisage(VisagesEvent visagesEvent) {
        
        final DetectedFace visagePlusGrand = trouverVisageLePlusGrand(visagesEvent);

        // Suivi du visage
        final MouvementTeteEvent mouvementTeteEvent = new MouvementTeteEvent();

        if (visagePlusGrand != null) {
            // Récupération du centre de gravité du visage
            Point2d centreVisage = visagePlusGrand.getBounds().calculateCentroid();

            // Mouvement du moteur A (lacet <==> tourner la tête horizontalement (pour faire non))
            if (centreVisage.getX() < X1 || centreVisage.getX() > X2) {
                if (centreVisage.getX() < X1) {
                    // Si le centre du visage est à gauche de la zone de tracking : tourner à gauche
                    mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_GAUCHE);
                } else {
                    // Si le centre du visage est à droite de la zone de tracking : tourner à droite
                    mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_DROITE);
                }
            } else {
                // Le centre du visage est dans la zone de tracking : on stoppe
                mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.STOPPER);
            }

            // Mouvement du moteur B (tangage <==> tourner la tête verticalement (pour faire oui))
            if (centreVisage.getY() < Y1 || centreVisage.getY() > Y2) {
                if (centreVisage.getY() < Y1) {
                    // Si le centre du visage est au-dessus de la zone de tracking : tourner vers le haut
                    mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_HAUT);
                } else {
                    // Si le centre du visage est en-dessous de la zone de tracking : tourner vers le bas
                    mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_BAS);
                }
            } else {
                // Le centre du visage est dans la zone de tracking : on stoppe
                mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.STOPPER);
            }

        } else {
            // Pas de visage : on stoppe tout
            mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.STOPPER);
            mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.STOPPER);
        }

        systemeNerveux.publish(mouvementTeteEvent);

        return visagePlusGrand;
    }
    
    public DetectedFace trouverVisageLePlusGrand(VisagesEvent visagesEvent) {
     // Recherche du visage le plus grand
        double aireVisagePlusGrand = 0d;
        DetectedFace visagePlusGrand = null;
        if (visagesEvent.getListeVisages() != null && !visagesEvent.getListeVisages().isEmpty()) {
            for (final DetectedFace visage : visagesEvent.getListeVisages()) {
                double aireVisage = visage.getBounds().calculateArea();
                if (aireVisage > aireVisagePlusGrand) {
                    aireVisagePlusGrand = aireVisage;
                    visagePlusGrand = visage;
                }
            }
        }
        return visagePlusGrand;
    }

}
