package fr.roboteek.robot.memoire.courtterme;

import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * L'apprentissage d'un visage pendant qu'il se fait : les empreintes déjà relevées, et jusqu'à
 * quand on accepte d'attendre les suivantes.
 * <p>
 * Mémoire court terme : quelque chose que le robot a en tête le temps de quelques images. Ce qui
 * doit durer part dans {@code VisageConnu}, et seulement à la fin.
 */
@Component
public class EnrolementEnCours {

    private static final Logger logger = LoggerFactory.getLogger(EnrolementEnCours.class);

    /**
     * Nombre d'empreintes relevées pour apprendre un visage.
     * <p>
     * Plusieurs et non une seule : une empreinte unique, prise de trois quarts ou à contre-jour,
     * et la personne n'est plus jamais reconnue. Les cycles étant espacés d'environ un tiers de
     * seconde, cinq empreintes couvrent près de deux secondes de menus changements de pose.
     */
    private static final int EMPREINTES_ATTENDUES = 5;

    /**
     * Délai au bout duquel on renonce à compléter un enrôlement.
     * <p>
     * Indispensable : la personne peut se détourner ou partir entre la demande et la prise. Sans
     * échéance, l'activité qui attend le résultat resterait suspendue et le cerveau avec elle.
     */
    private static final long DUREE_MAX_MS = 5000;

    private final ApplicationEventPublisher applicationEventPublisher;

    /** Personne en cours d'apprentissage, {@code null} si aucun enrôlement n'est en cours. */
    private String idPersonne;

    private long echeanceMs;

    private final List<PriseDeVisage> prises = new ArrayList<>();

    /**
     * Ce qu'on fera des prises une fois le compte atteint, fourni par l'organe qui a lancé
     * l'apprentissage.
     * <p>
     * Fourni et non injecté : la reconnaissance de visages n'est pas un bean Spring — l'organe de
     * vision la charge lui-même au démarrage, et elle peut manquer si les modèles ne sont pas là.
     * La mémoire courte décide <b>quand</b> écrire, l'organe sait <b>comment</b> ; c'est le même
     * partage que pour l'extraction des empreintes.
     */
    private Consumer<List<PriseDeVisage>> ecrireEnMemoireLongue;

    public EnrolementEnCours(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Ouvre un apprentissage : les empreintes se relèveront sur les images à venir.
     *
     * @param ecrireEnMemoireLongue ce qui rendra ces prises durables, appelé une seule fois,
     *                              à la fin, et seulement s'il y a quelque chose à écrire
     */
    public synchronized void demarrer(String idPersonne, Consumer<List<PriseDeVisage>> ecrireEnMemoireLongue) {
        this.idPersonne = idPersonne;
        this.ecrireEnMemoireLongue = ecrireEnMemoireLongue;
        this.echeanceMs = System.currentTimeMillis() + DUREE_MAX_MS;
        this.prises.clear();
        logger.info("Enrôlement démarré pour la personne {}", idPersonne);
    }

    /**
     * Avance d'une image : relève une prise de plus si on en a une, et conclut quand le compte
     * y est ou que le temps est écoulé.
     * <p>
     * <b>Trois temps, et le verrou n'est tenu que pour le deuxième.</b> L'extraction coûte ~68 ms
     * et l'écriture en mémoire longue touche le disque ; les tenir sous le moniteur bloquerait
     * quiconque veut seulement savoir où en est l'enrôlement. Surtout, la publication du résultat
     * est synchrone : sous le verrou, toute la chaîne d'écoute — {@code RegistrePresence}, les
     * activités — s'exécuterait moniteur tenu, en prenant au passage leurs propres verrous.
     *
     * @param visageLePlusProche rend l'empreinte et le portrait du visage auquel on parle, ou
     *                           {@code null} si aucun visage n'est visible. Volontairement
     *                           paresseux : l'extraction ne doit pas être payée hors d'un
     *                           enrôlement.
     */
    public void avancer(Supplier<PriseDeVisage> visageLePlusProche) {
        String personneVisee;
        synchronized (this) {
            if (idPersonne == null) {
                return;
            }
            personneVisee = idPersonne;
        }

        PriseDeVisage prise = visageLePlusProche.get();

        Enrolement termine;
        synchronized (this) {
            // L'enrôlement a pu changer pendant l'extraction : cette prise ne concerne alors plus
            // personne, et l'attribuer au suivant lui donnerait le visage de quelqu'un d'autre.
            if (!personneVisee.equals(idPersonne)) {
                return;
            }
            if (prise != null) {
                prises.add(prise);
            }
            if (prises.size() < EMPREINTES_ATTENDUES && System.currentTimeMillis() < echeanceMs) {
                return;
            }
            termine = retirer();
        }

        conclure(termine);
    }

    /**
     * Renonce et répond quand même. Sert quand la reconnaissance n'est pas disponible : celui qui
     * attend le résultat ne doit jamais rester suspendu.
     */
    public void renoncer(String idPersonne, String motif) {
        synchronized (this) {
            this.idPersonne = null;
            prises.clear();
        }
        logger.warn("Enrôlement de la personne {} impossible : {}", idPersonne, motif);
        applicationEventPublisher.publishEvent(new EnrolementTermineEvent(idPersonne, 0, false));
    }

    /** Sort l'enrôlement de la mémoire courte. Appelée verrou tenu. */
    private Enrolement retirer() {
        Enrolement enrolement = new Enrolement(idPersonne, List.copyOf(prises), ecrireEnMemoireLongue);
        idPersonne = null;
        prises.clear();
        return enrolement;
    }

    /**
     * Écrit en mémoire longue et annonce le résultat, <b>hors du verrou</b>.
     * <p>
     * Les empreintes ne sont écrites qu'ici, en une fois : un enrôlement interrompu ne doit pas
     * laisser une personne à moitié apprise, reconnue une fois sur trois.
     * <p>
     * <b>Le résultat part quoi qu'il arrive</b>, y compris si l'écriture échoue. Même règle que
     * pour {@link #renoncer} : celui qui attend ne doit jamais rester suspendu. Sans cette garde,
     * un refus de la base a fait attendre dix secondes à l'activité de présentation, qui a conclu
     * à un visage mal vu — et l'exception, avalée par la boucle vidéo, n'a laissé aucune trace.
     */
    private void conclure(Enrolement enrolement) {
        boolean reussi = !enrolement.prises().isEmpty();
        if (reussi) {
            try {
                enrolement.ecrireEnMemoireLongue().accept(enrolement.prises());
                logger.info("Personne {} apprise : {} empreinte(s)", enrolement.idPersonne(), enrolement.prises().size());
            } catch (RuntimeException e) {
                reussi = false;
                logger.error("Empreintes de la personne {} non enregistrées : {}",
                        enrolement.idPersonne(), e.getMessage(), e);
            }
        } else {
            logger.warn("Enrôlement de la personne {} abandonné : aucun visage vu à temps", enrolement.idPersonne());
        }
        applicationEventPublisher.publishEvent(new EnrolementTermineEvent(
                enrolement.idPersonne(), reussi ? enrolement.prises().size() : 0, reussi));
    }

    /**
     * Un enrôlement sorti de la mémoire courte, avec de quoi le conclure sans plus rien lui
     * demander — la fonction d'écriture comprise, pour que le résultat aille bien là où celui qui
     * a lancé cet enrôlement-là l'attendait.
     */
    private record Enrolement(String idPersonne, List<PriseDeVisage> prises,
                              Consumer<List<PriseDeVisage>> ecrireEnMemoireLongue) {
    }
}
