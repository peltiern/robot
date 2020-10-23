package fr.roboteek.robot.server.service.model.visage;

public class Image {
	
	/** Nom. */
	private String nom;
	
	/** Date de modification. */
	private long dateModification;
	
	public Image(String nom, long dateModification) {
		this.nom = nom;
		this.dateModification = dateModification;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public long getDateModification() {
		return dateModification;
	}

	public void setDateModification(long dateModification) {
		this.dateModification = dateModification;
	}

}
