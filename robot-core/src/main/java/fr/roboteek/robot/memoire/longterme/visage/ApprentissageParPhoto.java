package fr.roboteek.robot.memoire.longterme.visage;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceDetectionVisage;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDansLImage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Apprendre un visage depuis une photo, plutôt que devant la caméra.
 * <p>
 * Le robot fait connaissance en parlant aux gens ; ceci sert aux cas que ce chemin ne couvre pas —
 * réimporter quelqu'un après un effacement, ou enregistrer une personne qui n'est pas là.
 * <p>
 * Les modèles ONNX sont chargés à la première demande et non au démarrage : ils peuvent manquer
 * sur une installation neuve, et le contexte Spring entier échouerait alors pour une
 * fonctionnalité dont le robot peut très bien se passer. C'est la même précaution que dans
 * l'organe de vision, qui se contente d'un avertissement.
 */
@Component
public class ApprentissageParPhoto {

    private static final Logger logger = LoggerFactory.getLogger(ApprentissageParPhoto.class);

    private static final String MODELE_SFACE = "face_recognition_sface_2021dec.onnx";

    /**
     * Confiance minimale exigée de la détection à l'import.
     * <p>
     * Plus sévère que le 0,8 de la détection courante ({@code OpenCvServiceDetectionVisage}), et
     * ce n'est pas la même question : sur le flux vidéo, rater un visage n'a aucune conséquence,
     * il repassera à l'image suivante. Ici l'empreinte part en base pour de bon — un motif pris à
     * tort pour un visage y resterait, et la reconnaissance le comparerait à tout le monde.
     * <p>
     * Ce score dit « c'est bien un visage », pas « la photo est bonne » : un visage flou peut très
     * bien obtenir 0,95. C'est la netteté qui répond à l'autre question.
     */
    private static final float SCORE_MINIMAL = 0.9f;

    private final VisageConnuRepository visageConnuRepository;

    public ApprentissageParPhoto(VisageConnuRepository visageConnuRepository) {
        this.visageConnuRepository = visageConnuRepository;
    }

    /**
     * Lit une photo et en tire de quoi reconnaître la personne, plus son portrait.
     *
     * @param photo le fichier tel qu'il a été envoyé (JPEG, PNG…)
     * @throws PhotoInexploitable si l'image est illisible, ne montre aucun visage, en montre
     *                            plusieurs, ou si la reconnaissance n'est pas installée
     */
    public Empreinte lire(byte[] photo) {
        Mat image = Imgcodecs.imdecode(new MatOfByte(photo), Imgcodecs.IMREAD_COLOR);
        if (image.empty()) {
            throw new PhotoInexploitable("image illisible");
        }
        try {
            List<VisageDetecte> visages = detection().detecter(image);
            if (visages.isEmpty()) {
                throw new PhotoInexploitable("aucun visage sur la photo");
            }
            // Refusé plutôt qu'arbitré : sur une photo de groupe, deviner lequel des visages est
            // le bon revient à enregistrer quelqu'un sous le nom d'un autre — et le robot
            // appellerait alors cette personne par un prénom qui n'est pas le sien.
            if (visages.size() > 1) {
                throw new PhotoInexploitable(visages.size() + " visages sur la photo, il en faut un seul");
            }
            VisageDetecte visage = visages.getFirst();
            if (visage.score() < SCORE_MINIMAL) {
                throw new PhotoInexploitable("le visage n'est pas assez net pour être sûr que c'en est un");
            }
            // La même porte qu'à la caméra, avec un message pour celui qui a envoyé la photo.
            switch (VisageDansLImage.qualite(image, visage)) {
                case TROP_FLOU -> throw new PhotoInexploitable("photo trop floue pour apprendre ce visage");
                case TROP_DE_PROFIL -> throw new PhotoInexploitable("visage trop de profil, il en faut une prise de face");
                case EXPLOITABLE -> { }
            }
            return new Empreinte(reconnaissance().extraireEmbedding(image, visage),
                    VisageDansLImage.portraitJpeg(image, visage));
        } finally {
            image.release();
        }
    }

    private OpenCvServiceDetectionVisage detection() {
        try {
            return OpenCvServiceDetectionVisage.getInstance();
        } catch (RuntimeException e) {
            throw modelesAbsents(e);
        }
    }

    private OpenCvServiceReconnaissanceVisage reconnaissance() {
        try {
            return OpenCvServiceReconnaissanceVisage.getInstance(visageConnuRepository);
        } catch (RuntimeException e) {
            throw modelesAbsents(e);
        }
    }

    private PhotoInexploitable modelesAbsents(RuntimeException cause) {
        logger.warn("Reconnaissance de visages indisponible (modèles absents de {} ?) : {}",
                Constantes.DOSSIER_VISAGE + File.separator + MODELE_SFACE, cause.getMessage());
        return new PhotoInexploitable("la reconnaissance de visages n'est pas installée sur ce robot");
    }

    /**
     * Ce qu'une photo a donné.
     *
     * @param embedding empreinte SFace
     * @param vignette  portrait recadré en JPEG, {@code null} si le visage touchait le bord
     */
    public record Empreinte(float[] embedding, byte[] vignette) {
    }

    /** La photo ne permet pas d'apprendre ce visage, et le message dit pourquoi à l'utilisateur. */
    public static class PhotoInexploitable extends RuntimeException {
        public PhotoInexploitable(String message) {
            super(message);
        }
    }
}
