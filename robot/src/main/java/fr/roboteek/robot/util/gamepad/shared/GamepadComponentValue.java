package fr.roboteek.robot.util.gamepad.shared;

public class GamepadComponentValue <C> {

    private C component;

    private Float oldNumericValue;

    private Float currentNumericValue;

    private Boolean oldPressed;

    private Boolean currentPressed;

    public GamepadComponentValue(C component, Float oldNumericValue, Float currentNumericValue) {
        this.component = component;
        this.oldNumericValue = oldNumericValue;
        this.currentNumericValue = currentNumericValue;
    }

    public GamepadComponentValue(C component, Boolean oldPressed, Boolean currentPressed) {
        this.component = component;
        this.oldPressed = oldPressed;
        this.currentPressed = currentPressed;
    }

    public C getComponent() {
        return component;
    }

    public void setComponent(C component) {
        this.component = component;
    }

    public Float getOldNumericValue() {
        return oldNumericValue;
    }

    public void setOldNumericValue(Float oldNumericValue) {
        this.oldNumericValue = oldNumericValue;
    }

    public Float getCurrentNumericValue() {
        return currentNumericValue;
    }

    public void setCurrentNumericValue(Float currentNumericValue) {
        this.currentNumericValue = currentNumericValue;
    }

    public Boolean getOldPressed() {
        return oldPressed;
    }

    public void setOldPressed(Boolean oldPressed) {
        this.oldPressed = oldPressed;
    }

    public Boolean getCurrentPressed() {
        return currentPressed;
    }

    public void setCurrentPressed(Boolean currentPressed) {
        this.currentPressed = currentPressed;
    }

    @Override
    public String toString() {
        return "GamepadComponentValue{" +
                "component=" + component +
                ", oldNumericValue=" + oldNumericValue +
                ", currentNumericValue=" + currentNumericValue +
                ", oldPressed=" + oldPressed +
                ", currentPressed=" + currentPressed +
                '}';
    }
}
