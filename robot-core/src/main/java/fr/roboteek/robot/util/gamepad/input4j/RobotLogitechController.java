package fr.roboteek.robot.util.gamepad.input4j;

import fr.roboteek.robot.securite.ArretUrgence;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.*;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;
import fr.roboteek.robot.util.gamepad.shared.RobotGamepadController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Contrôleur de manette Logitech : transforme les entrées manette en évènements du robot.
 * <p>
 * Migré en bean Spring : démarrage géré par {@link SmartLifecycle} en phase CAPTEURS
 * (c'est une entrée de commandes). Backend {@link Input4jGamepadManager} (input4j, mode
 * XInput) ; l'arrêt stoppe la boucle de polling et ferme input4j.
 */
@Component
public class RobotLogitechController implements RobotGamepadController, LogitechListener, SmartLifecycle, OrganeSurveille {

    private final Input4jGamepadManager gamepadManager;

    /**
     * Publication des évènements du système nerveux.
     */
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Arrêt d'urgence (bouton B). Seule dépendance directe à un service : la bascule a besoin de
     * connaître l'état courant, qu'un évènement à sens unique ne donne pas.
     */
    private final ArretUrgence arretUrgence;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(RobotLogitechController.class);

    /**
     * Flag de démarrage (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Seuil de détection d'appui des gâchettes analogiques. Les gâchettes montent à +1 à fond,
     * mais leurs valeurs de repos diffèrent (mesuré sur la F710/Jetson : gâchette gauche au repos
     * ≈ 0.0, gâchette droite ≈ -1.0). Un seuil à 0.5 sépare correctement « enfoncée » du repos pour
     * les DEUX (un seuil bas comme -0.9 rendait la gâchette gauche « toujours enfoncée »).
     */
    private static final float SEUIL_GACHETTE = 0.5F;

    /**
     * Mode ROULIS actif = bouton Y maintenu. Piloté par l'évènement de Y lui-même (et non relu à
     * chaque évènement de gâchette), ce qui supprime la course entre l'arrivée de Y et celle d'une
     * gâchette. Tant qu'il est vrai, le côté droit fait un roulis et le côté gauche est neutralisé —
     * impossible alors de piloter un oeil vers le bas, donc impossible de baisser les deux yeux.
     */
    private volatile boolean roulisActif = false;

    /**
     * États « enfoncé » précédents des deux commandes arrière droites, pour ne déclencher le roulis
     * qu'au front montant (appui) et non à chaque évènement analogique pendant le maintien.
     */
    private boolean gachetteDroiteEnfoncee = false;
    private boolean boutonDroitEnfonce = false;

