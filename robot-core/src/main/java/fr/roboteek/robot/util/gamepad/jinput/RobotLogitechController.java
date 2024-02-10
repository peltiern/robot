package fr.roboteek.robot.util.gamepad.jinput;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.systemenerveux.event.*;
import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;
import fr.roboteek.robot.util.gamepad.shared.RobotGamepadController;

import java.lang.reflect.Field;
import java.util.Arrays;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

public class RobotLogitechController implements RobotGamepadController, LogitechListener {

    private GamepadManager gamepadManager;

    public RobotLogitechController() {
        // Ajout de la librairie native JInput
        try {
            addLibraryPath(Constantes.DOSSIER_JINPUT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        gamepadManager = new GamepadManager(LogitechController.class);
        gamepadManager.addListener(this);
    }

    @Override
    public void start() {
        gamepadManager.start();
    }

    @Override
    public void onEvent(LogitechControllerEvent event) {
//        System.out.println("ESSAI = " + event.toString());
        event.getModifiedComponents().forEach(logitechComponent -> {
//            System.out.println(event);
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
                case BUTTON_CROSS_TOP_RIGHT:
                    processButtonCrossCenter(event);
                case BUTTON_CROSS_RIGHT:
                    processButtonCrossRight(event);
                case BUTTON_CROSS_BOTTOM_RIGHT:
                    processButtonCrossCenter(event);
                case BUTTON_CROSS_BOTTOM:
                    processButtonCrossBottom(event);
                case BUTTON_CROSS_BOTTOM_LEFT:
                    processButtonCrossCenter(event);
                case BUTTON_CROSS_LEFT:
                    processButtonCrossLeft(event);
                case BUTTON_CROSS_TOP_LEFT:
                    processButtonCrossCenter(event);
                case BUTTON_CROSS_CENTER:
                    processButtonCrossCenter(event);
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
                case BUTTON_START:
                    processButtonStart(event);
                    break;
                case BUTTON_BACK:
                    processButtonBack(event);
                    break;
            }
        });
    }

//    private void processJoystickLeft(LogitechControllerEvent event) {
//        double amplitude = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AMPLITUDE).getCurrentNumericValue();
//        // Ceil amplitude to 1
//        amplitude = amplitude > 1 ? 1 : amplitude;
//        double xValue = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AXIS_X).getCurrentNumericValue();
//        double yValue = event.getMapValues().get(PS3Component.JOYSTICK_LEFT_AXIS_Y).getCurrentNumericValue();
//        MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
//        if (amplitude > 0.2) {
//            // Differential drive
//            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.DIFFERENTIEL);
//            mouvementRoueEvent.setAccelerationRoueGauche(phidgetsConfig().differentialDrivingMotorAcceleration());
//            mouvementRoueEvent.setAccelerationRoueDroite(phidgetsConfig().differentialDrivingMotorAcceleration());
//            int signe = yValue > -0.1 ? 1 : -1;
//            if (xValue > 0) {
//                // Turn right
//                mouvementRoueEvent.setVitesseRoueGauche(amplitude * signe);
//                mouvementRoueEvent.setVitesseRoueDroite((1 - xValue) * amplitude * signe);
//            } else {
//                // Turn left
//                mouvementRoueEvent.setVitesseRoueGauche((1 + xValue) * amplitude * signe);
//                mouvementRoueEvent.setVitesseRoueDroite(amplitude * signe);
//            }
//        } else {
//            // Stop
//            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
//        }
//        RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
//    }

        private void processJoystickLeftY(LogitechControllerEvent event) {
            GamepadComponentValue<LogitechComponent> leftYValue = event.getMapValues().get(LogitechComponent.JOYSTICK_LEFT_AXIS_Y);
            if (Math.abs(leftYValue.getCurrentNumericValue() - leftYValue.getOldNumericValue()) > 0.03) {
                MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
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
                System.out.println("GAMEPAD processJoystickLeftY = " + mouvementCouEvent);
                RobotEventBus.getInstance().publish(mouvementCouEvent);
            }
    }

    private void processButtonCrossTop(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> topValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_TOP);
        if (topValue.getCurrentPressed()) {
            // Forward
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.AVANCER, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        }
    }

