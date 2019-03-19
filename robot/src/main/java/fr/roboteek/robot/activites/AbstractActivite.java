package fr.roboteek.robot.activites;

import fr.roboteek.robot.decisionnel.Contexte;
import fr.roboteek.robot.memoire.ReconnaissanceFaciale;
import fr.roboteek.robot.systemenerveux.event.*;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_GAUCHE_DROITE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_HAUT_BAS;
import net.engio.mbassy.listener.Handler;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.Rectangle;


/**
 * Classe abstraite représentant une activité.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class AbstractActivite {

    private static final int LARGEUR_WEBCAM = 640;
    private static final int HAUTEUR_WEBCAM = 480;
    private static final int AXE_MEDIAN_X = LARGEUR_WEBCAM / 2;
    private static final int AXE_MEDIAN_Y = HAUTEUR_WEBCAM / 2;

    private static final int LARGEUR_ZONE_TRACKING = 220;
    private static final int HAUTEUR_ZONE_TRACKING = 220;

    private static final int COMPENSATION_DECALAGE = 20;

    private static final int X1 = (LARGEUR_WEBCAM - LARGEUR_ZONE_TRACKING) / 2;
    private static final int X2 = X1 + LARGEUR_ZONE_TRACKING;
    private static final int Y1 = (HAUTEUR_WEBCAM - HAUTEUR_ZONE_TRACKING) / 2;
    private static final int Y2 = Y1 + HAUTEUR_ZONE_TRACKING;

    private static final double PSEUDO_FOCALE = 400;
    private static final double LARGEUR_VISAGE_MM = 300;

    /** Contexte du robot. */
    protected Contexte contexte;

    /** Reconnaissance faciale. */
    // TODO Utiliser plutôt la mémoire pour accéder à la reconnaissance
    protected ReconnaissanceFaciale reconnaissanceFaciale;

    /**
     * Constructeur.
     * @param systemeNerveux le système nerveux
     */
    public AbstractActivite(Contexte contexte, ReconnaissanceFaciale reconnaissanceFaciale) {
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
        RobotEventBus.getInstance().publishAsync(paroleEvent);
    }

//    /**
//     * Suit le visage le plus grand qui est détecté.
//     * @param visagesEvent évènement de détection de visages
//     * @return le visage le suivi (le plus grand détecté)
//     */
//    public DetectedFace suivreVisage(VisagesEvent visagesEvent) {
//
//        final DetectedFace visagePlusGrand = trouverVisageLePlusGrand(visagesEvent);
//
//        // Suivi du visage
//        final MouvementCouEvent mouvementTeteEvent = new MouvementCouEvent();
//
//
//        if (visagePlusGrand != null) {
//            mouvementTeteEvent.setVitesseGaucheDroite(30d);
//            mouvementTeteEvent.setVitesseHautBas(10d);
//            mouvementTeteEvent.setAccelerationGaucheDroite(100d);
//            mouvementTeteEvent.setAccelerationHautBas(100d);
//
//            // Récupération du centre de gravité du visage
//            Point2d centreVisage = visagePlusGrand.getBounds().calculateCentroid();
//
//            // Mouvement du moteur A (lacet <==> tourner la tête horizontalement (pour faire non))
//            if (centreVisage.getX() < X1 || centreVisage.getX() > X2) {
//                if (centreVisage.getX() < X1) {
//                    // Si le centre du visage est à gauche de la zone de tracking : tourner à gauche
//                    mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_GAUCHE);
//                } else {
//                    // Si le centre du visage est à droite de la zone de tracking : tourner à droite
//                    mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_DROITE);
//                }
//            } else {
//                // Le centre du visage est dans la zone de tracking : on stoppe
//                mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.STOPPER);
//            }
//
//            // Mouvement du moteur B (tangage <==> tourner la tête verticalement (pour faire oui))
//            if (centreVisage.getY() < Y1 || centreVisage.getY() > Y2) {
//                if (centreVisage.getY() < Y1) {
//                    // Si le centre du visage est au-dessus de la zone de tracking : tourner vers le haut
//                    mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_HAUT);
//                } else {
//                    // Si le centre du visage est en-dessous de la zone de tracking : tourner vers le bas
//                    mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_BAS);
//                }
//            } else {
//                // Le centre du visage est dans la zone de tracking : on stoppe
//                mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.STOPPER);
//            }
//
//        } else {
//            // Pas de visage : on stoppe tout
//            mouvementTeteEvent.setVitesseGaucheDroite(30d);
//            mouvementTeteEvent.setVitesseHautBas(30d);
//            mouvementTeteEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.STOPPER);
//            mouvementTeteEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.STOPPER);
//        }
//
//        RobotEventBus.getInstance().publishAsync(mouvementTeteEvent);
//
//        return visagePlusGrand;
//    }

    /**
     * Suit le visage le plus grand qui est détecté.
     * @param visagesEvent évènement de détection de visages
     * @return le visage le suivi (le plus grand détecté)
     */
    public static DetectedFace suivreVisage(VisagesEvent visagesEvent) {

        final DetectedFace visagePlusGrand = trouverVisageLePlusGrand(visagesEvent);

        // Suivi du visage
        if (visagePlusGrand != null) {

            // Récupération du centre de gravité du visage
            Point2d centreVisage = visagePlusGrand.getBounds().calculateCentroid();

            // Calcul de la distance entre la webcam et le visage
            double distanceVisage = calculerDistanceVisage(visagePlusGrand.getBounds());

            // Mouvement du moteur A (lacet <==> tourner la tête horizontalement (pour faire non))
            // Calcul de l'angle "Gauche - Droite"
            double angleGaucheDroite = Math.toDegrees(Math.atan((centreVisage.getX() - AXE_MEDIAN_X) / PSEUDO_FOCALE));
            if (Math.abs(angleGaucheDroite) < 5) {
                angleGaucheDroite = MouvementCouEvent.ANGLE_NEUTRE;
            }

            // Mouvement du moteur B (tangage <==> tourner la tête verticalement (pour faire oui))
            // Calcul de l'angle "Haut - Bas"
            double angleHautBas = Math.toDegrees(Math.atan((centreVisage.getY() - AXE_MEDIAN_Y) / PSEUDO_FOCALE));
            if (Math.abs(angleHautBas) < 5) {
                angleHautBas = MouvementCouEvent.ANGLE_NEUTRE;
            }

            System.out.println("DIST = " + distanceVisage + ", angleGD = " + angleGaucheDroite + ", angleHB = " + angleHautBas);

            if (angleGaucheDroite != MouvementCouEvent.ANGLE_NEUTRE || angleHautBas != MouvementCouEvent.ANGLE_NEUTRE) {
                MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
                mouvementCouEvent.setAngleGaucheDroite(angleGaucheDroite);
                mouvementCouEvent.setAngleHautBas(angleHautBas);
                mouvementCouEvent.setVitesseGaucheDroite(80d);
                mouvementCouEvent.setVitesseHautBas(80d);
                mouvementCouEvent.setAccelerationGaucheDroite(2000d);
                mouvementCouEvent.setAccelerationHautBas(2000d);
                mouvementCouEvent.setSynchrone(true);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
            }
        }

        return visagePlusGrand;
    }
    
    public static DetectedFace trouverVisageLePlusGrand(VisagesEvent visagesEvent) {
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

    private static double calculerDistanceVisage(Rectangle visage) {
        return PSEUDO_FOCALE * LARGEUR_VISAGE_MM / visage.getWidth();
    }

}
