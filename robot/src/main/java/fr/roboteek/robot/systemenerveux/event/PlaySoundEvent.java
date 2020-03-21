package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.organes.actionneurs.RobotSound;

/**
 * Evènement pour lire un fichier audio.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class PlaySoundEvent extends RobotEvent {

	public static final String EVENT_TYPE = "PlaySound";

    /** Texte à dire. */
    private RobotSound sound;

	public PlaySoundEvent() {
		super(EVENT_TYPE);
	}

    public RobotSound getSound() {
        return sound;
    }

    public void setSound(RobotSound sound) {
        this.sound = sound;
    }
}
