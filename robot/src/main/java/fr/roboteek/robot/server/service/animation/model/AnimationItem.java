/**
 * 
 */
package fr.roboteek.robot.server.service.animation.model;

/**
 * Représente un item d'une animation.
 * @author Nicolas
 *
 */
public class AnimationItem {
	
	public static final String TYPE_IMAGE = "I";
	
	public static final String TYPE_PAUSE = "P";
	
	private String type;
	
	/** Identifiant de l'image. */
	private String idImage;

	/** Temps de la pause. */
	private int tempsPause;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
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
