package fr.roboteek.robot.memoire.personne;

import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreInconnuInaboutieEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreEvent;
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
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Qui est devant le robot, depuis quand — et à partir de là, quand une rencontre mérite d'être
 * annoncée.
 * <p>
 * Mémoire courte, sans persistance : tout est perdu au redémarrage, et c'est voulu. Ce qui doit
 * survivre vit dans {@link PersonneRepository}.
 * <p>
 * Toute la raison d'être de cette classe est l'hystérésis. La perception brute
 * ({@link VisagePercuEvent}) clignote : mesuré sur le robot, un visage immobile disparaît et
 * réapparaît par trous de 100 à 250 ms, une seule frame ratée par le détecteur suffisant. Branché
 * directement dessus, un comportement d'accueil demanderait son prénom à la même personne six fois
 * par quart de minute — et le robot finirait débranché. D'où trois garde-fous, tous réglables à
 * chaud dans {@code robot.properties} :
 * <ul>
 *   <li>une <b>durée de présence continue</b> avant de croire que quelqu'un est là — continue au
 *       sens strict : un trou franc fait repartir le décompte, sans quoi des perceptions éparses
 *       finiraient par cumuler la durée exigée ;</li>
 *   <li>une <b>durée d'absence</b> avant de le croire parti, qui absorbe le clignotement ;</li>
 *   <li>une <b>temporisation par personne</b>, qui empêche de resaluer un habitué des allers-retours.</li>
 * </ul>
 * <p>
 * Le départ n'est jamais constaté sur le moment mais déduit au retour (« ça fait plus de N secondes
 * que je ne t'avais pas vu »). C'est ce qui évite une tâche périodique de surveillance : sans
 * personne dans le champ, il n'y a rien à faire, donc rien à réveiller.
 */
@Component
public class RegistrePresence {

    private static final Logger logger = LoggerFactory.getLogger(RegistrePresence.class);

    /**
     * Clé unique regroupant tous les visages non reconnus.
     * <p>
     * Deux inconnus simultanés comptent donc pour une seule présence : rien ne permet de les
     * distinguer d'un cycle à l'autre — un visage inconnu n'a, par définition, pas d'identité à
     * suivre. Le robot s'occupera du premier, et découvrira le second une fois celui-là enregistré.
     */
    private static final String CLE_INCONNU = "inconnu";

    private final PersonneRepository personneRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Clock horloge;

    /** Présences en cours, par identifiant de personne (ou {@link #CLE_INCONNU}). */
    private final Map<String, Presence> presences = new HashMap<>();

    /**
     * {@code @Autowired} obligatoire ici : cette classe a deux constructeurs, et Spring n'en
     * choisit aucun d'office — il se rabat alors sur un constructeur vide, qui n'existe pas, et
     * le contexte entier échoue au démarrage.
     */
    @Autowired
    public RegistrePresence(PersonneRepository personneRepository, ApplicationEventPublisher applicationEventPublisher) {
        this(personneRepository, applicationEventPublisher, Clock.systemDefaultZone());
    }

    /** Permet aux tests de maîtriser le temps, dont tout le comportement dépend. */
    RegistrePresence(PersonneRepository personneRepository, ApplicationEventPublisher applicationEventPublisher, Clock horloge) {
        this.personneRepository = personneRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.horloge = horloge;
    }

    /**
     * Intercepte la perception des visages.
     * <p>
     * Traitement volontairement court : la publication est synchrone, ce code s'exécute donc sur
     * le thread de capture vidéo. Seule une rencontre effective — rare, et temporisée — touche la
     * base des personnes.
     */
    @EventListener
    public synchronized void handleVisagePercuEvent(VisagePercuEvent visagePercuEvent) {
        if (visagePercuEvent.getVisages() == null) {
            return;
        }
        Instant maintenant = horloge.instant();
        Set<String> clesVues = new HashSet<>();
        for (VisagePercu visage : visagePercuEvent.getVisages()) {
            String cle = visage.estConnu() ? visage.idPersonne() : CLE_INCONNU;
            // Plusieurs visages peuvent porter la même clé (deux inconnus) : une seule présence.
            if (clesVues.add(cle)) {
                mettreAJourPresence(cle, maintenant);
            }
        }
    }

