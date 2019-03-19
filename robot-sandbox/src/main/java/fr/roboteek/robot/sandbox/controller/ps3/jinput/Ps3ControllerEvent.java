package fr.roboteek.robot.sandbox.controller.ps3.jinput;

public class Ps3ControllerEvent extends GamepadEvent {

    private Boolean buttonTrianglePressed = false;
    private Boolean buttonSquarePressed = false;
    private Boolean buttonCrossPressed = false;
    private Boolean buttonRoundPressed = false;
    private Boolean buttonSelectPressed = false;
    private Boolean buttonStartPressed = false;
    private Boolean buttonModePressed = false;
    private Boolean buttonLeft1Pressed = false;
    private Boolean buttonLeft2Pressed = false;
    private Boolean buttonLeftJoystick3Pressed = false;
    private Boolean buttonRight1Pressed = false;
    private Boolean buttonRight2Pressed = false;
    private Boolean buttonRightJoystick3Pressed = false;

    private float buttonAnalogLeft2Value = 0;
    private float buttonAnalogRight2Value = 0;
    private float joystickLeftAxisXValue = 0;
    private float joystickLeftAxisYValue = 0;
    private float joystickRightAxisXValue = 0;
    private float joystickRightAxisYValue = 0;

    public Boolean isButtonTrianglePressed() {
        return buttonTrianglePressed;
    }

    public void setButtonTrianglePressed(Boolean buttonTrianglePressed) {
        this.buttonTrianglePressed = buttonTrianglePressed;
    }

    public Boolean isButtonSquarePressed() {
        return buttonSquarePressed;
    }

    public void setButtonSquarePressed(Boolean buttonSquarePressed) {
        this.buttonSquarePressed = buttonSquarePressed;
    }

    public Boolean isButtonCrossPressed() {
        return buttonCrossPressed;
    }

    public void setButtonCrossPressed(Boolean buttonCrossPressed) {
        this.buttonCrossPressed = buttonCrossPressed;
    }

    public Boolean isButtonRoundPressed() {
        return buttonRoundPressed;
    }

    public void setButtonRoundPressed(Boolean buttonRoundPressed) {
        this.buttonRoundPressed = buttonRoundPressed;
    }

    public Boolean isButtonSelectPressed() {
        return buttonSelectPressed;
    }

    public void setButtonSelectPressed(Boolean buttonSelectPressed) {
        this.buttonSelectPressed = buttonSelectPressed;
    }

    public Boolean isButtonStartPressed() {
        return buttonStartPressed;
    }

    public void setButtonStartPressed(Boolean buttonStartPressed) {
        this.buttonStartPressed = buttonStartPressed;
    }

    public Boolean isButtonModePressed() {
        return buttonModePressed;
    }

    public void setButtonModePressed(Boolean buttonModePressed) {
        this.buttonModePressed = buttonModePressed;
    }

    public Boolean isButtonLeft1Pressed() {
        return buttonLeft1Pressed;
    }

    public void setButtonLeft1Pressed(Boolean buttonLeft1Pressed) {
        this.buttonLeft1Pressed = buttonLeft1Pressed;
    }

    public Boolean isButtonLeft2Pressed() {
        return buttonLeft2Pressed;
    }

    public void setButtonLeft2Pressed(Boolean buttonLeft2Pressed) {
        this.buttonLeft2Pressed = buttonLeft2Pressed;
    }

    public Boolean isButtonLeftJoystick3Pressed() {
        return buttonLeftJoystick3Pressed;
    }

    public void setButtonLeftJoystick3Pressed(Boolean buttonLeftJoystick3Pressed) {
        this.buttonLeftJoystick3Pressed = buttonLeftJoystick3Pressed;
    }

    public Boolean isButtonRight1Pressed() {
        return buttonRight1Pressed;
    }

    public void setButtonRight1Pressed(Boolean buttonRight1Pressed) {
        this.buttonRight1Pressed = buttonRight1Pressed;
    }

    public Boolean isButtonRight2Pressed() {
        return buttonRight2Pressed;
    }

    public void setButtonRight2Pressed(Boolean buttonRight2Pressed) {
        this.buttonRight2Pressed = buttonRight2Pressed;
    }

    public Boolean isButtonRightJoystick3Pressed() {
        return buttonRightJoystick3Pressed;
    }

    public void setButtonRightJoystick3Pressed(Boolean buttonRightJoystick3Pressed) {
        this.buttonRightJoystick3Pressed = buttonRightJoystick3Pressed;
    }

    public float getButtonAnalogLeft2Value() {
        return buttonAnalogLeft2Value;
    }

    public void setButtonAnalogLeft2Value(float buttonAnalogLeft2Value) {
        this.buttonAnalogLeft2Value = buttonAnalogLeft2Value;
    }

    public float getButtonAnalogRight2Value() {
        return buttonAnalogRight2Value;
    }

    public void setButtonAnalogRight2Value(float buttonAnalogRight2Value) {
        this.buttonAnalogRight2Value = buttonAnalogRight2Value;
    }

    public float getJoystickLeftAxisXValue() {
        return joystickLeftAxisXValue;
    }

    public void setJoystickLeftAxisXValue(float joystickLeftAxisXValue) {
        this.joystickLeftAxisXValue = joystickLeftAxisXValue;
    }

    public float getJoystickLeftAxisYValue() {
        return joystickLeftAxisYValue;
    }

    public void setJoystickLeftAxisYValue(float joystickLeftAxisYValue) {
        this.joystickLeftAxisYValue = joystickLeftAxisYValue;
    }

    public float getJoystickRightAxisXValue() {
        return joystickRightAxisXValue;
    }

    public void setJoystickRightAxisXValue(float joystickRightAxisXValue) {
        this.joystickRightAxisXValue = joystickRightAxisXValue;
    }

    public float getJoystickRightAxisYValue() {
        return joystickRightAxisYValue;
    }

    public void setJoystickRightAxisYValue(float joystickRightAxisYValue) {
        this.joystickRightAxisYValue = joystickRightAxisYValue;
    }


    @Override
    public String toString() {
        return "Ps3ControllerEvent{" +
                super.toString() +
                ", buttonTrianglePressed=" + buttonTrianglePressed +
                ", buttonSquarePressed=" + buttonSquarePressed +
                ", buttonCrossPressed=" + buttonCrossPressed +
                ", buttonRoundPressed=" + buttonRoundPressed +
                ", buttonSelectPressed=" + buttonSelectPressed +
                ", buttonStartPressed=" + buttonStartPressed +
                ", buttonModePressed=" + buttonModePressed +
                ", buttonLeft1Pressed=" + buttonLeft1Pressed +
                ", buttonLeft2Pressed=" + buttonLeft2Pressed +
                ", buttonLeftJoystick3Pressed=" + buttonLeftJoystick3Pressed +
                ", buttonRight1Pressed=" + buttonRight1Pressed +
                ", buttonRight2Pressed=" + buttonRight2Pressed +
                ", buttonRightJoystick3Pressed=" + buttonRightJoystick3Pressed +
                ", buttonAnalogLeft2Value=" + buttonAnalogLeft2Value +
                ", buttonAnalogRight2Value=" + buttonAnalogRight2Value +
                ", joystickLeftAxisXValue=" + joystickLeftAxisXValue +
                ", joystickLeftAxisYValue=" + joystickLeftAxisYValue +
                ", joystickRightAxisXValue=" + joystickRightAxisXValue +
                ", joystickRightAxisYValue=" + joystickRightAxisYValue +
                '}';
    }
}
