package fr.roboteek.robot.sandbox.controller.ps3.jinput;

public abstract class GamepadEvent <C extends GamepadComponent> {

    private C component;

    private Float oldNumericValue;

    private Float newNumericValue;

    private Boolean oldPressed;

    private Boolean newPressed;

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

    public Float getNewNumericValue() {
        return newNumericValue;
    }

    public void setNewNumericValue(Float newNumericValue) {
        this.newNumericValue = newNumericValue;
    }

    public Boolean getOldPressed() {
        return oldPressed;
    }

    public void setOldPressed(Boolean oldPressed) {
        this.oldPressed = oldPressed;
    }

    public Boolean getNewPressed() {
        return newPressed;
    }

    public void setNewPressed(Boolean newPressed) {
        this.newPressed = newPressed;
    }


    @Override
    public String toString() {
        return "GamepadEvent{" +
                "component=" + component +
                ", oldNumericValue=" + oldNumericValue +
                ", newNumericValue=" + newNumericValue +
                ", oldPressed=" + oldPressed +
                ", newPressed=" + newPressed +
                '}';
    }
}
