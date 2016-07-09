package fr.roboteek.robot.util.phidget;

public interface MotorPositionChangeListener {

    /**
     * Méthode appelée lors d'un changement de position d'un moteur.
     * @param event évènement
     */
    void onPositionchanged(MotorPositionChangeEvent event);
}
