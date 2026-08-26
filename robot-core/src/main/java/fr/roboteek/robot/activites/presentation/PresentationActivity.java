package fr.roboteek.robot.activites.presentation;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import fr.roboteek.robot.memoire.longterme.rencontre.JournalDesRencontres;
import fr.roboteek.robot.systemenerveux.event.DemandeEnrolementEvent;
import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import fr.roboteek.robot.systemenerveux.event.RencontreSansSuiteEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleTermineeEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.VisagePercu;
import fr.roboteek.robot.systemenerveux.event.VisagePercuEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Aborder quelqu'un qu'on ne connaît pas : le saluer, lui demander son prénom, apprendre son
 * visage.
 * <p>
 * Activité au sens strict — un script séquentiel et <b>exclusif</b>, qui monopolise la bouche et
 * l'oreille, bloque dans son {@code run()} et rend la main à la conversation en sortant.
 * Déclenchée par {@code DeclencheurAccueil} sur une rencontre d'inconnu, jamais par elle-même.
 * <p>
 * Quatre partis pris :
 * <ul>
 *   <li><b>salutations tirées au sort, sans IA</b> : une seconde de latence se voit sur un
 *       bonjour, et le naturel vient de la variété ;</li>
 *   <li><b>prénom extrait par l'IA</b> ({@link ExtractionPrenomIA}), parce que personne ne répond
 *       « Marie » tout court ;</li>
 *   <li><b>confirmation vocale systématique</b> : Vosk massacre les prénoms rares, et on n'écrit
 *       jamais dans la base des visages sans l'accord de la personne ;</li>
 *   <li><b>attente bornée</b> : deux relances puis abandon poli — une activité qui attend sans
 *       limite bloque le cerveau, et le robot devient sourd à tout le reste.</li>
 * </ul>
 * <p>
 * La personne est créée avant l'apprentissage de son visage (les empreintes la référencent et ne
 * peuvent pas la précéder) mais <b>effacée si le visage n'est pas appris</b> : rien ne subsiste
 * de quelqu'un que le robot ne saurait pas reconnaître.
 */
@Component
public class PresentationActivity extends AbstractActivity {

    private static final Logger logger = LoggerFactory.getLogger(PresentationActivity.class);

    /** Première demande comprise : une question, puis deux relances. */
    private static final int TENTATIVES_MAX = 3;

    /** Temps laissé à la personne pour répondre, une fois la question posée. */
    private static final long DELAI_REPONSE_MS = 8000;

    /** Délai effectivement appliqué : les tests le raccourcissent, sans quoi ils dureraient des minutes. */
    private final long delaiReponseMs;

    /**
     * Garde-fou sur l'attente de fin de parole. Ne devrait jamais servir — la parole signale
     * toujours sa fin, même en échec — mais une activité bloquée fige le cerveau entier.
     * <p>
     * Dix secondes et non trente : la phrase la plus longue en fait quatre, et ce délai est
     * intégralement subi à chaque question le jour où la parole ne répond plus. Vu sur le robot
     * après un plantage de la synthèse : la rencontre a duré une minute et quart, cerveau bloqué,
     * pour trois questions sans réponse.
     */
    private static final long DELAI_MAX_PAROLE_MS = 10000;

    /** Garde-fou sur l'attente d'enrôlement, largement au-dessus du délai de l'organe de vision. */
    private static final long DELAI_MAX_ENROLEMENT_MS = 10000;

    /** Pas de l'attente d'une réponse : le temps au bout duquel une reconnaissance est prise en compte. */
    private static final long TRANCHE_ATTENTE_MS = 250;

    private static final List<String> SALUTATIONS = List.of(
            "Bonjour ! Je ne crois pas te connaître. Comment tu t'appelles ?",
            "Tiens, un visage tout neuf ! Comment tu t'appelles ?",
            "Salut ! Moi c'est Wall-E. Et toi, comment tu t'appelles ?",
            "Oh ! Quelqu'un que je ne connais pas. Quel est ton prénom ?");

    /**
     * Dit juste avant d'apprendre le visage, et attendu jusqu'au bout.
     * <p>
     * <b>Mesuré sur le robot</b> : un enrôlement a échoué sur 43 prises d'affilée vues à plus de
     * 78° de lacet — la personne avait répondu « oui » puis s'était retournée vers son écran. Le
     * robot apprenait en silence, il n'y avait aucune raison qu'elle sache qu'il fallait le
     * regarder. Le dire coûte une phrase et se termine avant que la prise commence.
     */
    static final List<String> DEMANDES_DE_REGARD = List.of(
            "Super ! Regarde-moi bien en face, je mémorise ton visage.",
            "Parfait ! Tourne-toi vers moi une seconde, que je retienne ta tête.",
            "Génial ! Regarde-moi droit dans les yeux, j'enregistre.");

