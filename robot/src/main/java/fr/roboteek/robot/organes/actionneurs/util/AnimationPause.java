/**
 * 
 */
package fr.roboteek.robot.organes.actionneurs.util;

/**
 * Représente une pause dans une animation.
 * @author Nicolas
 *
 */
public class AnimationPause extends AnimationItem {

	/** Temps de la pause. */
	private int tempsPause;

	public AnimationPause(int tempsPause) {
		this.tempsPause = tempsPause;
	}

	public int getTempsPause() {
		return tempsPause;
	}

	public void setTempsPause(int tempsPause) {
		this.tempsPause = tempsPause;
	}
}