    public RobotLogitechController(ApplicationEventPublisher applicationEventPublisher, ArretUrgence arretUrgence) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.arretUrgence = arretUrgence;
        gamepadManager = new Input4jGamepadManager();
        gamepadManager.addListener(this);
    }

    @Override
    public void start() {
        gamepadManager.start();
        running = true;
        logger.info("Contrôleur de manette démarré");
    }

    @Override
    public void stop() {
        gamepadManager.stop();
        running = false;
        logger.info("Contrôleur de manette arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.CAPTEURS;
    }

    @Override
    public void onEvent(LogitechControllerEvent event) {
        event.getModifiedComponents().forEach(logitechComponent -> {
            switch (logitechComponent) {
                //case JOYSTICK_LEFT_AXIS_X:
                case JOYSTICK_LEFT_AXIS_Y:
                    processJoystickLeftY(event);
                    break;
                case JOYSTICK_RIGHT_AXIS_X:
                    processJoystickRightX(event);
                    break;
                case JOYSTICK_RIGHT_AXIS_Y:
                    processJoystickRightY(event);
                    break;
                case BUTTON_CROSS_TOP:
                    processButtonCrossTop(event);
                    break;
                case BUTTON_CROSS_TOP_RIGHT:
                    processButtonCrossCenter(event);
                    break;
                case BUTTON_CROSS_RIGHT:
                    processButtonCrossRight(event);
                    break;
                case BUTTON_CROSS_BOTTOM_RIGHT:
                    processButtonCrossCenter(event);
                    break;
                case BUTTON_CROSS_BOTTOM:
                    processButtonCrossBottom(event);
                    break;
                case BUTTON_CROSS_BOTTOM_LEFT:
                    processButtonCrossCenter(event);
                    break;
                case BUTTON_CROSS_LEFT:
                    processButtonCrossLeft(event);
                    break;
                case BUTTON_CROSS_TOP_LEFT:
                    processButtonCrossCenter(event);
                    break;
                case BUTTON_CROSS_CENTER:
                    processButtonCrossCenter(event);
                    break;
                case BUTTON_Y:
                    processButtonY(event);
                    break;
                case BUTTON_LEFT_1:
                    processButtonLeft1(event);
                    break;
                case BUTTON_ANALOG_LEFT_2:
                    processButtonAnalogLeft2(event);
                    break;
                case BUTTON_RIGHT_1:
                    processButtonRight1(event);
                    break;
                case BUTTON_ANALOG_RIGHT_2:
                    processButtonAnalogRight2(event);
                    break;
                case BUTTON_A:
                    processButtonA(event);
                    break;
                case BUTTON_B:
                    processButtonB(event);
                    break;
                case BUTTON_START:
                    processButtonStart(event);
                    break;
                case BUTTON_BACK:
                    processButtonBack(event);
                    break;
            }
        });
    }

    private void processJoystickLeftY(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> leftYValue = event.getMapValues().get(LogitechComponent.JOYSTICK_LEFT_AXIS_Y);
        if (Math.abs(leftYValue.getCurrentNumericValue() - leftYValue.getOldNumericValue()) > 0.03) {
            MouvementCouEvent mouvementCouEvent = ordreDuCouDeLaManette();
            mouvementCouEvent.setAccelerationMonterDescendre(2000D);
            double absolute = Math.abs(leftYValue.getCurrentNumericValue());
            double value = (absolute < 0.15) ? 0 : leftYValue.getCurrentNumericValue();
            if (value == 0) {
                mouvementCouEvent.setMouvementMonterDescendre(MouvementCouEvent.MOUVEMENTS_MONTER_DESCENDRE.STOPPER);
            } else {
                mouvementCouEvent.setMouvementMonterDescendre(value > 0 ? MouvementCouEvent.MOUVEMENTS_MONTER_DESCENDRE.DESCENDRE : MouvementCouEvent.MOUVEMENTS_MONTER_DESCENDRE.MONTER);
                mouvementCouEvent.setVitesseMonterDescendre(60D);
            }
            mouvementCouEvent.setSynchrone(false);
            logger.debug("GAMEPAD processJoystickLeftY = {}", mouvementCouEvent);
            applicationEventPublisher.publishEvent(mouvementCouEvent);
        }
    }

    private void processButtonCrossTop(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> topValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_TOP);
        if (topValue.getCurrentPressed()) {
            // Forward
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.AVANCER, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            applicationEventPublisher.publishEvent(mouvementRoueEvent);
        }
    }

    private void processButtonCrossBottom(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> bottomValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_BOTTOM);
        if (bottomValue.getCurrentPressed()) {
            // Backward
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.RECULER, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            applicationEventPublisher.publishEvent(mouvementRoueEvent);
        }
    }

    private void processButtonCrossLeft(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> leftValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_LEFT);
        if (leftValue.getCurrentPressed()) {
            // Rotate left
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_GAUCHE, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            applicationEventPublisher.publishEvent(mouvementRoueEvent);
        }
    }

    private void processButtonCrossRight(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> rightValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_RIGHT);
        if (rightValue.getCurrentPressed()) {
            // Rotate right
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_DROIT, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            applicationEventPublisher.publishEvent(mouvementRoueEvent);
        }
    }

    private void processButtonCrossCenter(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> centerValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_CENTER);
        if (centerValue.getCurrentPressed()) {
            // Stop
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
            applicationEventPublisher.publishEvent(mouvementRoueEvent);
        }
    }

    private void processJoystickRightX(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> leftXValue = event.getMapValues().get(LogitechComponent.JOYSTICK_RIGHT_AXIS_X);
        if (Math.abs(leftXValue.getCurrentNumericValue() - leftXValue.getOldNumericValue()) > 0.03) {
            MouvementCouEvent mouvementCouEvent = ordreDuCouDeLaManette();
            mouvementCouEvent.setAccelerationPanoramique(2000D);
            double absolute = Math.abs(leftXValue.getCurrentNumericValue());
            double value = (absolute < 0.15) ? 0 : leftXValue.getCurrentNumericValue();
            if (value == 0) {
                mouvementCouEvent.setMouvementPanoramique(MouvementCouEvent.MOUVEMENTS_PANORAMIQUE.STOPPER);
            } else {
                mouvementCouEvent.setMouvementPanoramique(value > 0 ? MouvementCouEvent.MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE : MouvementCouEvent.MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE);
                mouvementCouEvent.setVitessePanoramique(40D);
            }
            mouvementCouEvent.setSynchrone(false);
            applicationEventPublisher.publishEvent(mouvementCouEvent);
        }
    }

    private void processJoystickRightY(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> leftYValue = event.getMapValues().get(LogitechComponent.JOYSTICK_RIGHT_AXIS_Y);
        if (Math.abs(leftYValue.getCurrentNumericValue() - leftYValue.getOldNumericValue()) > 0.03) {
            MouvementCouEvent mouvementCouEvent = ordreDuCouDeLaManette();
            mouvementCouEvent.setAccelerationInclinaison(2000D);
            double absolute = Math.abs(leftYValue.getCurrentNumericValue());
            double value = (absolute < 0.15) ? 0 : leftYValue.getCurrentNumericValue();
            if (value == 0) {
                mouvementCouEvent.setMouvementInclinaison(MouvementCouEvent.MOUVEMENTS_INCLINAISON.STOPPER);
            } else {
                mouvementCouEvent.setMouvementInclinaison(value > 0 ? MouvementCouEvent.MOUVEMENTS_INCLINAISON.TOURNER_BAS : MouvementCouEvent.MOUVEMENTS_INCLINAISON.TOURNER_HAUT);
                mouvementCouEvent.setVitesseInclinaison(10D);
            }
            mouvementCouEvent.setSynchrone(false);
            applicationEventPublisher.publishEvent(mouvementCouEvent);
        }
    }

    /**
     * Bouton Y = interrupteur du mode ROULIS. Piloté par l'évènement de Y lui-même : à l'appui on
     * entre en mode roulis, au relâchement on en sort. Dans les deux cas on stoppe les yeux : le
     * roulis est un geste MAINTENU qui doit s'arrêter dès que Y (ou la commande arrière) est
     * relâché, et l'appui repart d'un état propre (annule un pilotage d'oeil resté actif).
     */
    private void processButtonY(LogitechControllerEvent event) {
        boolean y = Boolean.TRUE.equals(event.getMapValues().get(LogitechComponent.BUTTON_Y).getCurrentPressed());
        if (y != roulisActif) {
            roulisActif = y;
            publierArretYeux();
        }
    }

    private void processButtonLeft1(LogitechControllerEvent event) {
        // Bouton arrière gauche = oeil gauche vers le HAUT (hors roulis ; cf. calibration inversée
        // du moteur gauche : TOURNER_BAS fait physiquement MONTER l'oeil gauche).
        boolean enfonce = Boolean.TRUE.equals(event.getMapValues().get(LogitechComponent.BUTTON_LEFT_1).getCurrentPressed());
        piloterOeilGauche(enfonce, MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS);
    }

    private void processButtonAnalogLeft2(LogitechControllerEvent event) {
        // Gâchette arrière gauche = oeil gauche vers le BAS (hors roulis ; TOURNER_HAUT fait
        // physiquement DESCENDRE l'oeil gauche).
        Float valeur = event.getMapValues().get(LogitechComponent.BUTTON_ANALOG_LEFT_2).getCurrentNumericValue();
        boolean enfonce = valeur != null && valeur >= SEUIL_GACHETTE;
        piloterOeilGauche(enfonce, MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_HAUT);
    }

    private void processButtonRight1(LogitechControllerEvent event) {
        // Bouton arrière droit : sous Y → roulis HORAIRE (oeil gauche descend / oeil droit monte) ;
        // sans Y → oeil droit vers le HAUT.
        boolean enfonce = Boolean.TRUE.equals(event.getMapValues().get(LogitechComponent.BUTTON_RIGHT_1).getCurrentPressed());
        boutonDroitEnfonce = piloterArriereDroite(boutonDroitEnfonce, enfonce,
                MouvementCouEvent.MOUVEMENTS_ROULIS.HORAIRE, MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_HAUT);
    }

    private void processButtonAnalogRight2(LogitechControllerEvent event) {
        // Gâchette arrière droite : sous Y → roulis ANTI_HORAIRE (oeil gauche monte / oeil droit
        // descend) ; sans Y → oeil droit vers le BAS.
        Float valeur = event.getMapValues().get(LogitechComponent.BUTTON_ANALOG_RIGHT_2).getCurrentNumericValue();
        boolean enfonce = valeur != null && valeur >= SEUIL_GACHETTE;
        gachetteDroiteEnfoncee = piloterArriereDroite(gachetteDroiteEnfoncee, enfonce,
                MouvementCouEvent.MOUVEMENTS_ROULIS.ANTI_HORAIRE, MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS);
    }

    /**
     * Pilotage de l'oeil gauche par les commandes arrière gauches (hors roulis). Sous Y, le côté
     * gauche est neutralisé : le roulis pilote les deux yeux via le côté droit, et on ne publie
     * aucun mouvement d'oeil gauche — impossible donc de le pousser vers le bas pendant un roulis.
     */
    private void piloterOeilGauche(boolean enfonce, MouvementYeuxEvent.MOUVEMENTS_OEIL mouvementEnfonce) {
        if (roulisActif) {
            return;
        }
        publierMouvementOeilGauche(enfonce ? mouvementEnfonce : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
    }

    /**
     * Pilotage d'une commande arrière droite (gâchette ou bouton), selon le mode courant :
     * <ul>
     *   <li>sous Y (roulis) : au FRONT MONTANT (appui) seulement, on incline jusqu'à la butée ;
     *   rien au relâchement (le roulis est un geste ponctuel, pas maintenu) ;</li>
     *   <li>sans Y : contrôle direct de l'oeil droit, maintenu tant que la commande est enfoncée
     *   (mouvement à l'appui, arrêt au relâchement).</li>
     * </ul>
     *
     * @param avant      état enfoncé précédent de cette commande
     * @param maintenant état enfoncé courant
     * @param sensRoulis sens du roulis à publier sous Y
     * @param mouvementOeilDroit mouvement de l'oeil droit à publier hors Y
     * @return le nouvel état enfoncé (à mémoriser)
     */
    private boolean piloterArriereDroite(boolean avant, boolean maintenant,
                                         MouvementCouEvent.MOUVEMENTS_ROULIS sensRoulis,
                                         MouvementYeuxEvent.MOUVEMENTS_OEIL mouvementOeilDroit) {
        boolean frontMontant = !avant && maintenant;
        boolean frontDescendant = avant && !maintenant;
        if (roulisActif) {
            if (frontMontant) {
                publierRoulis(sensRoulis);
            } else if (frontDescendant) {
                // Roulis MAINTENU : relâcher la commande arrière stoppe les deux yeux.
                publierArretYeux();
            }
        } else {
            if (frontMontant) {
                publierMouvementOeilDroit(mouvementOeilDroit);
            } else if (frontDescendant) {
                publierMouvementOeilDroit(MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            }
        }
        return maintenant;
    }

    /**
     * Publie un roulis des yeux (les deux yeux partent en sens opposés, cf.
     * {@code Yeux.setPositionRoulis}).
     * <p>
     * L'amplitude demandée n'est pas une constante : c'est la course totale d'un oeil
     * ({@code max − min} des butées relatives). Comme cette course est toujours ≥ à la marge
     * restante de chacun des deux yeux, {@code Yeux.setPositionRoulis} la plafonne à la marge
     * symétrique réelle du moment — le roulis va donc toujours au maximum possible dans le sens
     * demandé, les deux yeux bougeant de la même amplitude en sens opposés.
     */
    private void publierRoulis(MouvementCouEvent.MOUVEMENTS_ROULIS sens) {
        double amplitudeMax = phidgetsConfig().eyeMotorRelativePositionMax()
                - phidgetsConfig().eyeMotorRelativePositionMin();
        MouvementCouEvent mouvementCouEvent = ordreDuCouDeLaManette();
        mouvementCouEvent.setMouvementRoulis(sens);
        mouvementCouEvent.setAccelerationRoulis(2000D);
        mouvementCouEvent.setVitesseRoulis(50D);
        mouvementCouEvent.setPositionRoulis(amplitudeMax);
        mouvementCouEvent.setSynchrone(false);
        applicationEventPublisher.publishEvent(mouvementCouEvent);
    }

    /**
     * Publie un mouvement continu de l'oeil droit (contrôle direct arrière droit, sans Y).
     */
    private void publierMouvementOeilDroit(MouvementYeuxEvent.MOUVEMENTS_OEIL mouvement) {
        MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
        mouvementYeuxEvent.setAccelerationOeilDroit(2000D);
        mouvementYeuxEvent.setVitesseOeilDroit(50D);
        mouvementYeuxEvent.setMouvementOeilDroit(mouvement);
        mouvementYeuxEvent.setSynchrone(false);
        applicationEventPublisher.publishEvent(mouvementYeuxEvent);
    }

    /**
     * Publie un mouvement continu de l'oeil gauche (contrôle direct arrière gauche, sans Y).
     */
    private void publierMouvementOeilGauche(MouvementYeuxEvent.MOUVEMENTS_OEIL mouvement) {
        MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
        mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
        mouvementYeuxEvent.setVitesseOeilGauche(50D);
        mouvementYeuxEvent.setMouvementOeilGauche(mouvement);
        mouvementYeuxEvent.setSynchrone(false);
        applicationEventPublisher.publishEvent(mouvementYeuxEvent);
    }

    /**
     * Un ordre de cou signé « manette ».
     * <p>
     * Passer par cette fabrique plutôt que par le constructeur n'est pas une coquetterie : c'est
     * cette signature qui fait taire le suivi de visage le temps que quelqu'un conduise (cf.
     * {@code Regard}). Un ordre publié sans elle se ferait contredire par le regard au milieu du
     * mouvement.
     */
    private MouvementCouEvent ordreDuCouDeLaManette() {
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setOrigine(OrigineMouvement.MANETTE);
        return mouvementCouEvent;
    }

    /**
     * Stoppe les deux yeux (fin d'un roulis).
     */
    private void publierArretYeux() {
        MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
        mouvementYeuxEvent.setMouvementOeilGauche(MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
        mouvementYeuxEvent.setMouvementOeilDroit(MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
        mouvementYeuxEvent.setSynchrone(false);
        applicationEventPublisher.publishEvent(mouvementYeuxEvent);
    }

    private void processButtonStart(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> startValue = event.getMapValues().get(LogitechComponent.BUTTON_START);
        if (startValue.getCurrentPressed()) {
            MouvementCouEvent mouvementCouEvent = ordreDuCouDeLaManette();
            mouvementCouEvent.setAccelerationInclinaison(80D);
            mouvementCouEvent.setVitesseInclinaison(40D);
            mouvementCouEvent.setPositionInclinaison(0);
            mouvementCouEvent.setAccelerationPanoramique(100D);
            mouvementCouEvent.setVitessePanoramique(60D);
            mouvementCouEvent.setPositionPanoramique(0);
            mouvementCouEvent.setAccelerationMonterDescendre(100D);
            mouvementCouEvent.setVitesseMonterDescendre(40D);
            mouvementCouEvent.setPositionMonterDescendre(0);
            mouvementCouEvent.setSynchrone(false);
            applicationEventPublisher.publishEvent(mouvementCouEvent);
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilDroit(80D);
            mouvementYeuxEvent.setVitesseOeilDroit(50D);
            mouvementYeuxEvent.setPositionOeilDroit(0);
            mouvementYeuxEvent.setAccelerationOeilGauche(80D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setPositionOeilGauche(0);
            mouvementYeuxEvent.setSynchrone(false);
            applicationEventPublisher.publishEvent(mouvementYeuxEvent);
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
            applicationEventPublisher.publishEvent(mouvementRoueEvent);
        }
    }

    private void processButtonA(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> crossValue = event.getMapValues().get(LogitechComponent.BUTTON_A);
        if (crossValue.getCurrentPressed()) {
            final ParoleEvent paroleEvent = new ParoleEvent();
            paroleEvent.setTexte("Bonjour");
            applicationEventPublisher.publishEvent(paroleEvent);
        }
    }

    /**
     * Bouton B = arrêt d'urgence des moteurs.
     * <p>
     * Bascule et non simple déclenchement : la manette n'a pas de quoi dédier un second bouton au
     * réarmement, et rester bloqué en arrêt d'urgence parce que la tablette n'est pas à portée
     * serait pire que le risque d'un réarmement involontaire — réarmer ne fait bouger personne,
     * ça se contente de rouvrir la porte aux ordres.
     */
    private void processButtonB(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> bValue = event.getMapValues().get(LogitechComponent.BUTTON_B);
        if (Boolean.TRUE.equals(bValue.getCurrentPressed())) {
            arretUrgence.basculer("manette");
        }
    }

    private void processButtonBack(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> selectValue = event.getMapValues().get(LogitechComponent.BUTTON_BACK);
        if (selectValue.getCurrentPressed()) {
            DisplayPositionEvent displayPositionEvent = new DisplayPositionEvent();
            applicationEventPublisher.publishEvent(displayPositionEvent);
        }
    }

    // --- Surveillance (watchdog + pastilles d'état de l'interface) ---
    //
    // C'est l'organe qui donne tout son sens au watchdog. Les ordres de mouvement qu'il
    // publie sont CONTINUS : une croix directionnelle enfoncée fait avancer le robot jusqu'à ce
    // que la manette envoie un STOPPER. Si sa boucle de scrutation meurt pendant une marche avant,
    // ce STOPPER ne viendra jamais — et le bouton B, qui est l'arrêt d'urgence, est mort avec elle.

    @Override
    public String idOrgane() {
        return "manette";
    }

    @Override
    public String libelleOrgane() {
        return "Manette";
    }

    @Override
    public NatureOrgane nature() {
        return NatureOrgane.CAPTEUR;
    }

    /**
     * En service seulement si une manette a été trouvée, que sa scrutation tourne, et qu'elle est
     * encore censée répondre : sans manette, l'organe est éteint et non en panne — le robot se
     * pilote alors très bien depuis la tablette.
     * <p>
     * Une manette débranchée reste « en service » quelques secondes avant de basculer en éteint
     * (cf. {@code Input4jGamepadManager.devraitRepondre()}) : c'est cette fenêtre qui permet au
     * watchdog de couper si elle a disparu <b>pendant</b> un mouvement. Passé ce délai, son
     * absence est un fait acquis et ne doit plus peser sur rien.
     */
    @Override
    public boolean enService() {
        return running && gamepadManager.devraitRepondre();
    }

    @Override
    public long dernierBattement() {
        return gamepadManager.dernierPollReussi();
    }

    @Override
    public boolean provoqueUnMouvement() {
        return true;
    }

    /**
     * Toujours faux : la manette ne bouge rien elle-même, et ce qu'elle a commandé est déjà
     * constaté par les organes qui l'exécutent (chenilles, cou, yeux). Répondre vrai ici reviendrait
     * à ce que la manette se déclare elle-même en mouvement, ce qui n'apprend rien au watchdog.
     */
    @Override
    public boolean enMouvement() {
        return false;
    }
}
