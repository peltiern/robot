package fr.roboteek.robot.memoire;

import org.openimaj.math.geometry.shape.Rectangle;

public class RecognizedFace {

    private int x;
    private int y;
    private int width;
    private int height;

    private String name;

    private FaceLandmarks landmarks;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FaceLandmarks getLandmarks() {
        return landmarks;
    }

    public void setLandmarks(FaceLandmarks landmarks) {
        this.landmarks = landmarks;
    }

    public Rectangle getBounds() {
        return new Rectangle((float) x, (float) y, (float) width, (float) height);
    }


    @Override
    public String toString() {
        return "RecognizedFace{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", name='" + name + '\'' +
                '}';
    }
}
