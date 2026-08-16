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

    /**
     * Cadence maximale de publication du flux vidéo sur le WebSocket, en images par seconde.
     * <p>
     * C'est le débit de production qu'il faut maîtriser, pas la taille du tampon d'envoi :
     * la boucle de capture tourne libre (~30 images/s) et rien, en aval, n'applique de
     * contre-pression au producteur. Si le navigateur ou le WiFi n'écoule pas le flux aussi
     * vite qu'il est produit, les images s'empilent dans le tampon de la session jusqu'à la
     * limite, et Spring ferme la session (« Buffer size ... exceeds the allowed limit »).
     * <p>
     * Rechargé à chaud (voir {@code @HotReload} sur cette interface) : la valeur peut être
     * ajustée en observant les FPS affichés par la webapp, sans reconstruire ni redémarrer.
     *
     * @return la cadence maximale de publication, en images par seconde
     */
    @Key("robot.capteurs.vision.stream.fps")
    @DefaultValue("8")
    int fpsFluxVideo();

    /**
     * Le watchdog peut-il couper les moteurs ? Désactivé, les battements restent relevés et
     * les pastilles d'état de l'interface continuent de fonctionner : seul le déclenchement de
     * l'arrêt d'urgence est supprimé. Utile pour mettre au point un organe sans se faire couper
     * les moteurs, ou pour écarter le watchdog en cas de doute sur un faux positif.
     *
     * @return true si le watchdog est autorisé à déclencher l'arrêt d'urgence
     */
    @Key("robot.watchdog.enabled")
    @DefaultValue("true")
    boolean watchDogActive();

    /**
     * Durée de silence, en secondes, au-delà de laquelle un organe est considéré comme mort.
     * <p>
     * Volontairement large devant les cadences réelles de battement (16 ms pour la manette, 50 ms
     * pour le cou et les yeux, 200 ms pour les chenilles) : une pause du ramasse-miettes ou une
     * pointe de charge sur le Nano ne doit jamais passer pour une panne. Un watchdog qui se
     * déclenche à tort finit débranché — mieux vaut réagir en trois secondes à coup sûr qu'en une
     * seconde de temps en temps à tort.
     * <p>
     * Rechargé à chaud (voir {@code @HotReload} sur cette interface) : réglable sur le robot sans
     * reconstruire ni redémarrer.
     *
     * @return la durée de silence tolérée, en secondes
     */
    @Key("robot.watchdog.silence.seconds")
    @DefaultValue("3.0")
    double watchDogSilenceSecondes();

    /**
     * Qualité de compression JPEG (0-100) des images du flux vidéo.
     * <p>
     * OpenCV compresse à 95 par défaut, ce qui donne des images de 60 à 90 Ko en 640x480 :
     * une fois encodées en base64 (+33 %) puis en JSON, c'est le poste dominant du débit.
     * 60 divise le poids par trois environ pour une perte peu visible à cette résolution.
     * <p>
     * S'applique aussi à l'image transmise au serveur Python de détection, qui consomme le
     * même encodage.
     *
     * @return la qualité JPEG, entre 0 et 100
     */
    @Key("robot.capteurs.vision.stream.jpeg.quality")
    @DefaultValue("60")
    int qualiteJpegFluxVideo();

    /**
     * Durée de présence continue, en secondes, avant de considérer que quelqu'un est vraiment là.
     * <p>
     * Exprimée en durée et non en nombre de cycles : la cadence de la reconnaissance varie avec
     * la charge du Nano et le nombre de visages, un compte de cycles ne voudrait pas dire la même
     * chose d'un moment à l'autre. Le baisser rend le robot plus prompt à aborder les gens, mais
     * le fait réagir à quelqu'un qui ne fait que passer devant la caméra.
     *
     * @return la durée de présence à confirmer, en secondes
     */
    @Key("robot.presence.confirmation.seconds")
    @DefaultValue("1.5")
    double dureeConfirmationPresenceSecondes();

    /**
     * Durée d'absence, en secondes, au-delà de laquelle on considère la personne repartie.
     * <p>
     * C'est l'hystérésis qui absorbe le clignotement de la détection : mesuré sur le robot, un
     * visage parfaitement immobile disparaît et réapparaît par trous de 100 à 250 ms, une seule
     * frame ratée par YuNet suffisant à le faire s'évanouir. Sans ce délai, la même personne
     * serait « repartie puis revenue » six fois en quinze secondes, et saluée à chaque fois.
     *
     * @return la durée d'absence tolérée avant de considérer la personne partie, en secondes
     */
    @Key("robot.presence.absence.seconds")
    @DefaultValue("4.0")
    double dureeAbsenceAvantDepartSecondes();

    /**
     * Délai minimal, en secondes, entre deux rencontres déclenchées pour la même personne.
     * <p>
     * Second garde-fou, indépendant du précédent : même si quelqu'un sort réellement du champ et
     * revient, le robot ne doit pas rejouer les retrouvailles. Deux minutes par défaut, le temps
     * qu'une vraie absence se distingue d'un aller-retour à la cuisine.
     *
     * @return la temporisation entre deux rencontres d'une même personne, en secondes
     */
    @Key("robot.presence.rencontre.temporisation.seconds")
    @DefaultValue("120.0")
    double temporisationEntreRencontresSecondes();
}
