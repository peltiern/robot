package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.activites.presentation.PresentationActivity;
import fr.roboteek.robot.activites.retrouvailles.RetrouvaillesActivity;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteEvent;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteRefuseeEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreSansSuiteEvent;
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

    /**
     * La personne dont on vient de réclamer les retrouvailles, le temps de savoir si le cerveau
     * les accepte. Le refus revient dans la même pile d'appels que la demande, la valeur est donc
     * toujours la bonne au moment où on la lit.
     */
    private Personne personneDesRetrouvaillesReclamees;

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
        personneDesRetrouvaillesReclamees = rencontreEvent.getPersonne();
        // Posée avant la demande : une demande d'activité ne transporte qu'un identifiant, et la
        // boucle du cerveau peut lancer l'activité dès l'instant d'après.
        retrouvaillesActivity.setPersonneRetrouvee(
                rencontreEvent.getPersonne(), rencontreEvent.getSecondesDAbsence());
        applicationEventPublisher.publishEvent(
                new DemandeActiviteEvent(RetrouvaillesActivity.class.getSimpleName()));
    }

    /**
     * Rend sa venue à celui que le cerveau a refusé d'aborder.
     * <p>
     * La rencontre a été consommée en pure perte : le registre de présence tient la venue pour
     * tranchée alors que personne n'a été abordé. Sans cet aveu, la personne restant devant la
     * caméra, aucune nouvelle venue ne commence et le robot ne lui dira jamais rien. Constaté sur
     * le robot le 2026-08-15 pour un accueil, le 2026-08-16 pour des retrouvailles.
     * <p>
     * Rendu ici et non dans le registre : c'est le déclencheur qui a fait la demande, et lui seul
     * sait qui elle visait.
     */
    @EventListener
    public void handleDemandeActiviteRefuseeEvent(DemandeActiviteRefuseeEvent demandeActiviteRefuseeEvent) {
        String idActivite = demandeActiviteRefuseeEvent.getIdActivite();
        String motif = demandeActiviteRefuseeEvent.getMotif();

        if (PresentationActivity.class.getSimpleName().equals(idActivite)) {
            logger.info("Accueil refusé ({}) : la rencontre d'inconnu est rendue, elle sera rejouée", motif);
            applicationEventPublisher.publishEvent(
                    new RencontreSansSuiteEvent(null, "accueil refusé : " + motif));
        } else if (RetrouvaillesActivity.class.getSimpleName().equals(idActivite)) {
            // La personne visée est celle de la demande qu'on vient de faire : la chaîne est
            // synchrone — publier la demande appelle le cerveau, qui publie le refus, qui nous
            // revient — donc c'est bien la bonne, sans risque d'en croiser une autre.
            Personne personne = personneDesRetrouvaillesReclamees;
            logger.info("Retrouvailles avec {} refusées ({}) : la rencontre est rendue",
                    personne == null ? "?" : personne.prenom(), motif);
            applicationEventPublisher.publishEvent(
                    new RencontreSansSuiteEvent(personne, "retrouvailles refusées : " + motif));
        }
    }
}
