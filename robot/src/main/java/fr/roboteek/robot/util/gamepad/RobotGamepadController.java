package fr.roboteek.robot.util.gamepad;

import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

public class RobotGamepadController implements PS3Listener {

    private GamepadManager gamepadManager;

    public RobotGamepadController() {
        gamepadManager = new GamepadManager(Ps3Controller.class);
        gamepadManager.addListener(this);
    }

    public void start() {
        gamepadManager.start();
    }

    @Override
    public void onEvent(Ps3ControllerEvent event) {
        System.out.println("ESSAI = " + event.toString());
        switch (event.getComponent()) {
            case JOYSTICK_LEFT_AXIS_X:
                processJoystickLeftX(event);
                break;
            case JOYSTICK_LEFT_AXIS_Y:
                processJoystickLeftY(event);
                break;
            case BUTTON_LEFT_1:
                processButtonLeft1(event);
                break;
            case BUTTON_LEFT_2:
                processButtonLeft2(event);
                break;
            case BUTTON_RIGHT_1:
                processButtonRight1(event);
                break;
            case BUTTON_RIGHT_2:
                processButtonRight2(event);
                break;
            case BUTTON_CROSS:
                processButtonCross(event);
                break;
            case BUTTON_START:
                processButtonStart(event);
                break;
        }
    }

    private void processJoystickLeftX(Ps3ControllerEvent event) {
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setAccelerationGaucheDroite(2000D);
        double absolute = Math.abs(event.getNewNumericValue());
        double value = (absolute < 0.15) ? 0 : event.getNewNumericValue();
        if (value == 0) {
            mouvementCouEvent.setMouvementGaucheDroite(MouvementCouEvent.MOUVEMENTS_GAUCHE_DROITE.STOPPER);
        } else {
            mouvementCouEvent.setMouvementGaucheDroite(value > 0 ? MouvementCouEvent.MOUVEMENTS_GAUCHE_DROITE.TOURNER_DROITE : MouvementCouEvent.MOUVEMENTS_GAUCHE_DROITE.TOURNER_GAUCHE);
            mouvementCouEvent.setVitesseGaucheDroite(absolute * 60);
        }
        mouvementCouEvent.setSynchrone(false);
        RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
    }

    private void processJoystickLeftY(Ps3ControllerEvent event) {
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setAccelerationHautBas(2000D);
        double absolute = Math.abs(event.getNewNumericValue());
        double value = (absolute < 0.15) ? 0 : event.getNewNumericValue();
        if (value == 0) {
            mouvementCouEvent.setMouvementHauBas(MouvementCouEvent.MOUVEMENTS_HAUT_BAS.STOPPER);
        } else {
            mouvementCouEvent.setMouvementHauBas(value > 0 ? MouvementCouEvent.MOUVEMENTS_HAUT_BAS.TOURNER_BAS : MouvementCouEvent.MOUVEMENTS_HAUT_BAS.TOURNER_HAUT);
            mouvementCouEvent.setVitesseHautBas(absolute * 40);
        }
        mouvementCouEvent.setSynchrone(false);
        RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
    }

    private void processButtonLeft1(Ps3ControllerEvent event) {
        if (!event.isButtonTrianglePressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setMouvementOeilGauche(event.getNewPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_HAUT : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        }
    }

    private void processButtonLeft2(Ps3ControllerEvent event) {
        if (!event.isButtonTrianglePressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setMouvementOeilGauche(event.getNewPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        }
    }

    private void processButtonRight1(Ps3ControllerEvent event) {
        if (!event.isButtonTrianglePressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilDroit(2000D);
            mouvementYeuxEvent.setVitesseOeilDroit(50D);
            mouvementYeuxEvent.setMouvementOeilDroit(event.getNewPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_HAUT : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        } else {
            if (event.getNewPressed()) {
                MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
                mouvementCouEvent.setMouvementRoulis(MouvementCouEvent.MOUVEMENTS_ROULIS.HORAIRE);
                mouvementCouEvent.setAccelerationRoulis(2000D);
                mouvementCouEvent.setVitesseRoulis(50D);
                mouvementCouEvent.setPositionRoulis(180);
                mouvementCouEvent.setSynchrone(false);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
            } else {
                MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
                mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
                mouvementYeuxEvent.setVitesseOeilGauche(50D);
                mouvementYeuxEvent.setMouvementOeilGauche(MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
                mouvementYeuxEvent.setAccelerationOeilDroit(2000D);
                mouvementYeuxEvent.setVitesseOeilDroit(50D);
                mouvementYeuxEvent.setMouvementOeilDroit(MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
                mouvementYeuxEvent.setSynchrone(false);
                RobotEventBus.getInstance().publish(mouvementYeuxEvent);
            }
        }
    }

    private void processButtonRight2(Ps3ControllerEvent event) {
        if (!event.isButtonTrianglePressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilDroit(2000D);
            mouvementYeuxEvent.setVitesseOeilDroit(50D);
            mouvementYeuxEvent.setMouvementOeilDroit(event.getNewPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        } else {
            if (event.getNewPressed()) {
                MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
                mouvementCouEvent.setMouvementRoulis(MouvementCouEvent.MOUVEMENTS_ROULIS.HORAIRE);
                mouvementCouEvent.setAccelerationRoulis(2000D);
                mouvementCouEvent.setVitesseRoulis(50D);
                mouvementCouEvent.setPositionRoulis(-180);
                mouvementCouEvent.setSynchrone(false);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
            } else {
                MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
                mouvementYeuxEvent.setMouvementOeilGauche(MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
                mouvementYeuxEvent.setMouvementOeilDroit(MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
                mouvementYeuxEvent.setSynchrone(false);
                RobotEventBus.getInstance().publish(mouvementYeuxEvent);
            }
        }
    }

    private void processButtonStart(Ps3ControllerEvent event) {
        if (event.getNewPressed()) {
            MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
            mouvementCouEvent.setAccelerationHautBas(80D);
            mouvementCouEvent.setVitesseHautBas(40D);
            mouvementCouEvent.setPositionHautBas(0);
            mouvementCouEvent.setAccelerationGaucheDroite(100D);
            mouvementCouEvent.setVitesseGaucheDroite(60D);
            mouvementCouEvent.setPositionGaucheDroite(0);
            mouvementCouEvent.setSynchrone(false);
            RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilDroit(80D);
            mouvementYeuxEvent.setVitesseOeilDroit(50D);
            mouvementYeuxEvent.setPositionOeilDroit(0);
            mouvementYeuxEvent.setAccelerationOeilGauche(80D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setPositionOeilGauche(0);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publishAsync(mouvementYeuxEvent);
        }
    }

    private void processButtonCross(Ps3ControllerEvent event) {
        if (event.getNewPressed()) {
            final ParoleEvent paroleEvent = new ParoleEvent();
            paroleEvent.setTexte("Bonjour");
            RobotEventBus.getInstance().publishAsync(paroleEvent);
        }
    }
}