    private static final List<String> RELANCES = List.of(
            "Je n'ai pas bien entendu. Comment tu t'appelles ?",
            "Tu peux répéter ton prénom ?",
            "Pardon, je n'ai pas compris. Ton prénom, c'est quoi ?");

    /** Mots qui valent un oui. Tout le reste fait recommencer : on n'enregistre que sur accord. */
    private static final List<String> MOTS_AFFIRMATIFS = List.of(
            "oui", "ouais", "ouaip", "voila", "voilà", "exact", "exactement",
            "c'est ca", "c'est ça", "tout a fait", "tout à fait", "affirmatif");

    private final MemoireCourtTerme memoireCourtTerme;

    private final ExtractionPrenomIA extractionPrenomIA;

    private final PersonneRepository personneRepository;

    /** L'histoire des rencontres, à laquelle celle-ci ouvre le premier chapitre. */
    private final JournalDesRencontres journalDesRencontres;

    /**
     * Ce que le robot a entendu depuis la dernière question. Une file et non une variable : le
     * listener s'exécute sur un autre thread que le script, et {@code poll} avec délai est
     * exactement l'attente qu'on veut — bornée, et réveillée dès qu'une phrase arrive.
     */
    private final BlockingQueue<String> reponsesEntendues = new LinkedBlockingQueue<>();

    private final BlockingQueue<String> parolesTerminees = new LinkedBlockingQueue<>();

    private final BlockingQueue<EnrolementTermineEvent> enrolementsTermines = new LinkedBlockingQueue<>();

    /**
     * Quelqu'un de connu apparu <b>pendant</b> qu'on lui demandait son prénom : la question
     * n'avait pas lieu d'être, il n'y a plus qu'à s'en excuser.
     * <p>
     * C'est le rattrapage qui permet de ne plus chercher à être sûr avant de parler. Reconnaître
     * quelqu'un prend du temps — plus encore de profil, en bord de champ, ou pendant que la tête
     * bouge — et attendre cette certitude rendait le robot lent à aborder un vrai inconnu. Il
     * demande donc vite, et se corrige : « Excuse-moi Nicolas, je ne t'avais pas reconnu ! ».
     */
    private volatile VisagePercu visageReconnuEnCoursDeRoute;

    /**
     * {@code @Autowired} obligatoire ici : cette classe a deux constructeurs, et Spring n'en
     * choisit aucun d'office — il se rabat alors sur un constructeur vide, qui n'existe pas, et
     * le contexte entier échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public PresentationActivity(MemoireCourtTerme memoireCourtTerme,
                                ExtractionPrenomIA extractionPrenomIA,
                                PersonneRepository personneRepository,
                                JournalDesRencontres journalDesRencontres) {
        this(memoireCourtTerme, extractionPrenomIA, personneRepository, journalDesRencontres, DELAI_REPONSE_MS);
    }

    /** Permet aux tests de ne pas attendre huit secondes à chaque question sans réponse. */
    PresentationActivity(MemoireCourtTerme memoireCourtTerme,
                         ExtractionPrenomIA extractionPrenomIA,
                         PersonneRepository personneRepository,
                         JournalDesRencontres journalDesRencontres,
                         long delaiReponseMs) {
        this.memoireCourtTerme = memoireCourtTerme;
        this.extractionPrenomIA = extractionPrenomIA;
        this.personneRepository = personneRepository;
        this.journalDesRencontres = journalDesRencontres;
        this.delaiReponseMs = delaiReponseMs;
    }

    /**
     * Prioritaire : aborder quelqu'un ne se laisse pas interrompre en plein milieu, sous peine de
     * laisser la personne devant un robot qui lui a demandé son prénom et passe à autre chose.
     */
    @Override
    public int priorite() {
        return PRIORITE_HAUTE;
    }

    @Override
    public void init() {
        reponsesEntendues.clear();
        parolesTerminees.clear();
        enrolementsTermines.clear();
        visageReconnuEnCoursDeRoute = null;
        initialized = true;
    }

