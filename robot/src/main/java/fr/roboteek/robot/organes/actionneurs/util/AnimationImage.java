/**
 * 
 */
package fr.roboteek.robot.organes.actionneurs.util;

/**
 * Représrente une image d'une animation.
 * @author Nicolas
 *
 */
public class AnimationImage extends AnimationItem {

	/** Identifiant de l'image. */
	private String identifiant;
	
	/** Couche dans laquelle est dessinée l'image (1 à 5). */
	private int couche;
	
	/** Zone de la couche dans laquelle est dessinée l'image (0 ou 1). */
	private int zoneCouche;

	public AnimationImage(String identifiant, int couche, int zoneCouche) {
		this.identifiant = identifiant;
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

}
