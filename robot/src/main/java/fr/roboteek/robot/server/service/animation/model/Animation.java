package fr.roboteek.robot.server.service.animation.model;

import java.util.ArrayList;
import java.util.List;

public class Animation {

	private String idAnimation;
	
	private String libelleAnimation;
	
	private List<AnimationItem> listeItems;

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

	public List<AnimationItem> getListeItems() {
		if (listeItems == null) {
			listeItems = new ArrayList<AnimationItem>();
		}
		return listeItems;
	}

	public void setListeItems(List<AnimationItem> listeItems) {
		this.listeItems = listeItems;
	}
}
