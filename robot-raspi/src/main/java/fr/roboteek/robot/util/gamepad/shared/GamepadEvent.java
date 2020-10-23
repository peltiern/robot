package fr.roboteek.robot.util.gamepad.shared;

import java.util.List;
import java.util.Map;

public abstract class GamepadEvent <C> {

    private List<C> modifiedComponents;

    private Map<C, GamepadComponentValue<C>> mapValues;

    public GamepadEvent(List<C> modifiedComponents, Map<C, GamepadComponentValue<C>> mapValues) {
        this.modifiedComponents = modifiedComponents;
        this.mapValues = mapValues;
    }

    public List<C> getModifiedComponents() {
        return modifiedComponents;
    }

    public void setModifiedComponents(List<C> modifiedComponents) {
        this.modifiedComponents = modifiedComponents;
    }

    public Map<C, GamepadComponentValue<C>> getMapValues() {
        return mapValues;
    }

    public void setMapValues(Map<C, GamepadComponentValue<C>> mapValues) {
        this.mapValues = mapValues;
    }


    @Override
    public String toString() {
        return "GamepadEvent{" +
                "modifiedComponents=" + modifiedComponents +
                ", mapValues=" + mapValues +
                '}';
    }
}
