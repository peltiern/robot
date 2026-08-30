package fr.roboteek.robot.configuration;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

/**
 * Les réglages du robot, lus dans {@code $ROBOT_HOME/configuration/robot.properties} et
 * <b>rechargés à chaud</b> : tout ce qui est ici se règle sur le robot en marche, sans
 * reconstruire ni redémarrer. C'est ce qui permet de chercher un compromis en l'observant.
 */
@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface RobotConfig extends Config {

    @Key("language.code")
    @DefaultValue("fr-FR")
    String languageCode();

    /** Name of the the webcam to select. */
    @Key("device.webcam.name")
    String webcamName();

    /** Name of the the microphone to select. */
    @Key("device.microphone.name")
    String microphoneName();

    /**
     * Durée de silence, en secondes, à partir de laquelle une phrase est considérée terminée.
     * <p>
     * C'est le poste dominant du délai perçu entre la fin de la parole et le texte reconnu : la
     * reconnaissance ne peut pas démarrer (en batch) ou se conclure (en streaming) avant qu'il soit
     * écoulé.
     * <p>
     * Le baisser rend le robot plus réactif mais coupe les phrases sur une simple hésitation : le
     * fragment déjà détecté part à la reconnaissance, et la suite est perdue puisque l'écoute est
     * mise en pause pendant la réponse.
     */
    @Key("speech.recognizer.silence.duration.seconds")
    @DefaultValue("1.2")
    double dureeSilenceFinPhraseSecondes();

    /**
     * Indique si l'organe de vision (webcam + serveur Python) doit être démarré.
     * Désactivé par défaut : nécessite une webcam et le serveur Python gRPC (localhost:50051).
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
     * limite, et Spring ferme la session (« Buffer size ... exceeds the allowed limit »). À ajuster
     * en observant les FPS affichés par la webapp.
     */
    @Key("robot.capteurs.vision.stream.fps")
    @DefaultValue("8")
    int fpsFluxVideo();

    /**
     * Le watchdog peut-il couper les moteurs ? Désactivé, les battements restent relevés et
     * les pastilles d'état de l'interface continuent de fonctionner : seul le déclenchement de
     * l'arrêt d'urgence est supprimé. Utile pour mettre au point un organe sans se faire couper
     * les moteurs, ou pour écarter le watchdog en cas de doute sur un faux positif.
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
     */
    @Key("robot.capteurs.vision.stream.jpeg.quality")
    @DefaultValue("60")
    int qualiteJpegFluxVideo();

    /**
     * Netteté minimale exigée d'un visage pour en tirer une empreinte, en variance du laplacien
     * (voir {@code VisageDansLImage.nettete}).
     * <p>
     * <b>Le même seuil pour la photo importée et pour la caméra</b>, et c'est voulu : une empreinte
     * bâclée ne gêne pas que la personne concernée — la reconnaissance retient la meilleure
     * similarité parmi toutes les empreintes connues, et celle-là peut dépasser le seuil face à
     * quelqu'un d'<b>autre</b>. La mesure est ramenée à 192 px des deux côtés, justement pour que
     * la même valeur veuille dire la même chose sur une photo de téléphone et sur la webcam.
     * <p>
     * Mesuré sur les photos d'exemple du dépôt : originaux à 222, 537, 1146 et 3423 ; les mêmes
     * franchement floutés à 3, 8, 15 et 52. Cent sépare les deux paquets avec une marge confortable
     * de part et d'autre. <b>Ce seuil sépare l'inexploitable de l'exploitable, pas le bon du
     * parfait</b> : le monter recalerait des prises honnêtes, et refuser ce que quelqu'un vient
     * d'envoyer coûte plus cher que d'accepter une empreinte moyenne, que d'autres viendront
     * compléter.
     */
    @Key("robot.capteurs.vision.visage.nettete.minimale")
    @DefaultValue("100")
    double netteteMinimaleDuVisage();

    /**
     * Décalage maximal du nez par rapport au milieu des yeux, en écarts d'yeux, au-delà duquel on
     * tient le visage pour trop de profil (voir {@code VisageDetecte.asymetrieDuNez}).
     * <p>
     * SFace est entraîné sur des visages à peu près de face : de profil, l'empreinte s'éloigne de
     * celle qu'on a enrôlée au point de tomber parfois plus près de quelqu'un d'autre. Passé ce
     * seuil, le robot ne cherche plus qui c'est et n'enrôle plus — il rend « quelqu'un » au lieu
     * d'un nom. Ça ne le fait <b>pas</b> mieux reconnaître de profil : ça remplace une mauvaise
     * réponse par pas de réponse.
     * <p>
     * Calibrage : sur les photos d'exemple du dépôt, des visages de face donnent 0,008 à 0,104,
     * roulis compris (le nez est projeté sur l'axe des yeux, une tête penchée ne compte donc pas
     * pour un profil). Géométriquement, le décalage vaut environ 0,32·tan(lacet) — 0,35 correspond
     * donc à peu près à 45°, soit trois fois la pire mesure de face. Volontairement large : la
     * reconnaissance tient très bien le trois-quarts, et une porte trop sévère rendrait le robot
     * amnésique dès qu'on ne le regarde pas droit dans les yeux.
     */
    @Key("robot.capteurs.vision.visage.asymetrie.maximale")
    @DefaultValue("0.35")
    double asymetrieMaximaleDuNez();

    /**
     * Durée de présence continue, en secondes, avant de considérer que quelqu'un est vraiment là.
     * <p>
     * Exprimée en durée et non en nombre de cycles : la cadence de la reconnaissance varie avec
     * la charge du Nano et le nombre de visages, un compte de cycles ne voudrait pas dire la même
     * chose d'un moment à l'autre. Le baisser rend le robot plus prompt à aborder les gens, mais
     * le fait réagir à quelqu'un qui ne fait que passer devant la caméra.
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
     */
    @Key("robot.presence.absence.seconds")
    @DefaultValue("4.0")
    double dureeAbsenceAvantDepartSecondes();

    /**
     * Trou maximal toléré, en secondes, <b>tant que la venue n'est pas encore confirmée</b> : au
     * delà, le décompte de présence repart de zéro.
     * <p>
     * Sans lui, la tolérance de {@link #dureeAbsenceAvantDepartSecondes()} — faite pour ne pas
     * croire quelqu'un parti — vaudrait aussi pour la confirmation : des perceptions éparses,
     * espacées de trois secondes, cumuleraient la durée exigée comme si elles se suivaient, et le
     * robot aborderait quelqu'un qu'il n'a fait qu'entrevoir. Ce seuil garantit plusieurs visages
     * vus <b>à la suite</b>, tout en laissant passer le clignotement de la détection (100 à
     * 250 ms).
     */
    @Key("robot.presence.continuite.seconds")
    @DefaultValue("1.0")
    double dureeContinuitePresenceSecondes();

    /**
     * Fenêtre glissante, en secondes, sur laquelle un inconnu et les personnes connues sont
     * départagés au nombre de fois qu'ils ont été vus.
     * <p>
     * Un même visage sort tantôt reconnu, tantôt inconnu — la reconnaissance oscille autour de son
     * seuil. Sans cette fenêtre, les deux lectures accumulent leur présence chacune de leur côté et
     * l'inconnu peut se confirmer alors que la personne est reconnue trois fois sur cinq.
     * <p>
     * Trop courte, elle ne départage rien ; trop longue, elle fait traîner l'abord d'un vrai
     * inconnu, dont le compte doit d'abord dépasser celui des connus.
     */
    @Key("robot.presence.fenetre.seconds")
    @DefaultValue("3.0")
    double fenetreArbitragePresenceSecondes();

    /**
     * Délai minimal, en secondes, entre deux rencontres déclenchées pour la même personne.
     * <p>
     * Second garde-fou, indépendant du précédent : même si quelqu'un sort réellement du champ et
     * revient, le robot ne doit pas rejouer les retrouvailles aussitôt.
     */
    @Key("robot.presence.rencontre.temporisation.seconds")
    @DefaultValue("30.0")
    double temporisationEntreRencontresSecondes();

    /**
     * Durée d'absence en deçà de laquelle le robot <b>reprend</b> la conversation au lieu de
     * saluer.
     * <p>
     * Quelqu'un qui s'absente une minute n'attend pas un nouveau bonjour mais qu'on reprenne où
     * on en était. Au-delà, l'absence se remarque et la salutation redevient juste.
     */
    @Key("robot.retrouvailles.reprise.seconds")
    @DefaultValue("300.0")
    double dureeRepriseSansSalutationSecondes();

    /**
     * Durée d'absence en deçà de laquelle le robot ne dit <b>rien du tout</b> au retour.
     * <p>
     * La personne s'est tournée, s'est levée, est sortie du champ un instant : la conversation
     * n'a jamais été interrompue, et la reprendre à voix haute revient à la couper — « on parlait
     * de la population française », douze secondes après en avoir parlé.
     */
    @Key("robot.retrouvailles.silence.seconds")
    @DefaultValue("60.0")
    double dureeSilenceAuRetourSecondes();

    /**
     * Délai minimal, en secondes, avant qu'une activité qui vient de se terminer puisse être
     * réclamée à nouveau.
     * <p>
     * Distinct de {@link #temporisationEntreRencontresSecondes()}, qui raisonne par personne :
     * celui-ci protège de toute demande insistante. Le cas traité est l'activité qui tourne court
     * — quelqu'un qu'on aborde et qui ne répond pas — dont la cause est encore là à la fin : sans
     * délai, elle repartirait en boucle.
     */
    @Key("robot.activite.temporisation.seconds")
    @DefaultValue("30.0")
    double delaiAvantRelanceActiviteSecondes();

    /** Indique si le robot tourne la tête vers les visages qu'il perçoit. */
    @Key("robot.regard.enabled")
    @DefaultValue("true")
    boolean regardEnabled();

    /**
     * Champ de vision horizontal de la webcam, en degrés.
     * <p>
     * C'est lui qui convertit un écart en pixels en un angle de rotation du cou : une valeur
     * fausse ne fait pas regarder à côté, elle fait sous-corriger ou dépasser. À mesurer une fois
     * (repère à distance connue) plutôt qu'à croire sur parole du fabricant.
     */
    @Key("robot.regard.camera.champ.horizontal.degres")
    @DefaultValue("60.0")
    double champHorizontalCameraDegres();

    /**
     * Écart angulaire en deçà duquel le robot considère qu'il regarde déjà la personne.
     * <p>
     * Sans cette zone morte, la moindre imprécision de la boîte englobante — qui respire d'une
     * image à l'autre — suffirait à faire bouger la tête en permanence.
     */
    @Key("robot.regard.zone.morte.degres")
    @DefaultValue("5.0")
    double zoneMorteRegardDegres();

    /**
     * Combien d'unités de commande du cou valent un degré d'écart vu par la caméra.
     * <p>
     * <b>Seul réglage entre l'écart perçu et la consigne envoyée</b> : le cou se commande en
     * position absolue, la cible est donc calculée directement et non approchée. Corriger moins
     * que l'écart n'apporte qu'une suite de petits mouvements.
     * <p>
     * Se mesure en commandant un angle connu et en regardant de combien l'écart perçu a changé,
     * <b>une fois la tête stabilisée</b> — mesurer pendant qu'elle traverse surestime largement
     * le déplacement.
     */
    @Key("robot.regard.panoramique.commande.par.degre.vu")
    @DefaultValue("1.0")
    double commandePanoramiqueParDegreVu();

    /**
     * Idem pour l'inclinaison — <b>et la valeur n'est pas la même</b>.
     * <p>
     * Une unité de commande déplace le regard d'environ <b>trois degrés</b> en inclinaison (6,3
     * commandés pour 22,6° vus) contre un seul en panoramique : les deux servos n'ont pas le même
     * bras de levier, et une échelle commune fait dépasser lourdement sur cet axe.
     * <p>
     * 0,3 plutôt que le tiers exact : sous-corriger ne coûte qu'un mouvement de plus,
     * sur-corriger fait osciller la tête sans fin.
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
     */
    @Key("robot.regard.temporisation.seconds")
    @DefaultValue("1.0")
    double temporisationRegardSecondes();

    /**
     * Durée pendant laquelle la manette garde la main sur le cou, en secondes, comptée à partir
     * de son <b>dernier</b> ordre.
     * <p>
     * Le regard et la manette commandent le même servo, et rien ne les départageait : une
     * correction du regard s'intercalait au milieu d'un mouvement piloté à la main. Celui qui
     * conduit gagne, et le délai court depuis le dernier ordre — donc depuis le relâchement du
     * joystick, qui envoie un {@code STOPPER}. Le suivi reprend ensuite tout seul, sans rien à
     * rallumer.
     * <p>
     * Trop court, le regard reprend la tête entre deux coups de joystick ; trop long, le robot
     * paraît absent après qu'on a lâché la manette.
     */
    @Key("robot.regard.priorite.manette.seconds")
    @DefaultValue("3.0")
    double prioriteManetteSecondes();

    /**
     * Inverse le sens de rotation panoramique pour le regard.
     * <p>
     * Étalonnage à faire <b>une fois</b> : le sens dépend du montage du servo, que rien dans le
     * code ne permet de deviner (les deux conventions de {@code MouvementCouEvent} se
     * contredisent d'ailleurs). Vérifié sur le robot : {@code false} est le bon
     * sens. Rechargé à chaud, sans reconstruire l'image.
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
     */
    @Key("robot.regard.inclinaison.sens.inverse")
    @DefaultValue("false")
    boolean regardInclinaisonSensInverse();

    /**
     * Cadence d'échantillonnage d'une animation, en hertz.
     * <p>
     * Dix, parce que le contrôleur ne peut pas suivre plus vite : le banc du 2026-08-29 a mesuré
     * <b>5 ms par canal écrit plus 12 ms par écriture</b>. Cinq axes en mouvement coûtent donc
     * environ 85 ms par tour, pour un budget de 100. Monter à 30 Hz demanderait deux fois et demie
     * le débit du hub, et le lecteur passerait son temps en retard sur lui-même.
     * <p>
     * Cette lenteur est un atout, pas un pis-aller : avec 100 ms entre deux échantillons, la rampe
     * du contrôleur a le temps de <b>remplir l'intervalle</b> au lieu d'être interrompue avant
     * d'avoir commencé. C'est elle qui lisse le mouvement entre deux consignes.
     * <p>
     * Baisser si des animations bougent beaucoup d'axes à la fois et traînent ; monter n'a de sens
     * que sur une animation à un ou deux axes.
     */
    @Key("robot.animation.cadence.hertz")
    @DefaultValue("10")
    double animationCadenceHertz();

    /**
     * Écart, en degrés, en deçà duquel un axe n'est pas réécrit d'un échantillon à l'autre.
     * <p>
     * C'est le vrai levier du lecteur, bien plus que la cadence : le banc a montré qu'un canal
     * <b>engagé mais non écrit ne coûte rien</b>. Un axe immobile — et une animation en bouge
     * rarement plus de deux ou trois à la fois — est donc gratuit, ce qui laisse le budget aux
     * axes qui travaillent vraiment.
     * <p>
     * Trois dixièmes de degré, sous ce qu'un servo RC sait résoudre : ce qu'on économise ainsi,
     * le robot ne pouvait de toute façon pas le montrer. Monter le seuil ferait des paliers
     * visibles sur les mouvements lents.
     */
    @Key("robot.animation.seuil.degres")
    @DefaultValue("0.3")
    double animationSeuilDegres();
}
