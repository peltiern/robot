package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.organes.actionneurs.roues.Chassis;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Evènement envoyé par les encodeurs des roues.
 */
@AllArgsConstructor
@Getter
public class EncodeurRoueEvent extends RobotEvent {

    public static final String EVENT_TYPE = "encodeur-roue";

    private long positionEncodeurGauche;

    private long positionEncodeurDroit;
}
