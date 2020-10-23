/**
 * 
 * 
 */
package fr.roboteek.robot.server.service.visage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.builder.CompareToBuilder;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.server.service.model.visage.Animation;
import fr.roboteek.robot.server.service.model.visage.AnimationImage;

/**
 * Services Web liés aux animations.
 * @author Nicolas
 *
 */
@Path("/visage/animations")
public class AnimationResource {

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public List<Animation> getListeAnimations() {

		final List<Animation> listeAnimations = new ArrayList<Animation>();

		Properties fichierExpressions = new Properties();
		try {
			fichierExpressions.load(new FileInputStream(Constantes.DOSSIER_VISAGE + File.separator + "expressions.properties"));
			for (Object cleIdAnimation : fichierExpressions.keySet()) {
				final String idAnimation = (String) cleIdAnimation;
				final String chaineAnimation = fichierExpressions.getProperty(idAnimation);
				final String[] proprietesAnimation = chaineAnimation.split("\\|");
				final String libelleAnimation = proprietesAnimation[0];

				// Création de l'animation
				final Animation animation = new Animation();
				animation.setIdAnimation(idAnimation);
				animation.setLibelleAnimation(libelleAnimation);

				// Ajout des images à l'animation
				final List<AnimationImage> listeImages = new ArrayList<AnimationImage>();
				if (proprietesAnimation.length > 1 && proprietesAnimation[1] != null && !proprietesAnimation[1].equals("")) {
					final String[] imagesAnimation = proprietesAnimation[1].split(",");
					if (imagesAnimation != null && imagesAnimation.length != 0) {
						for (String image : imagesAnimation) {
							final AnimationImage animationImage = new AnimationImage();
							// Récupération de l'identifiant de l'image et du temps
							final String[] idImageTemps = image.split(":");
							if (idImageTemps != null && idImageTemps.length != 0) {
								animationImage.setIdImage(idImageTemps[0]);
								animationImage.setTempsPause(Integer.valueOf(idImageTemps[1]));
							}
							listeImages.add(animationImage);
						}
					}
				}
				animation.setListeImages(listeImages);

				listeAnimations.add(animation);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Tri par code
		Collections.sort(listeAnimations, new Comparator<Animation>() {
			@Override
			public int compare(Animation o1, Animation o2) {
				return new CompareToBuilder().append(o1.getIdAnimation(), o2.getIdAnimation()).toComparison();
			}
		});

		return listeAnimations;
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	public Response creerAnimation(Animation animation) {
		return sauvegarderAnimation(animation);
	}

	private Response sauvegarderAnimation(Animation animation) {
		Properties fichierExpressions = new Properties();
		try {
			fichierExpressions.load(new FileInputStream(Constantes.DOSSIER_VISAGE + File.separator + "expressions.properties"));

			if (animation == null || animation.getIdAnimation() == null || animation.equals("")) {
				throw new RuntimeException("Le code de l'animation n'est pas renseigné");
			}

			// Construction de la valeur
			final StringBuffer valeurAnimation = new StringBuffer();
			// Libellé
			valeurAnimation.append(animation.getLibelleAnimation());
			valeurAnimation.append("|");
			// Images
			final StringBuffer valeurListeImages = new StringBuffer("");
			if (animation.getListeImages() != null && !animation.getListeImages().isEmpty()) {
				for (AnimationImage animationImage : animation.getListeImages()) {
					if (valeurListeImages.length() > 0) {
						valeurListeImages.append(",");
					}
					valeurListeImages.append(animationImage.getIdImage());
					valeurListeImages.append(":");
					valeurListeImages.append(animationImage.getTempsPause());
				}
			}
			valeurAnimation.append(valeurListeImages);

			// Ajout de la propriété
			fichierExpressions.setProperty(animation.getIdAnimation(), valeurAnimation.toString());

			fichierExpressions.store(new FileOutputStream(Constantes.DOSSIER_VISAGE + File.separator + "expressions.properties"), "Liste des expressions du robot");

			return Response.ok(animation.getIdAnimation()).build();

		} catch (FileNotFoundException e) {
			throw new RuntimeException("Le fichier des expressions n'existe pas");
		} catch (IOException e) {
			throw new RuntimeException("Le fichier des expressions n'existe pas");
		}
	}

}
