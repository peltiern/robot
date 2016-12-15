/**
 * 
 */
package fr.roboteek.robot.organes.actionneurs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.util.ByteArrayBuffer;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDisplay;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.util.Animation;
import fr.roboteek.robot.systemenerveux.event.ExpressionVisageEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import net.engio.mbassy.listener.Handler;

/**
 * @author Nicolas
 *
 */
public class VisageDoubleBuffering extends AbstractOrgane {
	
	/** Instance. */
	private static VisageDoubleBuffering instance;

	/** Largeur de l'écran. */
	private static final int LARGEUR_ECRAN = 128;

	/** Hauteur de l'écran. */
	private static final int HAUTEUR_ECRAN = 64;

	/** Map des expressions (animations). */
	private Map<String, Animation> mapAnimationsExpressions = new LinkedHashMap<String, Animation>();

	/** Identifiant de l'expression en cours. */
	private String idAnimationExpressionEnCours;

	/** Ecran. */
	private YDisplay ecran;
	
	
	private VisageDoubleBuffering() {
		// TODO Auto-generated constructor stub
	}
	
	public static VisageDoubleBuffering getInstance() {
		if (instance == null) {
			instance = new VisageDoubleBuffering();
		}
		return instance;
	}

	@Override
	public void initialiser() {
		try {
			// Lancement du VirtualHub
			Process p = Runtime.getRuntime().exec("C:/Yoctopuce/VirtualHub/VirtualHub.exe");
			//			p.waitFor();
			// setup the API to use local VirtualHub
			YAPI.RegisterHub("127.0.0.1");
		} catch (YAPI_Exception ex) {
			System.out.println("Cannot contact VirtualHub on 127.0.0.1 (" + ex.getLocalizedMessage() + ")");
			System.out.println("Ensure that the VirtualHub application is running");
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Recherche de l'écran
		ecran = YDisplay.FirstDisplay();
		
		try {
			ecran.resetAll();
		} catch (YAPI_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Chargement des images
		chargerImages();
		
		jouerExpression("01_allumer");

	}

	@Override
	public void arreter() {
		// TODO Auto-generated method stub

	}

	/**
	 * Intercepte les évènements pour modifier les expressions du visage.
	 * @param paroleEvent évènement pour lire du texte
	 */
	@Handler
	public void handleExpressionVisageEvent(ExpressionVisageEvent expressionVisageEvent) {
		if (expressionVisageEvent.getExpression() != null && !expressionVisageEvent.getExpression().trim().equals("")) {
			jouerExpression(expressionVisageEvent.getExpression());
		}
	}

	/**
	 * Intercepte les évènements pour modifier les expressions du visage.
	 * @param paroleEvent évènement pour lire du texte
	 */
	@Handler
	public void handleReconnaissanceVocaleEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
		if (reconnaissanceVocaleEvent.getTexteReconnu() != null && !reconnaissanceVocaleEvent.getTexteReconnu().trim().equals("")) {
			jouerExpression(reconnaissanceVocaleEvent.getTexteReconnu().toLowerCase());
		}
	}

	private void chargerImages() {
		// Lecture de l'ensemble des images du dossier pour les transformer en tableau de bytes lisibles par l'écran
		final Map<String, ByteArrayBuffer> mapImages = new HashMap<String, ByteArrayBuffer>();
		final File dossierImage = new File(Constantes.DOSSIER_VISAGE + File.separator + "images");
		if (dossierImage.exists()) {
			final FileFilter filtreBmp = new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isFile() && pathname.getName().endsWith(".png");
				}

			};
			for (File fichier : dossierImage.listFiles(filtreBmp)) {
				mapImages.put(FilenameUtils.removeExtension(fichier.getName()), getDataFromImage(fichier));
			}
		}

		// Création des animations des expressions à partir du fichier de propriétés
		Properties fichierExpressions = new Properties();
		try {
			fichierExpressions.load(new FileInputStream(Constantes.DOSSIER_VISAGE + File.separator + "expressions.properties"));
			for (Object cleIdAnimation : fichierExpressions.keySet()) {
				final String idAnimation = (String) cleIdAnimation;
				final String chaineAnimation = fichierExpressions.getProperty(idAnimation);
				final String[] proprietesAnimation = chaineAnimation.split("\\|");
				final String libelleAnimation = proprietesAnimation[0];
				final String[] itemsAnimation = proprietesAnimation[1].split(",");

				// Création de l'animation
				final Animation animation = new Animation(idAnimation, libelleAnimation, ecran);

				// Ajout des images à l'animation
				if (itemsAnimation != null && itemsAnimation.length != 0) {
					for (String item : itemsAnimation) {
						// Récupération du type de l'item et de la valeur
						final String[] typeValeurItem = item.split(":");
						if (typeValeurItem != null && typeValeurItem.length != 0) {
							final String typeItem = typeValeurItem[0];
							if (typeItem.equals("I")) {
								// Image
								final String nomImage = typeValeurItem[1];
								animation.ajouterImage(nomImage, mapImages.get(nomImage));
							} else if (typeItem.equals("P")) {
								// Pause
								final int tempsPause = Integer.valueOf(typeValeurItem[1]);
								animation.ajouterPause(tempsPause);
							}
						}
					}
				}
				mapAnimationsExpressions.put(idAnimation, animation);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ByteArrayBuffer getDataFromImage(File image) {

		final int bytesPerLines = LARGEUR_ECRAN / 8;
		final ByteArrayBuffer imageByteArray = new ByteArrayBuffer(HAUTEUR_ECRAN * bytesPerLines);

		try {
			final FImage fImage = ImageUtilities.readF(image);
			String valByte = "";
			if (image != null && fImage.getWidth() <= LARGEUR_ECRAN && fImage.getHeight() <= HAUTEUR_ECRAN) {
				for (int j = 0; j < HAUTEUR_ECRAN; j++) {
					for (int i = 0; i < LARGEUR_ECRAN; i++) {
						// Si pixel noir ==> 0
						if (fImage.getPixelNative(i, j) == 0) {
							valByte += "0";
						} else {
							valByte += "1";
						}

						// On ajoute l'octet dans le tableau pour le dernier bit
						if (i % 8 == 7) {
							//[j * bytesPerLines + ((i + 1) - 8) / 8]
							imageByteArray.append(Integer.parseInt(valByte, 2));
							valByte = "";
						}
					}
				}
			}
			return imageByteArray;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private void jouerExpression(String nomExpression) {
		final Animation animationExpression = mapAnimationsExpressions.get(nomExpression);

		if (animationExpression != null) {

			// Préparation de l'animation si l'animation précédente est différente
			if (idAnimationExpressionEnCours == null || !idAnimationExpressionEnCours.equals(nomExpression)) {
				animationExpression.preparer();
			}

			idAnimationExpressionEnCours = nomExpression;

			// Lecture de l'animation
			animationExpression.jouer();

		}

	}

	public static void main(String[] args) {
		final VisageDoubleBuffering visage = new VisageDoubleBuffering();
		visage.initialiser();
	}

	public Map<String, Animation> getMapAnimationsExpressions() {
		return mapAnimationsExpressions;
	}

}
