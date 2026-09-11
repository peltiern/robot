package fr.roboteek.robot.decisionnel;

import jakarta.annotation.PostConstruct;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationEnCours;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.TelemetrieOrganeEvent;
import fr.roboteek.robot.systemenerveux.event.OrigineMouvement;
import fr.roboteek.robot.systemenerveux.event.VisagePercu;
import fr.roboteek.robot.systemenerveux.event.VisagePercuEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Tourner la tête vers la personne qu'on regarde.
 * <p>
 * <b>Boucle ouverte, un coup à la fois, sur les deux axes du regard</b> (panoramique et
 * inclinaison ; l'axe « monter / descendre » du cou n'en fait pas partie, il change la posture
 * et non la direction du regard). À chaque correction, l'écart entre le centre du visage et le
 * centre de l'image est converti en angles par le champ de vision de la webcam, et le cou reçoit
 * un ordre de rotation relative — puis plus rien pendant un délai. La caméra étant portée par la
 * tête, le visage se retrouve recentré et la correction suivante n'a plus lieu d'être : la
 * convergence vient de la géométrie, pas d'un asservissement.
 * <p>
 * Ce n'est <b>pas</b> un suivi asservi (vitesse commandée en continu sur l'erreur) : les servos
 * RC du cou ne rendent pas leur position réelle, ce qui l'a déjà fait échouer.
 * <p>
 * Deux garde-fous contre le tic nerveux, tous deux réglables à chaud :
 * <ul>
 *   <li>une <b>zone morte</b> : un visage déjà à peu près en face ne fait pas bouger la tête ;</li>
 *   <li>une <b>temporisation</b> entre deux corrections, qui laisse au servo le temps d'arriver —
 *       sans elle, les corrections s'empileraient sur une image d'avant le mouvement.</li>
 * </ul>
 * <p>
 * <b>Aucun amortissement</b> : l'écart mesuré est corrigé en entier. N'en corriger que 70 % fait
 * arriver la tête en quatre à-coups au lieu d'un mouvement. Ce qui protège du dépassement, ce
 * n'est pas de viser court mais la justesse de l'échelle, et la zone morte absorbe le reste.
 * <p>
 * <b>La manette est prioritaire</b> : tant que quelqu'un conduit, le regard se tait — sinon les
 * deux se disputent le même servo. La priorité court depuis le <b>dernier</b> ordre de la manette,
 * donc depuis le relâchement du joystick : le suivi reprend seul, sans interrupteur à rallumer.
 * <p>
 * Rien à savoir de l'arrêt d'urgence : le cou refuse déjà tout mouvement tant qu'il est armé.
 */
@Component
public class Regard {

    private static final Logger logger = LoggerFactory.getLogger(Regard.class);

    /**
     * Consigne si faible qu'elle ne vaut pas la peine d'être envoyée. Simple garde-fou : un
     * dixième de degré n'a aucun sens, et le répéter dix fois par seconde noierait les journaux.
     * <p>
     * Volontairement basse : ce qui ressemble à une zone insensible du servo est en général un
     * cou arrivé en butée, et monter le seuil pour cette raison-là serait une erreur.
     * <p>
     * <b>Un degré de TÊTE depuis le 2026-09-08</b>, et non plus une unité de position moteur. Le
     * seuil a donc silencieusement baissé — il valait 1,6° de tête au panoramique et 5,0° à
     * l'inclinaison, au point que celle-ci ne corrigeait rien sous 5° d'écart vu alors que sa zone
     * morte s'arrête à 4. C'était un second seuil, invisible, plus haut que celui qu'on croyait
     * régler.
     */
    private static final double COMMANDE_MINIMALE = 1.0;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final AnimationEnCours animationEnCours;

    private final Clock horloge;

    /** Instant de la dernière correction, qui porte la temporisation. {@code null} si aucune. */
    private Instant derniereCorrection;

