/**
 * 
 */
package fr.roboteek.robot.server.service.model.visage;

/**
 * Représente une image d'une animation.
 * @author Nicolas
 *
 */
public class AnimationImage {
	
	/** Identifiant de l'image. */
	private String idImage;

	/** Temps de la pause. */
	private int tempsPause;
	
	public String getIdImage() {
		return idImage;
	}

	public void setIdImage(String idImage) {
		this.idImage = idImage;
	}
	
	public int getTempsPause() {
		return tempsPause;
	}

	public void setTempsPause(int tempsPause) {
		this.tempsPause = tempsPause;
	}

}