    private void processButtonCrossBottom(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> bottomValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_BOTTOM);
        if (bottomValue.getCurrentPressed()) {
            // Backward
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.RECULER, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        }
    }

    private void processButtonCrossLeft(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> leftValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_LEFT);
        if (leftValue.getCurrentPressed()) {
            // Rotate left
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_GAUCHE, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        }
    }

    private void processButtonCrossRight(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> rightValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_RIGHT);
        if (rightValue.getCurrentPressed()) {
            // Rotate right
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent(MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_DROIT, 1D, phidgetsConfig().differentialDrivingMotorAcceleration());
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        }
    }

    private void processButtonCrossCenter(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> centerValue = event.getMapValues().get(LogitechComponent.BUTTON_CROSS_CENTER);
        if (centerValue.getCurrentPressed()) {
            // Stop
            MouvementRoueEvent mouvementRoueEvent = new MouvementRoueEvent();
            mouvementRoueEvent.setMouvementRoue(MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER);
            RobotEventBus.getInstance().publishAsync(mouvementRoueEvent);
        }
    }

    private void processJoystickRightX(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> leftXValue = event.getMapValues().get(LogitechComponent.JOYSTICK_RIGHT_AXIS_X);
        if (Math.abs(leftXValue.getCurrentNumericValue() - leftXValue.getOldNumericValue()) > 0.03) {
            MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
            mouvementCouEvent.setAccelerationPanoramique(2000D);
            double absolute = Math.abs(leftXValue.getCurrentNumericValue());
            double value = (absolute < 0.15) ? 0 : leftXValue.getCurrentNumericValue();
            if (value == 0) {
                mouvementCouEvent.setMouvementPanoramique(MouvementCouEvent.MOUVEMENTS_PANORAMIQUE.STOPPER);
            } else {
                mouvementCouEvent.setMouvementPanoramique(value > 0 ? MouvementCouEvent.MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE : MouvementCouEvent.MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE);
                mouvementCouEvent.setVitessePanoramique(60D);
            }
            mouvementCouEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementCouEvent);
        }
    }

    private void processJoystickRightY(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> leftYValue = event.getMapValues().get(LogitechComponent.JOYSTICK_RIGHT_AXIS_Y);
        if (Math.abs(leftYValue.getCurrentNumericValue() - leftYValue.getOldNumericValue()) > 0.03) {
            MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
            mouvementCouEvent.setAccelerationInclinaison(2000D);
            double absolute = Math.abs(leftYValue.getCurrentNumericValue());
            double value = (absolute < 0.15) ? 0 : leftYValue.getCurrentNumericValue();
            if (value == 0) {
                mouvementCouEvent.setMouvementInclinaison(MouvementCouEvent.MOUVEMENTS_INCLINAISON.STOPPER);
            } else {
                mouvementCouEvent.setMouvementInclinaison(value < 0 ? MouvementCouEvent.MOUVEMENTS_INCLINAISON.TOURNER_BAS : MouvementCouEvent.MOUVEMENTS_INCLINAISON.TOURNER_HAUT);
                mouvementCouEvent.setVitesseInclinaison(40D);
            }
            mouvementCouEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementCouEvent);
        }
    }

    private void processButtonLeft1(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> left1Value = event.getMapValues().get(LogitechComponent.BUTTON_LEFT_1);
        GamepadComponentValue<LogitechComponent> buttonYValue = event.getMapValues().get(LogitechComponent.BUTTON_Y);
        if (!buttonYValue.getCurrentPressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setMouvementOeilGauche(left1Value.getCurrentPressed() ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_HAUT : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        }
    }

    private void processButtonAnalogLeft2(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> analogLeft2Value = event.getMapValues().get(LogitechComponent.BUTTON_ANALOG_LEFT_2);
        boolean analogLeft2Pressed = analogLeft2Value.getCurrentNumericValue() >= -0.9F;
        GamepadComponentValue<LogitechComponent> buttonYValue = event.getMapValues().get(LogitechComponent.BUTTON_Y);
        if (!buttonYValue.getCurrentPressed()) {
            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilGauche(2000D);
            mouvementYeuxEvent.setVitesseOeilGauche(50D);
            mouvementYeuxEvent.setMouvementOeilGauche(analogLeft2Pressed ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        }
    }

    private void processButtonRight1(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> right1Value = event.getMapValues().get(LogitechComponent.BUTTON_RIGHT_1);
        GamepadComponentValue<LogitechComponent> buttonYValue = event.getMapValues().get(LogitechComponent.BUTTON_Y);
        if (!buttonYValue.getCurrentPressed()) {
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

    private void processButtonAnalogRight2(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> analogRight2Value = event.getMapValues().get(LogitechComponent.BUTTON_ANALOG_RIGHT_2);
        boolean analogRight2Pressed = analogRight2Value.getCurrentNumericValue() >= -0.9F;
        GamepadComponentValue<LogitechComponent> buttonYValue = event.getMapValues().get(LogitechComponent.BUTTON_Y);
        if (!buttonYValue.getCurrentPressed()) {

            MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
            mouvementYeuxEvent.setAccelerationOeilDroit(2000D);
            mouvementYeuxEvent.setVitesseOeilDroit(50D);
            mouvementYeuxEvent.setMouvementOeilDroit(analogRight2Pressed ? MouvementYeuxEvent.MOUVEMENTS_OEIL.TOURNER_BAS : MouvementYeuxEvent.MOUVEMENTS_OEIL.STOPPER);
            mouvementYeuxEvent.setSynchrone(false);
            RobotEventBus.getInstance().publish(mouvementYeuxEvent);
        } else {
            if (analogRight2Pressed) {
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

    private void processButtonStart(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> startValue = event.getMapValues().get(LogitechComponent.BUTTON_START);
        if (startValue.getCurrentPressed()) {
            MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
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

    private void processButtonA(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> crossValue = event.getMapValues().get(LogitechComponent.BUTTON_A);
        if (crossValue.getCurrentPressed()) {
            final ParoleEvent paroleEvent = new ParoleEvent();
            paroleEvent.setTexte("Bonjour");
            RobotEventBus.getInstance().publishAsync(paroleEvent);
        }
    }

    private void processButtonBack(LogitechControllerEvent event) {
        GamepadComponentValue<LogitechComponent> selectValue = event.getMapValues().get(LogitechComponent.BUTTON_BACK);
        if (selectValue.getCurrentPressed()) {
            DisplayPositionEvent displayPositionEvent = new DisplayPositionEvent();
            RobotEventBus.getInstance().publishAsync(displayPositionEvent);
        }
    }

    /**
     * Adds the specified path to the java library path
     *
     * @param pathToAdd the path to add
     * @throws Exception
     */
    private static void addLibraryPath(String pathToAdd) throws Exception{
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[])usrPathsField.get(null);

        //check if the path to add is already present
        for(String path : paths) {
            if(path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }
}
