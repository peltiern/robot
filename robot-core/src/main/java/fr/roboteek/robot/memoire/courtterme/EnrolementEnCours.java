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
 * Mémoire court terme, parce que c'est exactement ce que c'est — quelque chose que le robot a en
 * tête le temps de quelques images, et qui ne survit ni à son terme ni à un redémarrage. Ce qui
 * doit durer part dans la mémoire longue, {@code VisageConnu}, et seulement à la fin.
 * <p>
 * <b>Aucune image n'arrive ici</b> : l'organe de vision extrait l'empreinte pendant qu'il tient
 * le {@code Mat}, et ne transmet que le résultat — 128 flottants. Voir {@link SuiviDesVisages}
 * pour la même règle appliquée au suivi.
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
     *
     * @param visageLePlusProche rend l'empreinte et le portrait du visage auquel on parle, ou
     *                           {@code null} si aucun visage n'est visible. Volontairement
     *                           paresseux : l'extraction coûte ~68 ms, elle ne doit pas être
     *                           payée hors d'un enrôlement.
     */
    public synchronized void avancer(Supplier<PriseDeVisage> visageLePlusProche) {
        if (idPersonne == null) {
            return;
        }
        PriseDeVisage prise = visageLePlusProche.get();
        if (prise != null) {
            prises.add(prise);
        }
        if (prises.size() < EMPREINTES_ATTENDUES && System.currentTimeMillis() < echeanceMs) {
            return;
        }
        conclure();
    }

    /**
     * Renonce et répond quand même. Sert quand la reconnaissance n'est pas disponible : celui qui
     * attend le résultat ne doit jamais rester suspendu.
     */
    public synchronized void renoncer(String idPersonne, String motif) {
        logger.warn("Enrôlement de la personne {} impossible : {}", idPersonne, motif);
        this.idPersonne = null;
        prises.clear();
        applicationEventPublisher.publishEvent(new EnrolementTermineEvent(idPersonne, 0, false));
    }

    /**
     * Écrit en mémoire longue et annonce le résultat.
     * <p>
     * Les empreintes ne sont écrites qu'ici, en une fois : un enrôlement interrompu ne doit pas
     * laisser une personne à moitié apprise, reconnue une fois sur trois.
     * <p>
     * <b>Le résultat part quoi qu'il arrive</b>, y compris si l'écriture échoue. Même règle que
     * pour {@link #renoncer} : celui qui attend ne doit jamais rester suspendu. Sans cette garde,
     * un refus de la base a fait attendre dix secondes à l'activité de présentation, qui a conclu
     * à un visage mal vu — et l'exception, avalée par la boucle vidéo, n'a laissé aucune trace
     * dans les logs. Une panne d'écriture doit se dire, pas se déguiser en personne mal cadrée.
     */
    private void conclure() {
        String personne = idPersonne;
        List<PriseDeVisage> relevees = List.copyOf(prises);
        idPersonne = null;
        prises.clear();

        boolean reussi = !relevees.isEmpty();
        if (reussi) {
            try {
                ecrireEnMemoireLongue.accept(relevees);
                logger.info("Personne {} apprise : {} empreinte(s)", personne, relevees.size());
            } catch (RuntimeException e) {
                reussi = false;
                logger.error("Empreintes de la personne {} non enregistrées : {}", personne, e.getMessage(), e);
            }
        } else {
            logger.warn("Enrôlement de la personne {} abandonné : aucun visage vu à temps", personne);
        }
        applicationEventPublisher.publishEvent(
                new EnrolementTermineEvent(personne, reussi ? relevees.size() : 0, reussi));
    }
}
