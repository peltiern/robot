package fr.roboteek.robot.util.gamepad.jamepad;

import fr.roboteek.robot.organes.actionneurs.ConduiteDifferentielle;
import fr.roboteek.robot.systemenerveux.event.DisplayPositionEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementRoueEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;
import fr.roboteek.robot.util.gamepad.shared.RobotGamepadController;

public class RobotJamepadController implements RobotGamepadController, PS3Listener {

    private JamepadManager jamepadManager;

    public RobotJamepadController() {
        jamepadManager = new JamepadManager();
        jamepadManager.addListener(this);
    }

    @Override
    public void start() {
        jamepadManager.start();
    }

    @Override
    public void onEvent(Ps3ControllerEvent event) {
        // System.out.println("ESSAI = " + event.toString());
        event.getModifiedComponents().forEach(ps3Component -> {
            switch (ps3Component) {
                case JOYSTICK_LEFT_AXIS_X:
                case JOYSTICK_LEFT_AXIS_Y:
                    processJoystickLeft(event);
                    break;
                case JOYSTICK_RIGHT_AXIS_X:
                    processJoystickRightX(event);
                    break;
                case JOYSTICK_RIGHT_AXIS_Y:
                    processJoystickRightY(event);
                    break;
                case BUTTON_CROSS_TOP:
                    processButtonCrossTop(event);
                case BUTTON_CROSS_BOTTOM:
                    processButtonCrossBottom(event);
                case BUTTON_CROSS_LEFT:
                    processButtonCrossLeft(event);
                case BUTTON_CROSS_RIGHT:
                    processButtonCrossRight(event);
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
                case BUTTON_SELECT:
                    processButtonSelect(event);
                    break;
            }
        });
    }

    private void processJoystickLeft(Ps3ControllerEvent event) {
        double amplitude = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AMPLITUDE).getCurrentNumericValue();
        // Ceil amplitude to 1
        amplitude = amplitude > 1 ? 1 : amplitude;
        double xValue = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AXIS_X).getCurrentNumericValue();
        double yValue = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AXIS_Y).getCurrentNumericValue();
        MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
        if (amplitude > 0.2) {
            // Differential drive
            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.DIFFERENTIEL);
            mouvementRoueEvent.setAccelerationRoueGauche(ConduiteDifferentielle.ACCELERATION_PAR_DEFAUT);
            mouvementRoueEvent.setAccelerationRoueDroite(ConduiteDifferentielle.ACCELERATION_PAR_DEFAUT);
            int signe = yValue > -0.1 ? 1 : -1;
            if (xValue > 0) {
                // Turn right
                mouvementRoueEvent.setVitesseRoueGauche(amplitude * signe);
                mouvementRoueEvent.setVitesseRoueDroite((1 - xValue) * amplitude * signe);
            } else {
                // Turn left
                mouvementRoueEvent.setVitesseRoueGauche((1 + xValue) * amplitude * signe);
                mouvementRoueEvent.setVitesseRoueDroite(amplitude * signe);
            }
        } else {
            // Stop
            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
        }
        RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
    }

    private void processButtonCrossTop(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> topValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_TOP);
        if (topValue.getCurrentPressed()) {
            // Forward
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.AVANCER, 1D, ConduiteDifferentielle.ACCELERATION_PAR_DEFAUT);
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        } else {
            // Stop if all cross buttons are not pressed
            GamepadComponentValue<PS3Component> bottomValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_BOTTOM);
            GamepadComponentValue<PS3Component> leftValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_LEFT);
            GamepadComponentValue<PS3Component> rightValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_RIGHT);
            if (!bottomValue.getCurrentPressed() && !leftValue.getCurrentPressed() && !rightValue.getCurrentPressed()) {
                MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
                mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
                RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
            }
        }
    }

    private void processButtonCrossBottom(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> bottomValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_BOTTOM);
        if (bottomValue.getCurrentPressed()) {
            // Backward
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.RECULER, 1D, ConduiteDifferentielle.ACCELERATION_PAR_DEFAUT);
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        } else {
            // Stop if all cross buttons are not pressed
            GamepadComponentValue<PS3Component> topValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_TOP);
            GamepadComponentValue<PS3Component> leftValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_LEFT);
            GamepadComponentValue<PS3Component> rightValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_RIGHT);
            if (!topValue.getCurrentPressed() && !leftValue.getCurrentPressed() && !rightValue.getCurrentPressed()) {
                MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
                mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
                RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
            }
        }
    }

    private void processButtonCrossLeft(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> leftValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_LEFT);
        if (leftValue.getCurrentPressed()) {
            // Rotate left
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_GAUCHE, 1D, ConduiteDifferentielle.ACCELERATION_PAR_DEFAUT);
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        } else {
            // Stop if all cross buttons are not pressed
            GamepadComponentValue<PS3Component> topValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_TOP);
            GamepadComponentValue<PS3Component> bottomValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_BOTTOM);
            GamepadComponentValue<PS3Component> rightValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_RIGHT);
            if (!topValue.getCurrentPressed() && !bottomValue.getCurrentPressed() && !rightValue.getCurrentPressed()) {
                MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
                mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
                RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
            }
        }
    }

    private void processButtonCrossRight(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> rightValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_RIGHT);
        if (rightValue.getCurrentPressed()) {
            // Rotate right
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_DROIT, 1D, ConduiteDifferentielle.ACCELERATION_PAR_DEFAUT);
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        } else {
            // Stop if all cross buttons are not pressed
            GamepadComponentValue<PS3Component> topValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_TOP);
            GamepadComponentValue<PS3Component> bottomValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_BOTTOM);
            GamepadComponentValue<PS3Component> leftValue = event.getMapValues().get(PS3Component.BUTTON_CROSS_LEFT);
            if (!topValue.getCurrentPressed() && !bottomValue.getCurrentPressed() && !leftValue.getCurrentPressed()) {
                MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
                mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
                RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
            }
        }
    }

    private void processJoystickRightX(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> leftXValue = event.getMapValues().get(PS3Component.JOYSTICK_RIGHT_AXIS_X);
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

    private void processJoystickRightY(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> leftYValue = event.getMapValues().get(PS3Component.JOYSTICK_RIGHT_AXIS_Y);
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setAccelerationHautBas(2000D);
        double absolute = Math.abs(leftYValue.getCurrentNumericValue());
        double value = (absolute < 0.15) ? 0 : leftYValue.getCurrentNumericValue();
        if (value == 0) {
            mouvementCouEvent.setMouvementHauBas(MouvementCouEvent.MOUVEMENTS_HAUT_BAS.STOPPER);
        } else {
            mouvementCouEvent.setMouvementHauBas(value > 0 ? MouvementCouEvent.MOUVEMENTS_HAUT_BAS.TOURNER_HAUT : MouvementCouEvent.MOUVEMENTS_HAUT_BAS.TOURNER_BAS);
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
                mouvementCouEvent.setPositionRoulis(-179);
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
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
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

    private void processButtonSelect(Ps3ControllerEvent event) {
        GamepadComponentValue<PS3Component> selectValue = event.getMapValues().get(PS3Component.BUTTON_SELECT);
        if (selectValue.getCurrentPressed()) {
            DisplayPositionEvent displayPositionEvent = new DisplayPositionEvent();
            RobotEventBus.getInstance().publishAsync(displayPositionEvent);
        }
    }
}
