/**
 * 
 */
package fr.roboteek.robot.organes.actionneurs.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.util.ByteArrayBuffer;

import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDisplay;
import com.yoctopuce.YoctoAPI.YDisplayLayer;

/**
 * Représente une animation, c'est à dire une séquence d'images.
 * @author Nicolas
 *
 */
public class Animation {

	private static int nbCouches = 4;

	private static int nbImagesParCouche = 2;

	private static int idxCoucheTransition = 4;

	/**
	 * Map des images de l'animation.
	 * Clé : identifiant de l'image, valeur : l'image
	 */
	private Map<String, AnimationImage> mapImages = new HashMap<String, AnimationImage>();

	/** Séquence des images de l'animation dans l'ordre dans lequel elles seront affichées. */
	private List<AnimationImage> listeImagesAnimation = new ArrayList<AnimationImage>();

	/** Tableau des données des différentes couches (128 * 128 : peut contenir 2 images contenues dans 2 zones différentes (A et B)). */
	private ByteArrayBuffer[] tabDonneesCouches;

	/** Ecran. */
	private YDisplay ecran;

	private int idxCoucheEnCours = -1;

	public Animation(YDisplay ecran) {
		this.ecran = ecran;

		// Création des 4 couches de données
		tabDonneesCouches = new ByteArrayBuffer[nbCouches + 1];
		for (int i = 0; i < nbCouches; i++) {
			tabDonneesCouches[i] = new ByteArrayBuffer(128 * 128);
		}
		// Création de la couche de transition
		tabDonneesCouches[idxCoucheTransition] = new ByteArrayBuffer(128 * 128);
		try {
			this.ecran.get_displayLayer(idxCoucheTransition).drawBitmap(0, 0, 128, tabDonneesCouches[idxCoucheTransition].toByteArray(), 0);
		} catch (YAPI_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void ajouterImage(String identifiantImage, ByteArrayBuffer contenuImage, int tempsPause) {
		// On teste si l'image n'est pas déjà présente
		AnimationImage image = mapImages.get(identifiantImage);
		if (image == null) {
			// L'image n'existe pas : on la crée
			// Calcul de la couche et de la zone dans laquelle dessiner l'image
			final int nbImagesDejaPresentes = mapImages.size();
			final int couche = (int) (nbImagesDejaPresentes / nbImagesParCouche);
			final int zone = nbImagesDejaPresentes % nbImagesParCouche;
			image = new AnimationImage(identifiantImage, tempsPause, couche, zone);
			// Ajout à la map
			mapImages.put(identifiantImage, image);
			// Ajout du contenu de l'image à la couche et la zone correspondante (à la suite)
			tabDonneesCouches[couche].append(contenuImage.toByteArray(), 0, contenuImage.length());
		}
		// Ajout de l'image à la séquence
		listeImagesAnimation.add(image);
	}

	public void preparer() {
		try {
			idxCoucheEnCours = idxCoucheTransition;
			for (int idxCouche = 0; idxCouche < nbCouches; idxCouche++) {

				final YDisplayLayer couche = ecran.get_displayLayer(idxCouche);
				couche.hide();
				if (tabDonneesCouches[idxCouche] != null && tabDonneesCouches[idxCouche].length() > 0) {
					couche.drawBitmap(0, 0, 128, tabDonneesCouches[idxCouche].toByteArray(), 0);
				} else {
					couche.clear();
				}
			}
		} catch (YAPI_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** Joue l'animation sur l'écran. */
	public void jouer() {
		try {
			AnimationImage derniereImage = null;
			for (AnimationImage image : listeImagesAnimation) {
				if (image instanceof AnimationImage) {
					ecran.get_displayLayer(image.getCouche()).setLayerPosition(0, - image.getZoneCouche() * 64, 0);
					if (idxCoucheEnCours != image.getCouche()) {
						ecran.get_displayLayer(image.getCouche()).unhide();
						if (idxCoucheEnCours != -1) {
							ecran.get_displayLayer(idxCoucheEnCours).hide();
						}
					}
					idxCoucheEnCours = image.getCouche();
					derniereImage = image;
					
					// Temps d'affichage de l'image
					Thread.sleep(image.getTempsPause());
				}
			}

			// Copie de la couche de la dernière image sur la couche de transition
			if (derniereImage != null) {
				ecran.copyLayerContent(derniereImage.getCouche(), idxCoucheTransition);
				ecran.get_displayLayer(idxCoucheTransition).setLayerPosition(0, - derniereImage.getZoneCouche() * 64, 0);
			}
			// Affichage de la couche de transition
			ecran.get_displayLayer(idxCoucheTransition).unhide();
			ecran.get_displayLayer(idxCoucheEnCours).hide();
			idxCoucheEnCours = idxCoucheTransition;

		} catch (YAPI_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
