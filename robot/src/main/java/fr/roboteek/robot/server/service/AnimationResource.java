/**
 * 
 */
package fr.roboteek.robot.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fr.roboteek.robot.organes.actionneurs.VisageDoubleBuffering;
import fr.roboteek.robot.organes.actionneurs.util.AnimationImage;
import fr.roboteek.robot.organes.actionneurs.util.AnimationItem;
import fr.roboteek.robot.organes.actionneurs.util.AnimationPause;
import fr.roboteek.robot.server.service.animation.model.Animation;

/**
 * Services Web liés aux animations.
 * @author Nicolas
 *
 */
@Path("/animations")
public class AnimationResource {

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public List<Animation> getListeAnimations() {
		final Map<String, fr.roboteek.robot.organes.actionneurs.util.Animation> mapAnimationsExpressions = VisageDoubleBuffering.getInstance().getMapAnimationsExpressions();
		final List<Animation> listeAnimations = new ArrayList<Animation>();
		// Mapping des données
		if (mapAnimationsExpressions != null && !mapAnimationsExpressions.isEmpty()) {
			final Set<String> listeIdsAnimationsTriee = new TreeSet<String>(mapAnimationsExpressions.keySet());
			for (String idAnimation : listeIdsAnimationsTriee) {
				final fr.roboteek.robot.organes.actionneurs.util.Animation animationSource = mapAnimationsExpressions.get(idAnimation);
				final Animation animationDestination = new Animation();
				animationDestination.setIdAnimation(animationSource.getIdAnimation());
				animationDestination.setLibelleAnimation(animationSource.getLibelleAnimation());
				for (AnimationItem animationItemSource : animationSource.getListeItemsAnimation()) {
					if (animationItemSource instanceof AnimationImage) {
						final AnimationImage animationImageSource = (AnimationImage) animationItemSource;
						final fr.roboteek.robot.server.service.animation.model.AnimationItem animationImageDestination = new fr.roboteek.robot.server.service.animation.model.AnimationItem();
						animationImageDestination.setIdImage(animationImageSource.getIdentifiant());
						animationImageDestination.setType(fr.roboteek.robot.server.service.animation.model.AnimationItem.TYPE_IMAGE);
						animationDestination.getListeItems().add(animationImageDestination);
					} else if (animationItemSource instanceof AnimationPause) {
						final AnimationPause animationPauseSource = (AnimationPause) animationItemSource;
						final fr.roboteek.robot.server.service.animation.model.AnimationItem animationPauseDestination = new fr.roboteek.robot.server.service.animation.model.AnimationItem();
						animationPauseDestination.setTempsPause(animationPauseSource.getTempsPause());
						animationPauseDestination.setType(fr.roboteek.robot.server.service.animation.model.AnimationItem.TYPE_PAUSE);
						animationDestination.getListeItems().add(animationPauseDestination);
					}
				}
				listeAnimations.add(animationDestination);
			}
		}
		return listeAnimations;
	}
	
}
