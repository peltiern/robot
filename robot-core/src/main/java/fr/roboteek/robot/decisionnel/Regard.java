package fr.roboteek.robot.decisionnel;

import jakarta.annotation.PostConstruct;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
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
 * Ce n'est <b>pas</b> le suivi asservi (vitesse commandée en continu sur l'erreur), qui reste un
 * chantier à part entière : les servos RC du cou ne rendent pas leur position réelle, ce qui
 * l'avait déjà fait échouer une fois.
 * <p>
 * Deux garde-fous contre le tic nerveux, tous deux réglables à chaud :
 * <ul>
 *   <li>une <b>zone morte</b> : un visage déjà à peu près en face ne fait pas bouger la tête ;</li>
 *   <li>une <b>temporisation</b> entre deux corrections, qui laisse au servo le temps d'arriver —
 *       sans elle, les corrections s'empileraient sur une image d'avant le mouvement.</li>
 * </ul>
 * <p>
 * Il n'y a en revanche <b>aucun amortissement</b> : l'écart mesuré est corrigé en entier. Essayé
 * le 2026-08-12, n'en corriger que 70 % n'a produit qu'une chose — la tête arrivait en quatre
 * petits à-coups au lieu d'un mouvement. Ce qui protège d'un dépassement, ce n'est pas de viser
 * court, c'est la justesse de l'échelle, et la zone morte absorbe ce qu'il en reste.
 * <p>
 * Aucun besoin de connaître l'arrêt d'urgence : le cou refuse déjà tout ordre de mouvement tant
 * qu'il est armé.
 */
@Component
public class Regard {

    private static final Logger logger = LoggerFactory.getLogger(Regard.class);

    /**
     * Consigne si faible qu'elle ne vaut pas la peine d'être envoyée.
     * <p>
     * Volontairement basse. Elle avait d'abord été fixée à 2,5, en croyant tenir une zone
     * insensible du servo : des consignes de 1,6 à 1,9 laissaient la position inchangée, relevé
     * après relevé. Le journal des butées a montré autre chose — ces consignes-là allaient
     * <b>toutes vers le haut</b>, c'est-à-dire vers une butée déjà atteinte. Ce n'était pas le
     * servo qui était sourd, c'était le cou qui était au bout.
     * <p>
     * Le seuil reste, mais comme simple garde-fou : une consigne d'un dixième de degré n'a aucun
     * sens, et la répéter dix fois par seconde noierait les journaux.
     */
    private static final double COMMANDE_MINIMALE = 1.0;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Clock horloge;

    /** Instant de la dernière correction, qui porte la temporisation. {@code null} si aucune. */
    private Instant derniereCorrection;

    /**
     * {@code @Autowired} obligatoire ici : cette classe a deux constructeurs, et Spring n'en
     * choisit aucun d'office — il se rabat alors sur un constructeur vide, qui n'existe pas, et
     * le contexte entier échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public Regard(ApplicationEventPublisher applicationEventPublisher) {
        this(applicationEventPublisher, Clock.systemDefaultZone());
    }

    /** Permet aux tests de maîtriser le temps, dont dépend la temporisation. */
    Regard(ApplicationEventPublisher applicationEventPublisher, Clock horloge) {
        this.applicationEventPublisher = applicationEventPublisher;
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
        logger.info("Regard : {}, {} / {} unité(s) de cou par degré vu (panoramique / inclinaison), "
                        + "zone morte {} degrés, une correction toutes les {} s",
                robotConfig().regardEnabled() ? "actif" : "inactif",
                robotConfig().commandePanoramiqueParDegreVu(), robotConfig().commandeInclinaisonParDegreVu(),
                robotConfig().zoneMorteRegardDegres(), robotConfig().temporisationRegardSecondes());
    }

    /**
     * Traitement volontairement court : la publication est synchrone, ce code s'exécute donc sur
     * le thread de capture vidéo.
     */
    @EventListener
    public synchronized void handleVisagePercuEvent(VisagePercuEvent visagePercuEvent) {
        if (!robotConfig().regardEnabled()
                || visagePercuEvent.getLargeurImage() <= 0 || visagePercuEvent.getHauteurImage() <= 0) {
            return;
        }
        VisagePercu cible = visageRegarde(visagePercuEvent.getVisages());
        if (cible == null) {
            return;
        }

        // Une seule focale pour les deux axes : les pixels sont carrés, l'échelle
        // pixels-par-degré est donc la même horizontalement et verticalement. C'est ce qui évite
        // d'avoir à déclarer — et à mesurer — un second champ de vision pour la hauteur.
        double focalePixels = focalePixels(visagePercuEvent.getLargeurImage());
        double ecartPanoramique = ecartAngulaire(centreX(cible) - visagePercuEvent.getLargeurImage() / 2.0, focalePixels);
        double ecartInclinaison = ecartAngulaire(centreY(cible) - visagePercuEvent.getHauteurImage() / 2.0, focalePixels);

        double zoneMorte = robotConfig().zoneMorteRegardDegres();
        boolean corrigerPanoramique = Math.abs(ecartPanoramique) >= zoneMorte;
        boolean corrigerInclinaison = Math.abs(ecartInclinaison) >= zoneMorte;
        if (!corrigerPanoramique && !corrigerInclinaison) {
            return;
        }
        Instant maintenant = horloge.instant();
        if (derniereCorrection != null
                && secondesEcoulees(derniereCorrection, maintenant) < robotConfig().temporisationRegardSecondes()) {
            return;
        }

        // Un axe déjà bien orienté — ou dont la consigne serait trop faible pour que le servo
        // la suive — n'est pas commandé du tout : une rotation nulle ferait repartir une consigne
        // pour rien, et une rotation trop petite ne ferait rien du tout.
        double commandePanoramique = corrigerPanoramique
                ? commande(ecartPanoramique, robotConfig().commandePanoramiqueParDegreVu()) : 0;
        double commandeInclinaison = corrigerInclinaison
                ? commande(ecartInclinaison, robotConfig().commandeInclinaisonParDegreVu()) : 0;
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
        logger.info("Regard : {} à {} / {} degrés (panoramique / inclinaison), commande de {} / {}, {} visage(s) dans le champ",
                cible.estConnu() ? cible.prenom() : "quelqu'un",
                arrondi(ecartPanoramique), arrondi(ecartInclinaison),
                arrondi(commandePanoramique), arrondi(commandeInclinaison),
                visagePercuEvent.getVisages().size());
        tournerLaTete(commandePanoramique, commandeInclinaison);
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
     * corriger qu'une part, comme essayé le 2026-08-12, ne fait qu'étaler le même mouvement en
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

    /**
     * Distance focale de la caméra exprimée en pixels, déduite du champ de vision déclaré.
     */
    private static double focalePixels(int largeurImage) {
        return (largeurImage / 2.0) / Math.tan(Math.toRadians(robotConfig().champHorizontalCameraDegres() / 2.0));
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
    private void tournerLaTete(double anglePanoramique, double angleInclinaison) {
        MouvementCouEvent mouvement = new MouvementCouEvent();
        // Rotation relative et non position absolue : le cou n'a pas à savoir d'où il part, et
        // nous n'avons aucun moyen fiable de le lui dire (servos RC sans retour de position).
        if (anglePanoramique != 0) {
            mouvement.setAnglePanoramique(
                    robotConfig().regardPanoramiqueSensInverse() ? -anglePanoramique : anglePanoramique);
        }
        if (angleInclinaison != 0) {
            mouvement.setAngleInclinaison(
                    robotConfig().regardInclinaisonSensInverse() ? -angleInclinaison : angleInclinaison);
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
