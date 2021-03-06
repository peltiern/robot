package fr.roboteek.robot.util.gamepad.jinput;

import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;
import fr.roboteek.robot.util.gamepad.shared.RobotGamepadController;

public class RobotJinputController implements RobotGamepadController, PS3Listener {

    private GamepadManager gamepadManager;

    public RobotJinputController() {
        gamepadManager = new GamepadManager(Ps3Controller.class);
        gamepadManager.addListener(this);
    }

    @Override
    public void start() {
        gamepadManager.start();
    }

    @Override
    public void onEvent(Ps3ControllerEvent event) {
        System.out.println("ESSAI = " + event.toString());
        event.getModifiedComponents().forEach(ps3Component -> {
            switch (ps3Component) {
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
        });
    }

    private void processJoystickLeftX(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> leftXValue = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AXIS_X);
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setAccelerationGaucheDroite(2000D);
        double absolute = Math.abs(leftXValue.getCurrentNumericValue());
        double value = (absolute < 0.15) ? 0 : leftXValue.getCurrentNumericValue();
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
        GamepadComponentValue<PS3Component> leftYValue = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AXIS_Y);
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setAccelerationHautBas(2000D);
        double absolute = Math.abs(leftYValue.getCurrentNumericValue());
        double value = (absolute < 0.15) ? 0 : leftYValue.getCurrentNumericValue();
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
        GamepadComponentValue<PS3Component> left1Value = event.getMapValues().get(PS3Component.BUTTON_LEFT_1);
        GamepadComponentValue<PS3Component> triangleValue = event.getMapValues().get(PS3Component.BUTTON_TRIANGLE);
        if (!triangleValue.getCurrentPressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setMouvementOeilGauche(left1Value.getCurrentPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_HAUT : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        }
    }

    private void processButtonLeft2(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> left2Value = event.getMapValues().get(PS3Component.BUTTON_LEFT_2);
        GamepadComponentValue<PS3Component> triangleValue = event.getMapValues().get(PS3Component.BUTTON_TRIANGLE);
        if (!triangleValue.getCurrentPressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setMouvementOeilGauche(left2Value.getCurrentPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        }
    }

    private void processButtonRight1(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> right1Value = event.getMapValues().get(PS3Component.BUTTON_RIGHT_1);
        GamepadComponentValue<PS3Component> triangleValue = event.getMapValues().get(PS3Component.BUTTON_TRIANGLE);
        if (!triangleValue.getCurrentPressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilDroit(2000D);
            mouvementYeuxEvent.setVitesseOeilDroit(50D);
            mouvementYeuxEvent.setMouvementOeilDroit(right1Value.getCurrentPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_HAUT : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        } else {
            if (right1Value.getCurrentPressed()) {
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
        GamepadComponentValue<PS3Component> right2Value = event.getMapValues().get(PS3Component.BUTTON_RIGHT_2);
        GamepadComponentValue<PS3Component> triangleValue = event.getMapValues().get(PS3Component.BUTTON_TRIANGLE);
        if (!triangleValue.getCurrentPressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilDroit(2000D);
            mouvementYeuxEvent.setVitesseOeilDroit(50D);
            mouvementYeuxEvent.setMouvementOeilDroit(right2Value.getCurrentPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        } else {
            if (right2Value.getCurrentPressed()) {
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
        GamepadComponentValue<PS3Component> startValue = event.getMapValues().get(PS3Component.BUTTON_START);
        if (startValue.getCurrentPressed()) {
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
        GamepadComponentValue<PS3Component> crossValue = event.getMapValues().get(PS3Component.BUTTON_CROSS);
        if (crossValue.getCurrentPressed()) {
            final ParoleEvent paroleEvent = new ParoleEvent();
            paroleEvent.setTexte("Bonjour");
            RobotEventBus.getInstance().publishAsync(paroleEvent);
        }
    }
}
