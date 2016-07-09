package fr.roboteek.robot.util.phidget;

/**
 * Interface d'un moteur.
 * @author Java Developer
 */
public interface Motor {
    
    /**
     * Pivote le moteur d'un certain nombre de degrés (positif ou négatif).
     * @param angle angle en degrés 
     */
    void rotate(double angle);
    
    /**
     * Permet au moteur d'avancer.
     */
    void forward();
    
    /**
     * Permet au moteur de reculer.
     */
    void backward();
    
    /**
     * Stoppe le moteur.
     */
    void stop();

}