    /** Instant du dernier ordre de cou venu de la manette. {@code null} si personne n'a conduit. */
    private Instant dernierOrdreManette;

    /** Ce que le journal a déjà annoncé, pour ne dire la mise en retrait qu'aux changements. */
    private boolean animationAvaitLaMain;

    /**
     * {@code @Autowired} obligatoire ici : cette classe a deux constructeurs, et Spring n'en
     * choisit aucun d'office — il se rabat alors sur un constructeur vide, qui n'existe pas, et
     * le contexte entier échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public Regard(ApplicationEventPublisher applicationEventPublisher, AnimationEnCours animationEnCours) {
        this(applicationEventPublisher, animationEnCours, Clock.systemDefaultZone());
    }

    /** Permet aux tests de maîtriser le temps, dont dépend la temporisation. */
    Regard(ApplicationEventPublisher applicationEventPublisher, AnimationEnCours animationEnCours, Clock horloge) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.animationEnCours = animationEnCours;
        this.horloge = horloge;
    }

    /**
     * Annonce les réglages effectivement lus au démarrage.
     * <p>
     * Trois séances d'essai ont été perdues à régler un robot qui tournait un binaire périmé et
     * un {@code robot.properties} qui écrasait les valeurs par défaut. Une ligne au démarrage
     * suffit à le voir. Les valeurs pouvant être rechargées à chaud, elle dit ce qui était en
     * vigueur à cet instant, pas pour toujours.
     */
    @PostConstruct
    void annoncerReglages() {
        RobotConfig reglages = robotConfig();
        logger.info("Regard : {}, {} / {} degré(s) de tête par degré vu (panoramique / inclinaison), "
                        + "zone morte {} degrés, déport caméra {} degrés, "
                        + "une correction toutes les {} s, "
                        + "manette prioritaire {} s après son dernier ordre",
                reglages.regardEnabled() ? "actif" : "inactif",
                reglages.commandePanoramiqueParDegreVu(), reglages.commandeInclinaisonParDegreVu(),
                reglages.zoneMorteRegardDegres(), reglages.deportCameraDegres(),
                reglages.temporisationRegardSecondes(),
                reglages.prioriteManetteSecondes());
    }

    /**
     * Traitement volontairement court : la publication est synchrone, ce code s'exécute donc sur
     * le thread de capture vidéo.
     */
    @EventListener
    public synchronized void handleVisagePercuEvent(VisagePercuEvent visagePercuEvent) {
        // Les réglages sont lus UNE fois par évènement, et non à chaque besoin : ils sont
        // rechargeables à chaud, chaque lecture est donc une recherche, et surtout ils pourraient
        // changer au milieu d'un calcul — une zone morte d'avant et une échelle d'après.
        RobotConfig reglages = robotConfig();
        if (!reglages.regardEnabled()
                || visagePercuEvent.getLargeurImage() <= 0 || visagePercuEvent.getHauteurImage() <= 0) {
            return;
        }
        if (animationALaMain()) {
            return;
        }
        Instant maintenant = horloge.instant();
        if (manetteALaMain(maintenant, reglages)) {
            // Volontairement muet : la scène est perçue dix fois par seconde, et le dire à chaque
            // fois noierait le journal pendant toute la conduite. La prise de main, elle, est
            // journalisée une fois (voir handleMouvementCouEvent).
            return;
        }
        VisagePercu cible = visageRegarde(visagePercuEvent.getVisages());
        if (cible == null) {
            return;
        }

        // Une seule focale pour les deux axes : les pixels sont carrés, l'échelle
        // pixels-par-degré est donc la même horizontalement et verticalement. C'est ce qui évite
        // d'avoir à déclarer — et à mesurer — un second champ de vision pour la hauteur.
        double focalePixels = focalePixels(visagePercuEvent.getLargeurImage(), reglages.champHorizontalCameraDegres());
        // L'écart se compte depuis l'AXE OPTIQUE, pas depuis le milieu de l'image. Les deux ne
        // coïncident pas — le capteur n'est pas parfaitement centré derrière l'objectif — et prendre
        // le milieu ajoutait ici un biais constant de 4,2 degrés vers le haut, qui poussait
        // l'inclinaison dans sa butée. Voir centreOptiqueYRelatif.
        double axeX = visagePercuEvent.getLargeurImage() * reglages.centreOptiqueXRelatif();
        double axeY = visagePercuEvent.getHauteurImage() * reglages.centreOptiqueYRelatif();
        // Le panoramique ne vise pas l'axe optique mais un point décalé : la webcam n'est que dans
        // un des deux yeux, et centrer le visage sur l'image laisse la tête tournée à côté. Voir
        // deportCameraDegres. L'inclinaison n'est pas concernée, les deux yeux sont à la même
        // hauteur.
        double ecartPanoramique = ecartAngulaire(centreX(cible) - axeX, focalePixels)
                - reglages.deportCameraDegres();
        double ecartInclinaison = ecartAngulaire(centreY(cible) - axeY, focalePixels);

        double zoneMorte = reglages.zoneMorteRegardDegres();
        boolean corrigerPanoramique = Math.abs(ecartPanoramique) >= zoneMorte;
        boolean corrigerInclinaison = Math.abs(ecartInclinaison) >= zoneMorte;
        if (!corrigerPanoramique && !corrigerInclinaison) {
            return;
        }
        if (derniereCorrection != null
                && secondesEcoulees(derniereCorrection, maintenant) < reglages.temporisationRegardSecondes()) {
            return;
        }

        // Un axe déjà bien orienté — ou dont la consigne serait trop faible pour que le servo
        // la suive — n'est pas commandé du tout : une rotation nulle ferait repartir une consigne
        // pour rien, et une rotation trop petite ne ferait rien du tout.
        double commandePanoramique = corrigerPanoramique
                ? commande(ecartPanoramique, reglages.commandePanoramiqueParDegreVu()) : 0;
        double commandeInclinaison = corrigerInclinaison
                ? commande(ecartInclinaison, reglages.commandeInclinaisonParDegreVu()) : 0;
        if (Math.abs(commandePanoramique) < COMMANDE_MINIMALE) {
            commandePanoramique = 0;
        }
        if (Math.abs(commandeInclinaison) < COMMANDE_MINIMALE) {
            commandeInclinaison = 0;
        }
        if (commandePanoramique == 0 && commandeInclinaison == 0) {
            // Rien à envoyer : ne pas consommer la temporisation, et surtout ne rien journaliser
            // — c'est le cas qui, répété dix fois par seconde, noierait le reste.
            return;
        }
        derniereCorrection = maintenant;

        // Le nombre de visages est journalisé pour une raison précise : si la perception en voit
        // plusieurs, « le plus gros » peut désigner l'un puis l'autre d'un cycle sur l'autre, et
        // le robot passerait son temps à faire l'aller-retour entre deux personnes — un défaut
        // qu'aucun réglage d'échelle ne corrigerait, et qui ressemble à s'y méprendre à un
        // dépassement.
        logger.info("Regard : {} à {} / {} degrés (panoramique / inclinaison), commande de {} / {}, "
                        + "cou à {} / {}, {} visage(s) dans le champ",
                cible.estConnu() ? cible.prenom() : "quelqu'un",
                arrondi(ecartPanoramique), arrondi(ecartInclinaison),
                arrondi(commandePanoramique), arrondi(commandeInclinaison),
                arrondi(positionPanoramique), arrondi(positionInclinaison),
                visagePercuEvent.getVisages().size());
        tournerLaTete(commandePanoramique, commandeInclinaison, reglages);
    }

    /**
     * Où le cou se trouvait quand la correction est partie, en degrés de tête.
     * <p>
     * <b>Cette ligne existe parce qu'elle a manqué.</b> Le journal disait l'écart vu et la consigne
     * envoyée, jamais la position atteinte — impossible d'y distinguer une tête qui n'a pas bougé
     * d'une tête qui a bougé pendant que la personne se déplaçait. Le 2026-09-08, un écart qui
     * grandit après une correction de 11° n'a pas pu être expliqué faute de ce chiffre, et la même
     * lacune avait déjà fait tirer un rapport de transmission faux d'un journal de suivi de visage
     * en août.
     * <p>
     * Lue sur le bus et non demandée à {@code Cou} : le regard décide, il n'interroge pas les
     * organes. {@code NaN} tant qu'aucune télémétrie n'est arrivée, ce que le journal montre tel
     * quel plutôt que de faire croire à un zéro.
     */
    private volatile double positionPanoramique = Double.NaN;

    private volatile double positionInclinaison = Double.NaN;

    @EventListener
    public void handleTelemetrieOrganeEvent(TelemetrieOrganeEvent telemetrieOrganeEvent) {
        if (!"cou".equals(telemetrieOrganeEvent.getOrganeId())) {
            return;
        }
        Double pan = telemetrieOrganeEvent.getValeurs().get("pan");
        Double tilt = telemetrieOrganeEvent.getValeurs().get("tilt");
        if (pan != null) {
            positionPanoramique = pan;
        }
        if (tilt != null) {
            positionInclinaison = tilt;
        }
    }

    /**
     * La manette prend la main sur le cou, et la garde un moment.
     * <p>
     * Le regard reçoit ici ses propres ordres en retour — il publie sur le même bus — d'où le
     * filtre sur l'origine, qui est aussi ce qui l'empêche de se suspendre lui-même.
     * <p>
     * On ne mémorise que l'instant : pas de « début » ni de « fin » de conduite à tenir, donc rien
     * à réarmer si un {@code STOPPER} se perd. Le pire qu'il puisse arriver est que le suivi
     * reprenne quelques secondes trop tard.
     */
    @EventListener
    public synchronized void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
        if (mouvementCouEvent.getOrigine() != OrigineMouvement.MANETTE) {
            return;
        }
        Instant maintenant = horloge.instant();
        if (!manetteALaMain(maintenant, robotConfig())) {
            logger.info("Regard : la manette prend la main sur le cou, suivi de visage suspendu");
        }
        dernierOrdreManette = maintenant;
    }

    /**
     * Vrai pendant qu'une animation se déroule : le regard s'efface entièrement.
     * <p>
     * Ce n'est pas une politesse, c'est une correction. Le regard commande le cou en <b>angle
     * relatif</b>, et {@code PhidgetsServoMotor.rotate} calcule {@code getPositionReelle() +
     * angle}. Or la position d'un servo RC ne se rafraîchit qu'à l'<b>atteinte d'une cible</b>, et
     * une animation en envoie une nouvelle toutes les 100 ms — la cible n'est donc presque jamais
     * atteinte, le regard part d'une position périmée, son erreur s'accumule à chaque correction
     * et le cou dérive jusqu'à la butée. Constaté le 2026-08-30 : {@code consigne 162.3 bornée à
     * 155.0}, puis une tête restée collée à sa butée à l'arrêt du robot.
     * <p>
     * Interrogation directe du lecteur, et non une fenêtre de temps comme pour la manette : une
     * animation peut n'agiter que les yeux pendant plusieurs secondes, sans publier un seul ordre
     * de cou, et le regard reprendrait la main au milieu du geste. Et non plus un couple
     * d'évènements début/fin : une fin perdue suspendrait le suivi pour toujours.
     */
    private boolean animationALaMain() {
        boolean enCours = animationEnCours.enLecture();
        if (enCours != animationAvaitLaMain) {
            animationAvaitLaMain = enCours;
            logger.info(enCours
                    ? "Regard : une animation prend la main sur le cou, suivi de visage suspendu"
                    : "Regard : animation terminée, suivi de visage repris");
        }
        return enCours;
    }

    /** Vrai tant que le dernier ordre de la manette est assez récent pour lui garder la main. */
    private boolean manetteALaMain(Instant maintenant, RobotConfig reglages) {
        return dernierOrdreManette != null
                && secondesEcoulees(dernierOrdreManette, maintenant) < reglages.prioriteManetteSecondes();
    }

    /**
     * Le visage à regarder : le plus gros, donc le plus proche.
     * <p>
     * Départager par la taille plutôt que par l'identité est délibéré : le robot regarde qui lui
     * fait face, connu ou non — c'est justement à un inconnu qu'il aura à s'adresser.
     */
    private static VisagePercu visageRegarde(List<VisagePercu> visages) {
        if (visages == null) {
            return null;
        }
        return visages.stream()
                .max(Comparator.comparingLong(visage -> (long) visage.largeur() * visage.hauteur()))
                .orElse(null);
    }

    /**
     * Traduit un écart perçu en consigne pour le cou, <b>avec l'échelle propre à l'axe</b> : les
     * deux servos n'entraînent pas la tête avec le même bras de levier.
     * <p>
     * <b>La totalité de l'écart, pas une fraction</b> : le cou se commande en absolu — il lit sa
     * position et y ajoute l'angle — donc viser juste est un calcul, pas une approche. N'en
     * corriger qu'une part ne fait qu'étaler le même mouvement en
     * quatre petits à-coups.
     */
    private static double commande(double ecartDegres, double commandeParDegreVu) {
        return ecartDegres * commandeParDegreVu;
    }

    private static double centreX(VisagePercu visage) {
        return visage.x() + visage.largeur() / 2.0;
    }

    private static double centreY(VisagePercu visage) {
        return visage.y() + visage.hauteur() / 2.0;
    }

    /** Distance focale de la caméra exprimée en pixels, déduite du champ de vision déclaré. */
    private static double focalePixels(int largeurImage, double champHorizontalDegres) {
        return (largeurImage / 2.0) / Math.tan(Math.toRadians(champHorizontalDegres / 2.0));
    }

    /**
     * Écart angulaire, en degrés, correspondant à un décalage en pixels par rapport à l'axe de
     * la caméra.
     * <p>
     * L'arc tangente plutôt qu'une simple règle de trois : celle-ci surestime l'angle sur les
     * bords de l'image, là où le visage à rattraper est justement le plus loin — elle ferait donc
     * dépasser précisément quand ça se voit le plus.
     */
    private static double ecartAngulaire(double ecartPixels, double focalePixels) {
        return Math.toDegrees(Math.atan(ecartPixels / focalePixels));
    }

    /**
     * @param anglePanoramique rotation horizontale, positive vers la droite de l'image
     * @param angleInclinaison rotation verticale, positive vers le bas de l'image
     */
    private void tournerLaTete(double anglePanoramique, double angleInclinaison, RobotConfig reglages) {
        MouvementCouEvent mouvement = new MouvementCouEvent();
        mouvement.setOrigine(OrigineMouvement.REGARD);
        // Rotation relative et non position absolue : le cou n'a pas à savoir d'où il part, et
        // nous n'avons aucun moyen fiable de le lui dire (servos RC sans retour de position).
        if (anglePanoramique != 0) {
            mouvement.setAnglePanoramique(
                    reglages.regardPanoramiqueSensInverse() ? -anglePanoramique : anglePanoramique);
        }
        if (angleInclinaison != 0) {
            mouvement.setAngleInclinaison(
                    reglages.regardInclinaisonSensInverse() ? -angleInclinaison : angleInclinaison);
        }
        applicationEventPublisher.publishEvent(mouvement);
    }

    private static double secondesEcoulees(Instant debut, Instant fin) {
        return Duration.between(debut, fin).toMillis() / 1000d;
    }

    private static double arrondi(double valeur) {
        return Math.round(valeur * 10) / 10d;
    }
}
