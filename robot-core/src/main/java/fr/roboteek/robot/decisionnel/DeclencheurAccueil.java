package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.presentation.PresentationActivity;
import fr.roboteek.robot.activites.retrouvailles.RetrouvaillesActivity;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteEvent;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteRefuseeEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreInconnuInaboutieEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fait le lien entre « il y a quelqu'un » et « va lui parler ».
 * <p>
 * Une classe à part, et non un écouteur de plus dans {@link PresentationActivity} : les écouteurs
 * d'une activité ne réagissent que lorsqu'elle est <b>active</b>, or il s'agit précisément de la
 * réveiller alors qu'elle ne l'est pas. C'est aussi le bon endroit pour décider un jour d'accueils
 * différents selon l'heure ou l'humeur, sans toucher au script de la rencontre.
 * <p>
 * Ne décide rien de plus que « ça vaut la peine » : c'est {@link ArbitrageActivites} qui tranche
 * ensuite, et qui refusera pendant un arrêt d'urgence ou si la présentation vient d'avoir lieu.
 */
@Component
public class DeclencheurAccueil {

    private static final Logger logger = LoggerFactory.getLogger(DeclencheurAccueil.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    private final RetrouvaillesActivity retrouvaillesActivity;

    public DeclencheurAccueil(ApplicationEventPublisher applicationEventPublisher,
                              RetrouvaillesActivity retrouvaillesActivity) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.retrouvaillesActivity = retrouvaillesActivity;
    }

    /**
     * Traitement volontairement court : la publication étant synchrone, ce code s'exécute sur le
     * thread de capture vidéo.
     */
    @EventListener
    public void handleRencontreEvent(RencontreEvent rencontreEvent) {
        if (rencontreEvent.getType() == RencontreEvent.TYPE.INCONNU) {
            logger.info("Quelqu'un d'inconnu : demande de faire connaissance");
            applicationEventPublisher.publishEvent(
                    new DemandeActiviteEvent(PresentationActivity.class.getSimpleName()));
            return;
        }

        if (rencontreEvent.getPersonne() == null) {
            logger.warn("Retrouvailles annoncées sans personne : rien à saluer");
            return;
        }
        logger.info("{} est de retour : demande de retrouvailles", rencontreEvent.getPersonne().prenom());
        // Posée avant la demande : une demande d'activité ne transporte qu'un identifiant, et la
        // boucle du cerveau peut lancer l'activité dès l'instant d'après.
        retrouvaillesActivity.setPersonneRetrouvee(
                rencontreEvent.getPersonne(), rencontreEvent.getSecondesDAbsence());
        applicationEventPublisher.publishEvent(
                new DemandeActiviteEvent(RetrouvaillesActivity.class.getSimpleName()));
    }

    /**
     * Rend son tour à l'inconnu quand le cerveau a refusé de l'aborder.
     * <p>
     * La rencontre a été consommée en pure perte : le registre de présence tient la venue pour
     * tranchée alors que personne n'a été abordé. Sans cet aveu, la temporisation du refus finissait
     * par expirer sans que rien ne relance quoi que ce soit — la personne restant devant la caméra,
     * aucune nouvelle venue ne commençait. Constaté sur le robot le 2026-08-15.
     * <p>
     * Rendu ici et non dans le registre : c'est le déclencheur qui a fait la demande, et lui seul
     * sait que ce refus concerne un accueil.
     */
    @EventListener
    public void handleDemandeActiviteRefuseeEvent(DemandeActiviteRefuseeEvent demandeActiviteRefuseeEvent) {
        if (!PresentationActivity.class.getSimpleName().equals(demandeActiviteRefuseeEvent.getIdActivite())) {
            return;
        }
        logger.info("Accueil refusé ({}) : la rencontre d'inconnu est rendue, elle sera rejouée",
                demandeActiviteRefuseeEvent.getMotif());
        applicationEventPublisher.publishEvent(
                new RencontreInconnuInaboutieEvent("accueil refusé : " + demandeActiviteRefuseeEvent.getMotif()));
    }
}
