package fr.roboteek.robot.memoire.longterme.rencontre;

import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.systemenerveux.event.RencontreEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreSansSuiteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tient l'histoire des rencontres : qui le robot a vu, et quand.
 * <p>
 * Écoute le bus plutôt que d'être appelé, et n'a donc aucune prise sur ceux qui décident. Deux
 * évènements suffisent, et ils existaient déjà :
 * <ul>
 *   <li>{@link RencontreEvent} — quelqu'un est là et ça vaut la peine d'y réagir : on inscrit ;</li>
 *   <li>{@link RencontreSansSuiteEvent} — le cerveau a refusé, « la rencontre est rendue » : on
 *       reprend la ligne qu'on venait d'écrire.</li>
 * </ul>
 * Ce second point n'est pas un détail de propreté. Une rencontre refusée est immédiatement rejouée
 * — c'est tout l'objet de {@code RencontreSansSuiteEvent} —, si bien que quelqu'un qui reste devant
 * le robot pendant qu'une activité l'occupe déclenche une annonce toutes les cinq secondes. Sans la
 * reprise, la timeline afficherait dix lignes pour une seule venue et ne voudrait plus rien dire.
 * <p>
 * Les inconnus n'y figurent pas : sans identité, il n'y a personne à qui rattacher une ligne.
 */
@Component
public class JournalDesRencontres {

    private static final Logger logger = LoggerFactory.getLogger(JournalDesRencontres.class);

    /**
     * Nombre de rencontres conservées par personne.
     * <p>
     * Quelqu'un qui vit dans la même pièce que le robot est rencontré plusieurs fois par heure :
     * sans borne, la table grossirait sans fin pour des lignes que personne n'ira jamais lire.
     * Deux cents couvrent largement ce qu'une timeline montre.
     */
    private static final int RENCONTRES_GARDEES = 200;

    /** Ce qu'une fiche affiche par défaut. */
    public static final int LIMITE_PAR_DEFAUT = 50;

    private final RencontreRepository rencontreRepository;

    private final Clock horloge;

    /**
     * Dernière ligne écrite pour chaque personne, celle qu'un refus viendrait reprendre.
     * <p>
     * En mémoire et non en base : cela n'a de sens que dans les secondes qui suivent l'annonce, et
     * une ligne qu'un redémarrage empêche de reprendre restera simplement dans l'histoire — c'est
     * bien la moins grave des deux erreurs possibles.
     */
    private final Map<String, Long> derniereLigneEcrite = new ConcurrentHashMap<>();

    /**
     * {@code @Autowired} obligatoire ici : cette classe a deux constructeurs, et Spring n'en
     * choisit aucun d'office — il se rabat alors sur un constructeur vide, qui n'existe pas, et le
     * contexte entier échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public JournalDesRencontres(RencontreRepository rencontreRepository) {
        this(rencontreRepository, Clock.systemDefaultZone());
    }

    /** Permet aux tests de maîtriser le temps. */
    JournalDesRencontres(RencontreRepository rencontreRepository, Clock horloge) {
        this.rencontreRepository = rencontreRepository;
        this.horloge = horloge;
    }

    /**
     * Inscrit la toute première rencontre, celle où l'on fait connaissance.
     * <p>
     * Appelée et non déduite d'un évènement : à l'enrôlement, {@code RegistrePresence} compte
     * délibérément la personne comme déjà rencontrée pour ne pas la saluer deux fois de suite,
     * si bien qu'aucun {@link RencontreEvent} ne part. Sans cet appel, la date qui compte le plus
     * dans une timeline — le jour de la rencontre — serait la seule à manquer.
     */
    public void inscrirePremiereRencontre(Personne personne) {
        inscrire(personne.id(), Rencontre.Type.PREMIERE, -1);
    }

    @EventListener
    public void handleRencontreEvent(RencontreEvent rencontreEvent) {
        Personne personne = rencontreEvent.getPersonne();
        if (personne == null) {
            return;
        }
        inscrire(personne.id(), Rencontre.Type.RETOUR, rencontreEvent.getSecondesDAbsence());
    }

    @EventListener
    public void handleRencontreSansSuiteEvent(RencontreSansSuiteEvent evenement) {
        Personne personne = evenement.getPersonne();
        if (personne == null) {
            return;
        }
        Long ligne = derniereLigneEcrite.remove(personne.id());
        if (ligne != null && rencontreRepository.supprimer(ligne)) {
            logger.debug("Rencontre avec {} reprise dans le journal : elle n'a pas eu lieu ({})",
                    personne.prenom(), evenement.getMotif());
        }
    }

    /** La timeline d'une personne, de la plus récente à la plus ancienne. */
    public List<Rencontre> pourPersonne(String idPersonne) {
        return rencontreRepository.parPersonne(idPersonne, LIMITE_PAR_DEFAUT);
    }

    /** Combien de fois chacun a été rencontré, pour toute la liste d'un coup. */
    public Map<String, Integer> nombreParPersonne() {
        return rencontreRepository.nombreParPersonne();
    }

    private void inscrire(String idPersonne, Rencontre.Type type, long secondesDAbsence) {
        long ligne = rencontreRepository.ajouter(idPersonne, LocalDateTime.now(horloge), type, secondesDAbsence);
        derniereLigneEcrite.put(idPersonne, ligne);
        rencontreRepository.elaguer(idPersonne, RENCONTRES_GARDEES);
    }
}
