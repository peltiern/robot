package fr.roboteek.robot.util.commons.geometry;

import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.commons.geometry.euclidean.twod.shape.Parallelogram;
import org.apache.commons.numbers.core.Precision;

public class Rectangle {

    private int x;
    private int y;
    private int width;
    private int height;

    private Parallelogram bounds;

    public Rectangle() {
    }

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        bounds = Parallelogram.axisAligned(Vector2D.of(x, y), Vector2D.of(x + width, y + height), Precision.doubleEquivalenceOfEpsilon(Precision.EPSILON));
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Parallelogram getBounds() {
        if (bounds == null) {
            bounds = Parallelogram.axisAligned(Vector2D.of(x, y), Vector2D.of(x + width, y + height), Precision.doubleEquivalenceOfEpsilon(Precision.EPSILON));
        }
        return bounds;
    }

    public Vector2D getCentroid() {
        return getBounds().getCentroid();
    }


}
