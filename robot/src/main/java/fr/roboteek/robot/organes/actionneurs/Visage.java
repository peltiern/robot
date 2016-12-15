/**
 * 
 */
package fr.roboteek.robot.organes.actionneurs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.util.ByteArrayBuffer;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDisplay;
import com.yoctopuce.YoctoAPI.YDisplayLayer;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.ExpressionVisageEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import net.engio.mbassy.listener.Handler;

/**
 * @author Nicolas
 *
 */
public class Visage extends AbstractOrgane {

	/** Largeur de l'écran. */
	private static final int LARGEUR_ECRAN = 128;

	/** Hauteur de l'écran. */
	private static final int HAUTEUR_ECRAN = 64;

	/** Map de l'ensemble des images d'expressions disponibles. */
	private Map<String, ByteArrayBuffer> mapImages = new HashMap<String, ByteArrayBuffer>();

	/** Map des séquences des expressions. */
	private Map<String, List<ByteArrayBuffer>> mapSequencesExpressions = new HashMap<String, List<ByteArrayBuffer>>();

	/** Ecran. */
	private YDisplay ecran;

	/** Couche d'affichage de l'écran. */
	private YDisplayLayer couche0;

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

		// Chargement des images
		chargerImages();

		// Recherche de l'écran
		ecran = YDisplay.FirstDisplay();
		try {
			couche0 = ecran.get_displayLayer(0);
		} catch (YAPI_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

		// Récupération des expressions à partir du fichier de propriété
		Properties fichierExpressions = new Properties();
		try {
			fichierExpressions.load(new FileInputStream(Constantes.DOSSIER_VISAGE + File.separator + "expressions.properties"));
			for (Object cleNomExpression : fichierExpressions.keySet()) {
				final String nomExpression = (String) cleNomExpression;
				final String chaineSequence = fichierExpressions.getProperty(nomExpression);
				final String[] imagesSequence = chaineSequence.split(",");
				final List<ByteArrayBuffer> listeImagesSequence = new ArrayList<ByteArrayBuffer>();
				if (imagesSequence != null && imagesSequence.length != 0) {
					for (String nomImage : imagesSequence) {
						listeImagesSequence.add(mapImages.get(nomImage));
					}
				}
				mapSequencesExpressions.put(nomExpression, listeImagesSequence);
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
		try {
			// Récupération de la liste des images de l'expression
			final List<ByteArrayBuffer> listeImagesExpression = mapSequencesExpressions.get(nomExpression);
			if (listeImagesExpression != null) {
				for (ByteArrayBuffer imageExpression : listeImagesExpression) {
					couche0.drawBitmap(0, 0, LARGEUR_ECRAN, imageExpression.buffer(), 0);
				}
			}
		} catch (YAPI_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		final Visage visage = new Visage();
		visage.initialiser();
	}

}
