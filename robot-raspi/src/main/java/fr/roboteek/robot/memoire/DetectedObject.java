package fr.roboteek.robot.memoire;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import fr.roboteek.robot.util.commons.geometry.Rectangle;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE
)
public class DetectedObject extends Rectangle {

    private String name;

    public DetectedObject(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public DetectedObject() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
