package fr.roboteek.robot.sandbox.deeplearning;

import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.math.geometry.shape.Shape;

/**
 * Objet reconnu par un détecteur.
 */
public class DetectedObject<T> {

    /**
     * Objet détecté.
     */
    private T object;

    private String label;

    /**
     * Contours
     */
    private Rectangle bounds;

    /**
     * Indice de confiance.
     */
    private double confidence;

    public DetectedObject(T object, String label, Rectangle bounds) {
        this(object, label, bounds, 1);
    }

    public DetectedObject(T object, String label, Rectangle bounds, double confidence) {
        this.object = object;
        this.label = label;
        this.bounds = bounds;
        this.confidence = confidence;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
