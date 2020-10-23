package fr.roboteek.robot.server.service.model.visage;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Animation {

	private String idAnimation;
	
	private String libelleAnimation;
	
	private List<AnimationImage> listeImages;

	public String getIdAnimation() {
		return idAnimation;
	}

	public void setIdAnimation(String idAnimation) {
		this.idAnimation = idAnimation;
	}

	public String getLibelleAnimation() {
		return libelleAnimation;
	}

	public void setLibelleAnimation(String libelleAnimation) {
		this.libelleAnimation = libelleAnimation;
	}

	public List<AnimationImage> getListeImages() {
		if (listeImages == null) {
			listeImages = new ArrayList<AnimationImage>();
		}
		return listeImages;
	}

	public void setListeImages(List<AnimationImage> listeImages) {
		this.listeImages = listeImages;
	}
}
