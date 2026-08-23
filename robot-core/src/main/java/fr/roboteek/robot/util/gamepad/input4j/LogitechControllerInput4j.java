package fr.roboteek.robot.util.gamepad.input4j;

import de.gurkenlabs.input4j.InputDevice;
import de.gurkenlabs.input4j.components.XInput;
import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Producteur d'évènements manette basé sur input4j (manette Logitech F710 en mode
 * <b>XInput</b> — switch physique sur X). Enregistre les callbacks input4j sur un
 * {@link InputDevice}, maintient l'état des contrôles, et émet un
 * {@link LogitechControllerEvent} (avec la carte d'état complète) à chaque changement.
 * <p>
 * Remplace l'ancien {@code LogitechController} jinput. Le contrat d'évènement
 * ({@link LogitechComponent}, carte d'état) est identique, si bien que
 * {@link RobotLogitechController} est inchangé.
 * <p>
 * Calibré et validé sur le robot (manette F710 en mode XInput, bouton
 * MODE désactivé) : gâchettes livrées par input4j directement en -1..1 (repos -1, pas
 * de renormalisation), axes Y non inversés.
 */
public class LogitechControllerInput4j {

    private static final boolean INVERSER_Y_STICK_GAUCHE = false;
    private static final boolean INVERSER_Y_STICK_DROIT = false;

    private final List<LogitechListener> listeners = new ArrayList<>();

    private boolean buttonXPressed = false;
    private boolean buttonYPressed = false;
    private boolean buttonAPressed = false;
    private boolean buttonBPressed = false;
    private boolean buttonBackPressed = false;
    private boolean buttonStartPressed = false;
    private boolean buttonLogitechPressed = false;
    private boolean buttonLeft1Pressed = false;
    private boolean buttonLeft2Pressed = false;
    private boolean buttonLeftJoystick3Pressed = false;
    private boolean buttonRight1Pressed = false;
    private boolean buttonRight2Pressed = false;
    private boolean buttonRightJoystick3Pressed = false;

    private float buttonAnalogLeft2Value = -1f;
    private float buttonAnalogRight2Value = -1f;
    private float joystickLeftAxisXValue = 0f;
    private float joystickLeftAxisYValue = 0f;
    private float joystickRightAxisXValue = 0f;
    private float joystickRightAxisYValue = 0f;

    private boolean buttonCrossTopPressed = false;
    private boolean buttonCrossRightPressed = false;
    private boolean buttonCrossBottomPressed = false;
    private boolean buttonCrossLeftPressed = false;
    private boolean buttonCrossCenterPressed = false;

    public void addListener(LogitechListener listener) {
        listeners.add(listener);
    }

    /**
     * Branche tous les callbacks XInput sur le device fourni. Les callbacks se
     * déclenchent lors des {@code device.poll()}.
     */
    public void register(InputDevice device) {
        // Boutons
        registerButton(device, XInput.A, LogitechComponent.BUTTON_A, pressed -> buttonAPressed = pressed);
        registerButton(device, XInput.B, LogitechComponent.BUTTON_B, pressed -> buttonBPressed = pressed);
        registerButton(device, XInput.X, LogitechComponent.BUTTON_X, pressed -> buttonXPressed = pressed);
        registerButton(device, XInput.Y, LogitechComponent.BUTTON_Y, pressed -> buttonYPressed = pressed);
        registerButton(device, XInput.BACK, LogitechComponent.BUTTON_BACK, pressed -> buttonBackPressed = pressed);
        registerButton(device, XInput.START, LogitechComponent.BUTTON_START, pressed -> buttonStartPressed = pressed);
        registerButton(device, XInput.LEFT_SHOULDER, LogitechComponent.BUTTON_LEFT_1, pressed -> buttonLeft1Pressed = pressed);
        registerButton(device, XInput.RIGHT_SHOULDER, LogitechComponent.BUTTON_RIGHT_1, pressed -> buttonRight1Pressed = pressed);
        registerButton(device, XInput.LEFT_THUMB, LogitechComponent.BUTTON_LEFT_JOYSTICK_3, pressed -> buttonLeftJoystick3Pressed = pressed);
        registerButton(device, XInput.RIGHT_THUMB, LogitechComponent.BUTTON_RIGHT_JOYSTICK_3, pressed -> buttonRightJoystick3Pressed = pressed);

        // Croix directionnelle (boutons discrets en XInput). Mapping neutre, à
        // recalibrer en MODE désactivé (cf. sens des sticks).
        registerCross(device, XInput.DPAD_UP, LogitechComponent.BUTTON_CROSS_TOP);
        registerCross(device, XInput.DPAD_DOWN, LogitechComponent.BUTTON_CROSS_BOTTOM);
        registerCross(device, XInput.DPAD_LEFT, LogitechComponent.BUTTON_CROSS_LEFT);
        registerCross(device, XInput.DPAD_RIGHT, LogitechComponent.BUTTON_CROSS_RIGHT);

        // Sticks
        device.onAxisChanged(XInput.LEFT_THUMB_X, v -> {
            float old = joystickLeftAxisXValue;
            joystickLeftAxisXValue = v;
            emitAxis(LogitechComponent.JOYSTICK_LEFT_AXIS_X, old, joystickLeftAxisXValue);
        });
        device.onAxisChanged(XInput.LEFT_THUMB_Y, v -> {
            float old = joystickLeftAxisYValue;
            joystickLeftAxisYValue = INVERSER_Y_STICK_GAUCHE ? -v : v;
            emitAxis(LogitechComponent.JOYSTICK_LEFT_AXIS_Y, old, joystickLeftAxisYValue);
        });
        device.onAxisChanged(XInput.RIGHT_THUMB_X, v -> {
            float old = joystickRightAxisXValue;
            joystickRightAxisXValue = v;
            emitAxis(LogitechComponent.JOYSTICK_RIGHT_AXIS_X, old, joystickRightAxisXValue);
        });
        device.onAxisChanged(XInput.RIGHT_THUMB_Y, v -> {
            float old = joystickRightAxisYValue;
            joystickRightAxisYValue = INVERSER_Y_STICK_DROIT ? -v : v;
            emitAxis(LogitechComponent.JOYSTICK_RIGHT_AXIS_Y, old, joystickRightAxisYValue);
        });

        // Gâchettes analogiques : input4j les livre déjà en -1..1 (repos -1), pas de
        // renormalisation (v*2-1 donnait des valeurs hors bornes).
        device.onAxisChanged(XInput.LEFT_TRIGGER, v -> {
            float old = buttonAnalogLeft2Value;
            buttonAnalogLeft2Value = v;
            emitAxis(LogitechComponent.BUTTON_ANALOG_LEFT_2, old, buttonAnalogLeft2Value);
        });
        device.onAxisChanged(XInput.RIGHT_TRIGGER, v -> {
            float old = buttonAnalogRight2Value;
            buttonAnalogRight2Value = v;
            emitAxis(LogitechComponent.BUTTON_ANALOG_RIGHT_2, old, buttonAnalogRight2Value);
        });
    }

    private void registerButton(InputDevice device, de.gurkenlabs.input4j.InputComponent.ID id,
                                LogitechComponent component, java.util.function.Consumer<Boolean> stateSetter) {
        device.onButtonPressed(id, () -> {
            stateSetter.accept(true);
            emitButton(component, false, true);
        });
        device.onButtonReleased(id, () -> {
            stateSetter.accept(false);
            emitButton(component, true, false);
        });
    }

    /**
     * Croix directionnelle : les directions sont exclusives (comme l'ancien hat).
     * L'appui active la direction et désactive les autres ; le relâchement, s'il ne
     * reste aucune direction, émet {@code BUTTON_CROSS_CENTER} (neutre = stop).
     */
    private void registerCross(InputDevice device, de.gurkenlabs.input4j.InputComponent.ID id, LogitechComponent direction) {
        device.onButtonPressed(id, () -> {
            setCross(direction);
            emitButton(direction, false, true);
        });
        device.onButtonReleased(id, () -> {
            setCrossReleased(direction);
            if (!buttonCrossTopPressed && !buttonCrossBottomPressed && !buttonCrossLeftPressed && !buttonCrossRightPressed) {
                buttonCrossCenterPressed = true;
                emitButton(LogitechComponent.BUTTON_CROSS_CENTER, false, true);
            }
        });
    }

    private void setCross(LogitechComponent direction) {
        buttonCrossTopPressed = direction == LogitechComponent.BUTTON_CROSS_TOP;
        buttonCrossBottomPressed = direction == LogitechComponent.BUTTON_CROSS_BOTTOM;
        buttonCrossLeftPressed = direction == LogitechComponent.BUTTON_CROSS_LEFT;
        buttonCrossRightPressed = direction == LogitechComponent.BUTTON_CROSS_RIGHT;
        buttonCrossCenterPressed = false;
    }

    private void setCrossReleased(LogitechComponent direction) {
        if (direction == LogitechComponent.BUTTON_CROSS_TOP) {
            buttonCrossTopPressed = false;
        } else if (direction == LogitechComponent.BUTTON_CROSS_BOTTOM) {
            buttonCrossBottomPressed = false;
        } else if (direction == LogitechComponent.BUTTON_CROSS_LEFT) {
            buttonCrossLeftPressed = false;
        } else if (direction == LogitechComponent.BUTTON_CROSS_RIGHT) {
            buttonCrossRightPressed = false;
        }
    }

    private void emitButton(LogitechComponent modified, boolean oldPressed, boolean newPressed) {
        Map<LogitechComponent, GamepadComponentValue<LogitechComponent>> map = buildFullStateMap();
        map.put(modified, new GamepadComponentValue<>(modified, oldPressed, newPressed));
        dispatch(new LogitechControllerEvent(List.of(modified), map));
    }

    private void emitAxis(LogitechComponent modified, float oldValue, float newValue) {
        Map<LogitechComponent, GamepadComponentValue<LogitechComponent>> map = buildFullStateMap();
        map.put(modified, new GamepadComponentValue<>(modified, oldValue, newValue));
        dispatch(new LogitechControllerEvent(List.of(modified), map));
    }

    private void dispatch(LogitechControllerEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }

    /**
     * Construit la carte de l'état courant de tous les composants (valeur = valeur
     * courante). L'appelant écrase ensuite l'entrée du composant modifié avec son
     * ancienne + nouvelle valeur.
     */
    private Map<LogitechComponent, GamepadComponentValue<LogitechComponent>> buildFullStateMap() {
        Map<LogitechComponent, GamepadComponentValue<LogitechComponent>> map = new HashMap<>();
        map.put(LogitechComponent.BUTTON_X, new GamepadComponentValue<>(LogitechComponent.BUTTON_X, buttonXPressed, buttonXPressed));
        map.put(LogitechComponent.BUTTON_Y, new GamepadComponentValue<>(LogitechComponent.BUTTON_Y, buttonYPressed, buttonYPressed));
        map.put(LogitechComponent.BUTTON_A, new GamepadComponentValue<>(LogitechComponent.BUTTON_A, buttonAPressed, buttonAPressed));
        map.put(LogitechComponent.BUTTON_B, new GamepadComponentValue<>(LogitechComponent.BUTTON_B, buttonBPressed, buttonBPressed));
        map.put(LogitechComponent.BUTTON_BACK, new GamepadComponentValue<>(LogitechComponent.BUTTON_BACK, buttonBackPressed, buttonBackPressed));
        map.put(LogitechComponent.BUTTON_START, new GamepadComponentValue<>(LogitechComponent.BUTTON_START, buttonStartPressed, buttonStartPressed));
        map.put(LogitechComponent.BUTTON_LOGITECH, new GamepadComponentValue<>(LogitechComponent.BUTTON_LOGITECH, buttonLogitechPressed, buttonLogitechPressed));
        map.put(LogitechComponent.BUTTON_LEFT_1, new GamepadComponentValue<>(LogitechComponent.BUTTON_LEFT_1, buttonLeft1Pressed, buttonLeft1Pressed));
        map.put(LogitechComponent.BUTTON_LEFT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_LEFT_2, buttonLeft2Pressed, buttonLeft2Pressed));
        map.put(LogitechComponent.BUTTON_LEFT_JOYSTICK_3, new GamepadComponentValue<>(LogitechComponent.BUTTON_LEFT_JOYSTICK_3, buttonLeftJoystick3Pressed, buttonLeftJoystick3Pressed));
        map.put(LogitechComponent.BUTTON_RIGHT_1, new GamepadComponentValue<>(LogitechComponent.BUTTON_RIGHT_1, buttonRight1Pressed, buttonRight1Pressed));
        map.put(LogitechComponent.BUTTON_RIGHT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_RIGHT_2, buttonRight2Pressed, buttonRight2Pressed));
        map.put(LogitechComponent.BUTTON_RIGHT_JOYSTICK_3, new GamepadComponentValue<>(LogitechComponent.BUTTON_RIGHT_JOYSTICK_3, buttonRightJoystick3Pressed, buttonRightJoystick3Pressed));
        map.put(LogitechComponent.BUTTON_ANALOG_LEFT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_ANALOG_LEFT_2, buttonAnalogLeft2Value, buttonAnalogLeft2Value));
        map.put(LogitechComponent.BUTTON_ANALOG_RIGHT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_ANALOG_RIGHT_2, buttonAnalogRight2Value, buttonAnalogRight2Value));
        map.put(LogitechComponent.JOYSTICK_LEFT_AXIS_X, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_LEFT_AXIS_X, joystickLeftAxisXValue, joystickLeftAxisXValue));
        map.put(LogitechComponent.JOYSTICK_LEFT_AXIS_Y, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_LEFT_AXIS_Y, joystickLeftAxisYValue, joystickLeftAxisYValue));
        map.put(LogitechComponent.JOYSTICK_RIGHT_AXIS_X, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_RIGHT_AXIS_X, joystickRightAxisXValue, joystickRightAxisXValue));
        map.put(LogitechComponent.JOYSTICK_RIGHT_AXIS_Y, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_RIGHT_AXIS_Y, joystickRightAxisYValue, joystickRightAxisYValue));
        map.put(LogitechComponent.BUTTON_CROSS_TOP, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_TOP, buttonCrossTopPressed, buttonCrossTopPressed));
        map.put(LogitechComponent.BUTTON_CROSS_RIGHT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_RIGHT, buttonCrossRightPressed, buttonCrossRightPressed));
        map.put(LogitechComponent.BUTTON_CROSS_BOTTOM, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_BOTTOM, buttonCrossBottomPressed, buttonCrossBottomPressed));
        map.put(LogitechComponent.BUTTON_CROSS_LEFT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_LEFT, buttonCrossLeftPressed, buttonCrossLeftPressed));
        map.put(LogitechComponent.BUTTON_CROSS_CENTER, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_CENTER, buttonCrossCenterPressed, buttonCrossCenterPressed));
        return map;
    }
}