    @Override
    public boolean run() {
        String prenom = demanderLePrenom();
        if (visageReconnuEnCoursDeRoute != null) {
            return sExcuser();
        }
        if (prenom == null) {
            return stopActivity;
        }

        // La personne est écrite AVANT l'enrôlement, et effacée s'il échoue : un visage non appris
        // ne doit laisser aucune trace, mais les empreintes référencent la personne en base et ne
        // peuvent pas la précéder. Écrire après coûtait dix secondes de silence — l'insertion des
        // empreintes échouait sur la clé étrangère, et personne ne recevait jamais de verdict.
        Personne personne = Personne.nouvelle(prenom);
        personneRepository.enregistrer(personne);
        if (!apprendreLeVisage(personne.id())) {
            personneRepository.supprimer(personne.id());
            direEtAttendreLaFin("Je n'ai pas réussi à bien te regarder, " + prenom + ". Ce sera pour une prochaine fois !");
            return stopActivity;
        }

        Personne personneEnregistree = personne.rencontreeLe(LocalDateTime.now());
        personneRepository.enregistrer(personneEnregistree);
        journalDesRencontres.inscrirePremiereRencontre(personneEnregistree);
        // Le robot connaît cette personne avant de savoir la reconnaître : c'est le seul cas où
        // l'interlocuteur se pose au lieu de se déduire de ce qu'on voit.
        memoireCourtTerme.poserInterlocuteur(personneEnregistree);
        logger.info("Nouvelle connaissance : {} ({})", prenom, personne.id());
        direEtAttendreLaFin("Enchanté " + prenom + " ! Je me souviendrai de toi.");
        return stopActivity;
    }

    /**
     * Demande le prénom, le fait confirmer, et renonce poliment au bout de deux relances.
     *
     * @return le prénom accepté par la personne, ou {@code null} si l'échange n'a pas abouti
     */
    private String demanderLePrenom() {
        direEtAttendreLaFin(auHasard(SALUTATIONS));

        for (int tentative = 0; tentative < TENTATIVES_MAX && !stopActivity && visageReconnuEnCoursDeRoute == null; tentative++) {
            if (tentative > 0) {
                direEtAttendreLaFin(auHasard(RELANCES));
            }
            String prenom = extractionPrenomIA.extraire(ecouter());
            if (prenom == null) {
                continue;
            }
            direEtAttendreLaFin(prenom + ", c'est bien ça ?");
            if (estAffirmatif(ecouter())) {
                return prenom;
            }
            // Toute réponse qui n'est pas un oui franc renvoie à la question : mieux vaut
            // redemander que d'enregistrer un prénom de travers, qui suivrait la personne.
            logger.info("Prénom {} non confirmé", prenom);
        }

        if (!stopActivity && visageReconnuEnCoursDeRoute == null) {
            direEtAttendreLaFin("Tant pis, ce sera pour une autre fois !");
        }
        return null;
    }

    /**
     * Reconnaît son erreur et rend la main : la personne est connue, il n'y a ni prénom à demander
     * ni visage à apprendre.
     */
    private boolean sExcuser() {
        VisagePercu visage = visageReconnuEnCoursDeRoute;
        String prenom = visage.prenom();
        logger.info("{} reconnu pendant la présentation : la question n'avait pas lieu d'être", prenom);
        // Rien à poser : la mémoire court terme a reconnu cette personne d'elle-même, c'est
        // même ce qui a déclenché ces excuses.
        // Dit au registre de présence que sa rencontre d'inconnu était une méprise : sans cela, sa
        // temporisation ferait ignorer le prochain inconnu, le vrai, pendant deux minutes.
        applicationEventPublisher.publishEvent(new RencontreSansSuiteEvent(null, "méprise sur " + prenom));
        direEtAttendreLaFin("Excuse-moi " + prenom + ", je ne t'avais pas reconnu !");
        return stopActivity;
    }

    /**
     * Fait regarder la personne, demande à l'organe de vision d'apprendre le visage, et attend son
     * verdict.
     * <p>
     * La phrase est <b>attendue jusqu'à sa fin</b> avant de lancer la prise : c'est tout l'intérêt
     * — la personne se tourne pendant qu'il parle, et le relevé commence sur quelqu'un qui regarde.
     */
    private boolean apprendreLeVisage(String idPersonne) {
        direEtAttendreLaFin(auHasard(DEMANDES_DE_REGARD));
        enrolementsTermines.clear();
        applicationEventPublisher.publishEvent(new DemandeEnrolementEvent(idPersonne));
        EnrolementTermineEvent resultat = attendre(enrolementsTermines, DELAI_MAX_ENROLEMENT_MS);
        if (resultat == null) {
            logger.warn("Aucune réponse à la demande d'enrôlement de {}", idPersonne);
            return false;
        }
        return resultat.isReussi();
    }

