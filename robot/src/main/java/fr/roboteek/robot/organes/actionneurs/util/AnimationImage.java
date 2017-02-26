/**
 * 
 */
package fr.roboteek.robot.organes.actionneurs.util;

/**
 * Représrente une image d'une animation.
 * @author Nicolas
 *
 */
public class AnimationImage {

	/** Identifiant de l'image. */
	private String identifiant;
	
	/** Temps de la pause. */
	private int tempsPause;
	
	/** Couche dans laquelle est dessinée l'image (1 à 5). */
	private int couche;
	
	/** Zone de la couche dans laquelle est dessinée l'image (0 ou 1). */
	private int zoneCouche;

	public AnimationImage(String identifiant, int tempsPause,  int couche, int zoneCouche) {
		this.identifiant = identifiant;
		this.tempsPause = tempsPause;
		this.couche = couche;
		this.zoneCouche = zoneCouche;
	}

	public String getIdentifiant() {
		return identifiant;
	}

	public int getCouche() {
		return couche;
	}

	public int getZoneCouche() {
		return zoneCouche;
	}

	public int getTempsPause() {
		return tempsPause;
	}

}
