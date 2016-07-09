package fr.roboteek.robot.util.phidget;

/**
 * Evènement de changement de position d'un moteur.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MotorPositionChangeEvent {
    
    /** Source de l'évènement. */
    private Motor source;
    
    /** Nouvelle position. */
    private double position;

    public MotorPositionChangeEvent(Motor source, double position) {
        this.source = source;
        this.position = position;
    }

    public Motor getSource() {
        return source;
    }

    public double getPosition() {
        return position;
    }

}