    /**
     * Dit une phrase et attend qu'elle soit prononcée.
     * <p>
     * Sans cette attente, le délai laissé pour répondre commencerait à courir pendant que le robot
     * parle encore : sur une question de trois secondes, autant de perdu sur les huit accordées.
     */
    private void direEtAttendreLaFin(String texte) {
        parolesTerminees.clear();
        say(texte);
        if (attendre(parolesTerminees, DELAI_MAX_PAROLE_MS) == null) {
            logger.warn("Fin de parole jamais signalée pour « {} »", texte);
        }
    }

    /**
     * Écoute la prochaine phrase, dans la limite du délai accordé.
     * <p>
     * La file n'est délibérément pas vidée avant d'attendre : quelqu'un qui répond à l'instant même
     * où le robot finit sa question doit être entendu, et la vider ici ferait perdre exactement ces
     * réponses-là. Rien de parasite ne s'y accumule — la reconnaissance vocale est en pause tant
     * que le robot parle, il ne s'entend donc jamais lui-même.
     */
    private String ecouter() {
        // Attente découpée en tranches, pour une seule raison : être reconnu doit couper la
        // question tout de suite. D'un seul poll de huit secondes, le robot finirait sa relance
        // avant de s'apercevoir qu'il parle à quelqu'un qu'il connaît.
        long echeance = System.currentTimeMillis() + delaiReponseMs;
        while (System.currentTimeMillis() < echeance && !stopActivity && visageReconnuEnCoursDeRoute == null) {
            String reponse = attendre(reponsesEntendues, TRANCHE_ATTENTE_MS);
            if (reponse != null) {
                return reponse;
            }
        }
        if (visageReconnuEnCoursDeRoute == null) {
            logger.info("Pas de réponse au bout de {} ms", delaiReponseMs);
        }
        return null;
    }

    private static <T> T attendre(BlockingQueue<T> file, long delaiMs) {
        try {
            return file.poll(delaiMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Les trois écouteurs ci-dessous sont <b>synchrones</b>, à la différence de ceux de la
     * conversation : ils ne font que déposer dans une file, ce qui ne retient personne. Les rendre
     * asynchrones prendrait un thread du pool des évènements — celui-là même que la parole occupe
     * pendant toute la durée d'une phrase — pour une opération de quelques microsecondes.
     */
    @EventListener
    public void handleReconnaissanceVocaleEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (isActive() && reconnaissanceVocaleEvent.isProcessedByBrain()
                && StringUtils.isNotBlank(reconnaissanceVocaleEvent.getTexteReconnu())) {
            reponsesEntendues.add(reconnaissanceVocaleEvent.getTexteReconnu());
        }
    }

    @EventListener
    public void handleParoleTermineeEvent(ParoleTermineeEvent paroleTermineeEvent) {
        if (isActive()) {
            parolesTerminees.add(StringUtils.defaultString(paroleTermineeEvent.getTexte()));
        }
    }

    @EventListener
    public void handleEnrolementTermineEvent(EnrolementTermineEvent enrolementTermineEvent) {
        if (isActive()) {
            enrolementsTermines.add(enrolementTermineEvent);
        }
    }

    /**
     * Guette une reconnaissance tardive de la personne qu'on est en train d'aborder.
     * <p>
     * Le plus gros visage seulement, comme le regard : c'est celui à qui le robot parle. Sans cette
     * restriction, une photo connue posée à côté d'un vrai inconnu ferait s'excuser le robot auprès
     * de quelqu'un qui ne lui a rien dit.
     */
    @EventListener
    public void handleVisagePercuEvent(VisagePercuEvent visagePercuEvent) {
        if (!isActive() || visageReconnuEnCoursDeRoute != null || visagePercuEvent.getVisages() == null) {
            return;
        }
        visagePercuEvent.getVisages().stream()
                .max(Comparator.comparingLong(visage -> (long) visage.largeur() * visage.hauteur()))
                .filter(VisagePercu::estConnu)
                .ifPresent(visage -> visageReconnuEnCoursDeRoute = visage);
    }

    /** Vrai si la réponse vaut acceptation. Le silence et le doute n'en sont pas une. */
    private static boolean estAffirmatif(String reponse) {
        if (StringUtils.isBlank(reponse)) {
            return false;
        }
        String normalisee = reponse.trim().toLowerCase(Locale.FRENCH);
        return MOTS_AFFIRMATIFS.stream().anyMatch(normalisee::contains);
    }

    private static String auHasard(List<String> phrases) {
        return phrases.get(ThreadLocalRandom.current().nextInt(phrases.size()));
    }
}