    /**
     * Oublie la présence « inconnu » dès qu'un visage vient d'être appris.
     * <p>
     * Tous les inconnus partagent une seule clé de présence, temporisée. Sans cet oubli, la
     * personne qu'on vient d'enrôler continuerait d'occuper cette place : quelqu'un d'autre
     * arrivant dans les minutes qui suivent ne serait pas annoncé, et le robot l'ignorerait
     * poliment. Or c'est justement le cas courant — deux personnes qui arrivent ensemble.
     * <p>
     * Celle qu'on vient d'apprendre, elle, est désormais reconnue : elle a sa propre clé.
     */
    @EventListener
    public synchronized void handleEnrolementTermineEvent(EnrolementTermineEvent enrolementTermineEvent) {
        if (!enrolementTermineEvent.isReussi()) {
            return;
        }
        presences.remove(CLE_INCONNU);
        logger.debug("Visage appris : la présence « inconnu » est oubliée, le suivant sera un autre");

        // Et la personne apprise est comptée comme rencontrée à l'instant. Sans cela, le robot la
        // reconnaîtrait dans la seconde qui suit et annoncerait des retrouvailles avec quelqu'un
        // dont il vient tout juste de faire la connaissance.
        Instant maintenant = horloge.instant();
        Presence presence = presences.computeIfAbsent(enrolementTermineEvent.getIdPersonne(), ignore -> new Presence());
        presence.debutVenue = maintenant;
        presence.derniereVue = maintenant;
        presence.derniereRencontre = maintenant;
        presence.venueTraitee = true;
    }

    /**
     * Rend son tour à l'inconnu quand la rencontre annoncée n'a mené à rien.
     * <p>
     * La venue est tenue pour tranchée et la temporisation court dès l'annonce, ce qui est juste
     * quand quelqu'un a effectivement été abordé. Quand l'annonce a été perdue — méprise sur une
     * personne connue, ou accueil refusé par le cerveau —, la garder ferait ignorer le prochain
     * inconnu, celui-là bien réel. Oublier la présence suffit à ce qu'elle se reconfirme d'elle-même
     * quelques perceptions plus tard.
     */
    @EventListener
    public synchronized void handleRencontreInconnuInaboutieEvent(RencontreInconnuInaboutieEvent evenement) {
        if (presences.remove(CLE_INCONNU) != null) {
            logger.info("Rencontre d'inconnu sans suite ({}) : elle est oubliée, la prochaine sera annoncée",
                    evenement.getMotif());
        }
    }

    private void mettreAJourPresence(String cle, Instant maintenant) {
        Presence presence = presences.computeIfAbsent(cle, ignore -> new Presence());

        // Deux tolérances au trou, et non une seule, parce qu'on n'y guette pas la même chose.
        // Tant que la venue n'est pas tranchée, on cherche à s'assurer que quelqu'un est bien là :
        // il faut des perceptions qui se suivent, et le moindre trou franc remet le compteur à
        // zéro. Une fois la venue tranchée, on ne guette plus que le départ, et il faut au
        // contraire beaucoup de patience pour ne pas prendre un clignotement pour une sortie.
        double trouTolere = presence.venueTraitee
                ? robotConfig().dureeAbsenceAvantDepartSecondes()
                : robotConfig().dureeContinuitePresenceSecondes();

        if (presence.derniereVue == null || secondesEcoulees(presence.derniereVue, maintenant) > trouTolere) {
            // Première apparition, ou retour après une absence assez longue pour compter : la
            // personne recommence une venue, et redevient donc saluable.
            // La durée du trou est retenue ici, et nulle part ailleurs : c'est la seule vraie
            // absence, et le seul instant où on la connaisse. La date de la dernière rencontre
            // annoncée, elle, ne dit rien de ce qui s'est passé entre-temps — quelqu'un peut
            // avoir parlé sans discontinuer depuis. Confondre les deux a fait dire au robot
            // « on parlait de la population française » douze secondes après l'avoir dit.
            presence.secondesDAbsence = presence.derniereVue == null
                    ? -1
                    : (long) secondesEcoulees(presence.derniereVue, maintenant);
            presence.debutVenue = maintenant;
            presence.venueTraitee = false;
        }
        presence.derniereVue = maintenant;
        presence.vuesRecentes.add(maintenant);

        if (presence.venueTraitee
                || secondesEcoulees(presence.debutVenue, maintenant) < robotConfig().dureeConfirmationPresenceSecondes()) {
            return;
        }

        // L'inconnu doit l'emporter sur les identités connues avant d'être annoncé. Rien n'est
        // tranché ici en cas d'échec : la venue reste ouverte, et l'arbitrage se rejouera au cycle
        // suivant — c'est bien le même visage qu'on continue d'observer.
        if (CLE_INCONNU.equals(cle) && !linconnuLemporte(maintenant)) {
            return;
        }

        // La venue est tranchée une fois pour toutes : sans cela, quelqu'un qui reste devant le
        // robot verrait la temporisation expirer sous ses pieds et se ferait saluer à nouveau.
        presence.venueTraitee = true;

        if (presence.derniereRencontre != null
                && secondesEcoulees(presence.derniereRencontre, maintenant) < robotConfig().temporisationEntreRencontresSecondes()) {
            logger.debug("Présence confirmée pour {}, mais rencontre trop récente : rien n'est annoncé", cle);
            return;
        }

        presence.derniereRencontre = maintenant;
        annoncerRencontre(cle, presence.secondesDAbsence);
    }

