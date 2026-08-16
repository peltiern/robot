package fr.roboteek.robot.memoire.personne;

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
 *   <li>une <b>durée de présence continue</b> avant de croire que quelqu'un est là ;</li>
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

    private void mettreAJourPresence(String cle, Instant maintenant) {
        Presence presence = presences.computeIfAbsent(cle, ignore -> new Presence());

        if (presence.derniereVue == null || secondesEcoulees(presence.derniereVue, maintenant) > robotConfig().dureeAbsenceAvantDepartSecondes()) {
            // Première apparition, ou retour après une absence assez longue pour compter : la
            // personne recommence une venue, et redevient donc saluable.
            presence.debutVenue = maintenant;
            presence.venueTraitee = false;
        }
        presence.derniereVue = maintenant;

        if (presence.venueTraitee
                || secondesEcoulees(presence.debutVenue, maintenant) < robotConfig().dureeConfirmationPresenceSecondes()) {
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
        annoncerRencontre(cle);
    }

    private void annoncerRencontre(String cle) {
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
        long secondesDepuisDerniereRencontre = personne.derniereRencontre() == null
                ? -1
                : Duration.between(personne.derniereRencontre(), maintenant).toSeconds();

        Personne personneAJour = personne.rencontreeLe(maintenant);
        personneRepository.enregistrer(personneAJour);

        logger.info("Rencontre : {}, vu pour la dernière fois il y a {} s", personne.prenom(), secondesDepuisDerniereRencontre);
        applicationEventPublisher.publishEvent(
                new RencontreEvent(RencontreEvent.TYPE.CONNU_REVU, personneAJour, secondesDepuisDerniereRencontre));
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
    }
}
