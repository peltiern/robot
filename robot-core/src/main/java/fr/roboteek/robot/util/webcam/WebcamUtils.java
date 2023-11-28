package fr.roboteek.robot.util.webcam;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classe utilitaire pour la gestion des webcams.
 * Cette classe nécessite que l'application "v4l2-ctl" soit installée. Voir le fichier d'installation.
 */
public class WebcamUtils {


    /**
     * Récupération d'une webcam par une partie de son nom.
     * @param nomWebcamRecherchee nom total ou partiel de la webcam recherchée
     * @return l'objet {@link VideoCapture} correspondant à la webcam, null si non trouvée
     * @throws IOException exception en cas d'erreur lors de la recherche de la webcam
     */
    public static VideoCapture getWebcamCaptureParNom(String nomWebcamRecherchee) throws IOException {
        // Recherche des liens symboliques de la webcam demandée
        List<String> liensSymboliquesWebcam = getLiensSymboliquesWebcam(nomWebcamRecherchee);

        VideoCapture capture = null;
        if (CollectionUtils.isNotEmpty(liensSymboliquesWebcam)) {
            for (String lienSymbolique : liensSymboliquesWebcam) {
                capture = new VideoCapture(lienSymbolique);
                if (capture.isOpened()) {
                    // Webcam trouvée
                    break;
                }
            }
        }

        return capture;
    }

    private static List<String> getLiensSymboliquesWebcam(String nomWebcamRecherchee) throws IOException {
        List<String> liensSymboliquesWebcam = new ArrayList<>();
        try {
            // Si la webcam n'est pas la dernière parmi les webcams
            ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "v4l2-ctl --list-devices | grep -A 10 \"" + nomWebcamRecherchee + "\" | grep -B 10 \"usb\" | grep -o \"/dev/video[0-9]*\"");
            Process process = builder.start();
            String retour = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(retour)) {
                liensSymboliquesWebcam = Arrays.asList(retour.split("\n"));
            }

            // Peut-être que la webcam est la dernière dans la liste
            if (CollectionUtils.isEmpty(liensSymboliquesWebcam)) {
                builder = new ProcessBuilder("/bin/sh", "-c", "v4l2-ctl --list-devices | grep -A 10 \"" + nomWebcamRecherchee + "\" | grep -o \"/dev/video[0-9]*\"");
                process = builder.start();
                retour = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                if (StringUtils.isNotBlank(retour)) {
                    liensSymboliquesWebcam = Arrays.asList(retour.split("\n"));
                }
            }

        } catch (IOException e) {
            throw e;
        }
        return liensSymboliquesWebcam;
    }
}