    /**
     * Départage l'inconnu et les personnes connues au nombre de fois qu'ils ont été vus sur la
     * fenêtre glissante : vu trois fois comme Nicolas et deux fois comme un inconnu, c'est Nicolas.
     * <p>
     * <b>L'arbitrage ne joue que dans ce sens</b>, et c'est délibéré : se tromper d'inconnu coûte
     * une question à quelqu'un qu'on connaît, se tromper de connu ne coûte rien — personne
     * n'écoute encore les retrouvailles. Le doute profite donc à ceux qu'on connaît.
     */
    private boolean linconnuLemporte(Instant maintenant) {
        int vuesInconnu = vuesDansLaFenetre(presences.get(CLE_INCONNU), maintenant);
        int vuesMeilleurConnu = presences.entrySet().stream()
                .filter(entree -> !CLE_INCONNU.equals(entree.getKey()))
                .mapToInt(entree -> vuesDansLaFenetre(entree.getValue(), maintenant))
                .max()
                .orElse(0);
        if (vuesInconnu <= vuesMeilleurConnu) {
            logger.debug("Inconnu vu {} fois contre {} pour la personne la mieux reconnue : rien n'est annoncé",
                    vuesInconnu, vuesMeilleurConnu);
            return false;
        }
        return true;
    }

    /** Nombre de perceptions encore dans la fenêtre, les plus anciennes étant oubliées au passage. */
    private int vuesDansLaFenetre(Presence presence, Instant maintenant) {
        if (presence == null) {
            return 0;
        }
        double fenetre = robotConfig().fenetreArbitragePresenceSecondes();
        presence.vuesRecentes.removeIf(vue -> secondesEcoulees(vue, maintenant) > fenetre);
        return presence.vuesRecentes.size();
    }

    private void annoncerRencontre(String cle, long secondesDAbsence) {
        Personne personne = CLE_INCONNU.equals(cle) ? null : personneRepository.parId(cle);

        if (CLE_INCONNU.equals(cle) || personne == null) {
            if (personne == null && !CLE_INCONNU.equals(cle)) {
                // Empreinte rattachée à une personne effacée : mieux vaut refaire connaissance
                // que d'entretenir une identité fantôme.
                logger.warn("Personne {} absente de la base : rencontre annoncée comme un inconnu", cle);
            }
            logger.info("Rencontre : quelqu'un que je ne connais pas");
            applicationEventPublisher.publishEvent(new RencontreEvent(RencontreEvent.TYPE.INCONNU, null, -1));
            return;
        }

        LocalDateTime maintenant = LocalDateTime.now(horloge);
        Personne personneAJour = personne.rencontreeLe(maintenant);
        personneRepository.enregistrer(personneAJour);

        logger.info("Rencontre : {}, absent pendant {} s (vu pour la dernière fois le {})",
                personne.prenom(), secondesDAbsence, personne.derniereRencontre());
        applicationEventPublisher.publishEvent(
                new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, personneAJour, secondesDAbsence));
    }

    private static double secondesEcoulees(Instant debut, Instant fin) {
        return Duration.between(debut, fin).toMillis() / 1000d;
    }

    /** Ce que le registre retient d'une identité, le temps qu'elle est là. */
    private static final class Presence {

        /** Début de la venue en cours (remis à zéro après une absence assez longue). */
        private Instant debutVenue;

        /** Dernière fois que cette identité a été perçue. */
        private Instant derniereVue;

        /** Dernière rencontre annoncée, qui porte la temporisation. */
        private Instant derniereRencontre;

        /** La venue en cours a déjà été tranchée (annoncée ou volontairement tue). */
        private boolean venueTraitee;

        /**
         * Durée, en secondes, du trou qui a précédé la venue en cours ; {@code -1} à la première
         * apparition. C'est l'absence réelle de la personne, celle qui décide si le robot doit la
         * saluer, reprendre la conversation, ou se taire.
         */
        private long secondesDAbsence = -1;

        /**
         * Instants des dernières perceptions, purgés au-delà de la fenêtre d'arbitrage. C'est de
         * quoi compter combien de fois cette identité a été vue récemment, et donc la comparer aux
         * autres lectures du même visage.
         */
        private final Deque<Instant> vuesRecentes = new ArrayDeque<>();
    }
}
