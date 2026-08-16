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
     * Trou maximal toléré, en secondes, <b>tant que la venue n'est pas encore confirmée</b> : au
     * delà, le décompte de présence repart de zéro.
     * <p>
     * Sans lui, la tolérance de {@link #dureeAbsenceAvantDepartSecondes()} — 4 s, faite pour ne pas
     * croire quelqu'un parti — s'appliquait aussi à la confirmation : des perceptions éparses,
     * espacées de trois secondes, finissaient par cumuler la durée exigée comme si elles avaient été
     * continues. Le robot pouvait donc aborder quelqu'un qu'il n'avait fait qu'entrevoir.
     * <p>
     * Ce que ce seuil garantit : plusieurs visages vus <b>à la suite</b> avant de décider quoi que
     * ce soit, inconnu comme connu. Une seconde laisse passer le clignotement de la détection
     * (trous de 100 à 250 ms) sans laisser passer une apparition intermittente.
     *
     * @return le trou maximal toléré pendant la phase de confirmation, en secondes
     */
    @Key("robot.presence.continuite.seconds")
    @DefaultValue("1.0")
    double dureeContinuitePresenceSecondes();

    /**
     * Fenêtre glissante, en secondes, sur laquelle un inconnu et les personnes connues sont
     * départagés au nombre de fois qu'ils ont été vus.
     * <p>
     * Un même visage sort tantôt reconnu, tantôt inconnu — la reconnaissance oscille autour de son
     * seuil. Les deux lectures accumulaient jusqu'ici leur présence chacune de leur côté, si bien
     * que l'inconnu pouvait se confirmer alors que la personne était reconnue trois fois sur cinq.
     * Sur cette fenêtre, c'est la lecture majoritaire qui l'emporte.
     * <p>
     * Trop courte, elle ne départage rien ; trop longue, elle fait traîner l'abord d'un vrai
     * inconnu, dont le compte doit d'abord dépasser celui des connus.
     *
     * @return la fenêtre d'arbitrage entre identités, en secondes
     */
    @Key("robot.presence.fenetre.seconds")
    @DefaultValue("3.0")
    double fenetreArbitragePresenceSecondes();

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

    /**
     * Délai minimal, en secondes, avant qu'une activité qui vient de se terminer puisse être
     * réclamée à nouveau.
     * <p>
     * Garde-fou du cerveau, distinct de {@link #temporisationEntreRencontresSecondes()} qui
     * raisonne, lui, par personne : celui-ci protège de toute demande insistante, d'où qu'elle
     * vienne. Le cas qu'il traite est celui d'une activité qui tourne court — quelqu'un qu'on
     * aborde et qui ne répond pas — et dont la cause est toujours là quand elle se termine :
     * sans délai, elle repartirait aussitôt, en boucle.
     *
     * @return la temporisation avant relance d'une même activité, en secondes
     */
    @Key("robot.activite.temporisation.seconds")
    @DefaultValue("30.0")
    double delaiAvantRelanceActiviteSecondes();

    /**
     * Indique si le robot tourne la tête vers les visages qu'il perçoit.
     *
     * @return true si le regard est activé
     */
    @Key("robot.regard.enabled")
    @DefaultValue("true")
    boolean regardEnabled();

    /**
     * Champ de vision horizontal de la webcam, en degrés.
     * <p>
     * C'est lui qui convertit un écart en pixels en un angle de rotation du cou : une valeur
     * fausse ne fait pas regarder à côté, elle fait sous-corriger ou dépasser. À mesurer une fois
     * (repère à distance connue) plutôt qu'à croire sur parole du fabricant.
     *
     * @return le champ horizontal de la caméra, en degrés
     */
    @Key("robot.regard.camera.champ.horizontal.degres")
    @DefaultValue("60.0")
    double champHorizontalCameraDegres();

    /**
     * Écart angulaire en deçà duquel le robot considère qu'il regarde déjà la personne.
     * <p>
     * Sans cette zone morte, la moindre imprécision de la boîte englobante — qui respire d'une
     * image à l'autre — suffirait à faire bouger la tête en permanence.
     *
     * @return la zone morte du regard, en degrés
     */
    @Key("robot.regard.zone.morte.degres")
    @DefaultValue("5.0")
    double zoneMorteRegardDegres();

    /**
     * Combien d'unités de commande du cou valent un degré d'écart vu par la caméra.
     * <p>
     * <b>Seul et unique réglage entre l'écart perçu et la consigne envoyée</b>, et c'est
     * délibéré : le cou se commande en position absolue (il lit où il est et ajoute l'angle), la
     * cible est donc calculée directement, pas approchée. Corriger volontairement moins que
     * l'écart n'apporte rien qu'une suite de petits mouvements — c'est exactement ce qu'un
     * amortissement, essayé le 2026-08-12, a produit.
     * <p>
     * Mesuré sur le robot le 2026-08-12, sur des relevés où la personne ne bougeait pas : une
     * unité de commande déplace le regard d'<b>un degré apparent</b>. Deux causes s'y multiplient
     * sans qu'un essai les sépare — un champ de vision réel différent du déclaré, et une unité de
     * servo qui ne vaut pas forcément un degré de tête. Le produit, lui, se mesure : commander un
     * angle connu et regarder de combien l'écart perçu a changé, <b>une fois la tête
     * stabilisée</b>. Mesurer pendant qu'elle traverse surestime largement le déplacement, et
     * c'est ce qui avait d'abord fait croire à un facteur 2.
     *
     * @return le nombre d'unités de commande panoramique par degré d'écart perçu
     */
    @Key("robot.regard.panoramique.commande.par.degre.vu")
    @DefaultValue("1.0")
    double commandePanoramiqueParDegreVu();

    /**
     * Idem pour l'inclinaison — <b>et la valeur n'est pas la même</b>.
     * <p>
     * Mesuré le 2026-08-12 : une unité de commande déplace le regard d'environ <b>trois degrés</b>
     * en inclinaison (quatre relevés concordants : 6,3 commandés pour 22,6° vus), contre un seul
     * en panoramique. Les deux servos n'entraînent pas la tête avec le même bras de levier, et
     * une échelle commune faisait donc dépasser lourdement sur cet axe.
     * <p>
     * 0,3 plutôt que le tiers exact, et c'est délibéré : sous-corriger ne coûte qu'un mouvement de
     * plus, sur-corriger fait osciller la tête sans fin.
     *
     * @return le nombre d'unités de commande d'inclinaison par degré d'écart perçu
     */
    @Key("robot.regard.inclinaison.commande.par.degre.vu")
    @DefaultValue("0.3")
    double commandeInclinaisonParDegreVu();

    /**
     * Délai minimal entre deux corrections du regard, en secondes.
     * <p>
     * Le temps que le servo arrive et que la caméra — portée par la tête — voie le résultat.
     * Trop court, les corrections s'empilent sur une image d'avant le mouvement et la tête part
     * trop loin.
     *
     * @return la temporisation entre deux corrections du regard, en secondes
     */
    @Key("robot.regard.temporisation.seconds")
    @DefaultValue("1.0")
    double temporisationRegardSecondes();

    /**
     * Inverse le sens de rotation panoramique pour le regard.
     * <p>
     * Étalonnage à faire <b>une fois</b> : le sens dépend du montage du servo, que rien dans le
     * code ne permet de deviner (les deux conventions de {@code MouvementCouEvent} se
     * contredisent d'ailleurs). Vérifié sur le robot le 2026-08-12 : {@code false} est le bon
     * sens. Rechargé à chaud, sans reconstruire l'image.
     *
     * @return true si le sens de rotation panoramique doit être inversé
     */
    @Key("robot.regard.panoramique.sens.inverse")
    @DefaultValue("false")
    boolean regardPanoramiqueSensInverse();

    /**
     * Inverse le sens de rotation en inclinaison pour le regard.
     * <p>
     * Réglage indépendant du panoramique : les deux servos sont montés séparément, et rien ne
     * garantit qu'ils partagent la même convention. Si le robot lève la tête quand il devrait la
     * baisser, passer à {@code true}.
     *
     * @return true si le sens de rotation en inclinaison doit être inversé
     */
    @Key("robot.regard.inclinaison.sens.inverse")
    @DefaultValue("false")
    boolean regardInclinaisonSensInverse();
}
