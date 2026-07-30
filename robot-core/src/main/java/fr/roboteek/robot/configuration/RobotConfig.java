package fr.roboteek.robot.configuration;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface RobotConfig extends Config {

    @Key("language.code")
    @DefaultValue("fr-FR")
    String languageCode();

    /**
     * Name of the the webcam to select.
     *
     * @return the the webcam to select
     */
    @Key("device.webcam.name")
    String webcamName();

    /**
     * Name of the the microphone to select.
     *
     * @return the the microphone to select
     */
    @Key("device.microphone.name")
    String microphoneName();

    /**
     * Durée de silence, en secondes, à partir de laquelle une phrase est considérée terminée.
     * <p>
     * C'est le poste dominant du délai perçu entre la fin de la parole et le texte reconnu : la
     * reconnaissance ne peut pas démarrer (en batch) ou se conclure (en streaming) avant qu'il soit
     * écoulé. Configurable et rechargé à chaud (voir {@code @HotReload} sur cette interface) pour
     * pouvoir chercher le bon compromis sans reconstruire ni redéployer.
     * <p>
     * Le baisser rend le robot plus réactif mais coupe les phrases sur une simple hésitation : le
     * fragment déjà détecté part à la reconnaissance, et la suite est perdue puisque l'écoute est
     * mise en pause pendant la réponse. C'est ce qui avait motivé le passage de 0,6 à 1,2 s.
     *
     * @return la durée de silence marquant la fin d'une phrase, en secondes
     */
    @Key("speech.recognizer.silence.duration.seconds")
    @DefaultValue("1.2")
    double dureeSilenceFinPhraseSecondes();

    /**
     * Indique si l'organe de vision (webcam + serveur Python) doit être démarré.
     * Désactivé par défaut : nécessite une webcam et le serveur Python gRPC (localhost:50051).
     *
     * @return true si la vision est activée
     */
    @Key("robot.capteurs.vision.enabled")
    @DefaultValue("false")
    boolean visionEnabled();
}
