package fr.roboteek.robot.memoire.courtterme;

import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Ce que le robot a en tête, là, maintenant : qui est devant lui, quels visages il vient de voir,
 * quel apprentissage est en cours.
 * <p>
 * Point d'entrée unique : le reste du robot s'adresse ici plutôt qu'aux trois mémoires qu'il y a
 * derrière, qui peuvent alors changer sans que personne d'autre ait à le savoir.
 * <p>
 * Cette mémoire ne perçoit rien elle-même, les organes lui apportent ce qu'ils ont vu — sous forme
 * de fonctions qui, elles, tiennent l'image, et n'en font sortir que des boîtes et des empreintes.
 */
@Component
public class MemoireCourtTerme {

    private final SuiviDesVisages suiviDesVisages;

    private final EnrolementEnCours enrolementEnCours;

    private final RegistrePresence registrePresence;

    private final PersonneRepository personneRepository;

    /**
     * La personne à qui le robot parle, telle qu'il s'en souvient.
     * <p>
     * Un souvenir, et non une lecture instantanée : la reconnaissance cligne, et un visage manqué
     * le temps d'un cycle ne doit pas faire changer d'interlocuteur au milieu d'une phrase. Ce
     * souvenir vit ici et non dans la conversation, parce que c'est exactement ce qu'est la mémoire
     * court terme — et parce que le regard, l'accueil et les retrouvailles y ont tous intérêt.
     */
    private volatile Personne interlocuteur;

    public MemoireCourtTerme(SuiviDesVisages suiviDesVisages,
                             EnrolementEnCours enrolementEnCours,
                             RegistrePresence registrePresence,
                             PersonneRepository personneRepository) {
        this.suiviDesVisages = suiviDesVisages;
        this.enrolementEnCours = enrolementEnCours;
        this.registrePresence = registrePresence;
        this.personneRepository = personneRepository;
    }

    /**
     * Rattache les visages qui viennent d'être détectés à ce qu'on savait, et leur donne une
     * identité. Voir {@link SuiviDesVisages#suivre}.
     */
    public List<VisageSuivi> suivreLesVisages(List<VisageDetecte> visagesDetectes,
                                              Function<VisageDetecte, Personne> reconnaissance) {
        List<VisageSuivi> visagesSuivis = suiviDesVisages.suivre(visagesDetectes, reconnaissance);
        retenirLInterlocuteur(visagesSuivis);
        return visagesSuivis;
    }

    /**
     * Retient à qui le robot parle : le plus gros visage identifié, donc le plus proche — le même
     * critère que pour le regard. Un cycle qui n'identifie personne ne change rien : on garde ce
     * qu'on savait.
     */
    private void retenirLInterlocuteur(List<VisageSuivi> visagesSuivis) {
        visagesSuivis.stream()
                .filter(VisageSuivi::estIdentifie)
                .max(Comparator.comparingLong(visage ->
                        (long) visage.boite().getWidth() * visage.boite().getHeight()))
                .map(visage -> personneRepository.parId(visage.idPersonne()))
                .ifPresent(personne -> interlocuteur = personne);
    }

    /**
     * La personne à qui le robot parle, ou {@code null} s'il ne parle à personne d'identifié.
     * <p>
     * Le souvenir est confronté à la présence avant d'être rendu : quelqu'un qui a quitté le champ
     * depuis assez longtemps n'est plus un interlocuteur, même s'il est le dernier qu'on ait vu.
     */
    public Personne interlocuteur() {
        Personne connu = interlocuteur;
        if (connu == null) {
            return null;
        }
        return registrePresence.idsPersonnesPresentes().contains(connu.id()) ? connu : null;
    }

    /**
     * Désigne l'interlocuteur sans attendre que la reconnaissance le confirme.
     * <p>
     * Utile dans un seul cas, mais il est réel : le robot vient d'apprendre un visage et connaît
     * la personne avant de savoir la reconnaître. Partout ailleurs, l'interlocuteur se déduit de
     * ce qu'on voit.
     */
    public void poserInterlocuteur(Personne personne) {
        this.interlocuteur = personne;
    }

    /**
     * Sort quelqu'un de la tête du robot : sa présence, son suivi, et le fait qu'il lui parlait.
     * <p>
     * Appelé quand la personne vient d'être effacée de la mémoire longue. Sans cela, le robot
     * continuerait un moment de la croire devant lui et de lui parler — en cherchant en base une
     * identité qui n'y est plus. Les visages suivis sont oubliés en bloc plutôt que triés : le
     * suivi se refait en trois images, ce n'est pas un état qu'il vaut la peine de réparer.
     */
    public void oublier(String idPersonne) {
        registrePresence.oublier(idPersonne);
        suiviDesVisages.oublierTout();
        Personne connu = interlocuteur;
        if (connu != null && connu.id().equals(idPersonne)) {
            interlocuteur = null;
        }
    }

    /** Les visages du dernier cycle, pour qui veut les afficher. */
    public List<VisageSuivi> visagesVus() {
        return suiviDesVisages.derniersVisages();
    }

    /** Oublie les visages suivis : la perception n'est plus fiable, mieux vaut repartir de zéro. */
    public void oublierLesVisages() {
        suiviDesVisages.oublierTout();
    }

    /** Ouvre un apprentissage de visage. Voir {@link EnrolementEnCours}. */
    public void demarrerUnEnrolement(String idPersonne, Consumer<List<PriseDeVisage>> ecrireEnMemoireLongue) {
        enrolementEnCours.demarrer(idPersonne, ecrireEnMemoireLongue);
    }

    /** Renonce à un apprentissage, en répondant tout de même à celui qui l'attend. */
    public void renoncerALEnrolement(String idPersonne, String motif) {
        enrolementEnCours.renoncer(idPersonne, motif);
    }

    /** Avance l'apprentissage d'une image. Sans effet s'il n'y en a pas en cours. */
    public void avancerLEnrolement(Supplier<PriseDeVisage> visageLePlusProche) {
        enrolementEnCours.avancer(visageLePlusProche);
    }

    /**
     * Les personnes que le robot a devant lui à cet instant.
     * <p>
     * La question que tout le reste finit par poser — « à qui je parle ? ». Les inconnus n'y
     * figurent pas : ils n'ont pas d'identité à rendre.
     */
    public List<Personne> personnesPresentes() {
        return registrePresence.idsPersonnesPresentes().stream()
                .map(personneRepository::parId)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
