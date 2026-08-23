package fr.roboteek.robot.activites.retrouvailles;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.conversation.ConversationIA;
import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Retrouver quelqu'un qu'on connaît : le saluer par son prénom, en se rappelant de quoi on avait
 * parlé la dernière fois.
 * <p>
 * <b>Trois réactions selon la durée de l'absence</b>, et non deux : se taire si elle se compte en
 * secondes (la conversation n'a jamais été interrompue), reprendre le fil sans saluer si elle se
 * compte en minutes, saluer au-delà. Les deux seuils sont dans {@code robot.properties}.
 * <p>
 * <b>Aucun résumé à stocker</b> : {@link ConversationIA} tient déjà le fil de chacun en base. Le
 * souvenir est là ; il suffit de dire au robot qui il a en face et de lui demander une phrase.
 * <p>
 * Une activité et non un simple écouteur de {@code RencontreEvent} : c'est ce qui la fait passer
 * par l'arbitrage du cerveau — jamais un « bonjour Marie » par-dessus une présentation en cours.
 * Elle dure le temps d'une phrase, puis rend la main à la conversation, qui reprend en sachant à
 * qui elle parle.
 */
@Component
public class RetrouvaillesActivity extends AbstractActivity {

    private static final Logger logger = LoggerFactory.getLogger(RetrouvaillesActivity.class);

    /** Ce qui est dit quand l'IA ne répond pas : mieux vaut un bonjour plat que pas de bonjour. */
    private static final String SALUTATION_DE_SECOURS = "Salut %s ! Content de te revoir.";

    /** Et son équivalent quand la personne n'a fait que s'absenter un instant. */
    private static final String REPRISE_DE_SECOURS = "Te revoilà %s ! On en était où ?";

    private final MemoireCourtTerme memoireCourtTerme;

    private final ConversationIA conversationIA;

    /**
     * Les retrouvailles qu'on nous a confiées, par personne, en attendant que le cerveau en lance
     * une — s'il en lance une.
     * <p>
     * Par personne, et non dans un champ unique : deux venues peuvent se confirmer coup sur coup,
     * et la seconde écrasait la première. Le cerveau ne lance rien lui-même — il dépose la demande
     * et sa boucle la reprend plus tard —, si bien qu'un champ unique faisait saluer la dernière
     * personne annoncée sous le nom de celle pour qui l'activité avait été acceptée.
     * <p>
     * Une entrée dont la demande a été refusée reste ici jusqu'à la venue suivante de la même
     * personne, qui la remplace : la table est donc bornée par la taille du répertoire.
     */
    private final Map<String, Retrouvailles> confiees = new ConcurrentHashMap<>();

    /** Celles que le cerveau vient de lancer, remises par {@link #preparer}. */
    private volatile Retrouvailles enCours;

    /** Ce qu'il faut savoir pour saluer quelqu'un : qui, et depuis combien de temps absent. */
    private record Retrouvailles(Personne personne, long secondesDAbsence) {
    }

    @Autowired
    public RetrouvaillesActivity(MemoireCourtTerme memoireCourtTerme, ConversationIA conversationIA) {
        this.memoireCourtTerme = memoireCourtTerme;
        this.conversationIA = conversationIA;
    }

    /** Confie des retrouvailles, sans rien décider : c'est l'arbitrage qui tranchera. */
    public void confier(Personne personne, long secondesDAbsence) {
        confiees.put(personne.id(), new Retrouvailles(personne, secondesDAbsence));
    }

    /** Reprend ce qu'on avait confié, quand la demande n'a pas abouti. */
    public Personne reprendre(String idPersonne) {
        Retrouvailles retirees = idPersonne == null ? null : confiees.remove(idPersonne);
        return retirees == null ? null : retirees.personne();
    }

    @Override
    public void preparer(DemandeActiviteEvent demandeActiviteEvent) {
        enCours = reprendreRetrouvailles(demandeActiviteEvent.getIdPersonne());
    }

    private Retrouvailles reprendreRetrouvailles(String idPersonne) {
        return idPersonne == null ? null : confiees.remove(idPersonne);
    }

    @Override
    public void init() {
        initialized = true;
    }

    @Override
    public boolean run() {
        Retrouvailles retrouvailles = enCours;
        Personne personneRetrouvee = retrouvailles == null ? null : retrouvailles.personne();
        if (personneRetrouvee == null || StringUtils.isBlank(personneRetrouvee.prenom())) {
            logger.warn("Retrouvailles demandées sans personne : rien à dire");
            return stopActivity;
        }

        long secondesDAbsence = retrouvailles.secondesDAbsence();
        String prenom = personneRetrouvee.prenom();
        // Posé sans attendre : la salutation part sur le fil de cette personne, et la
        // conversation qui reprend derrière y reste même si un cycle la manque.
        memoireCourtTerme.poserInterlocuteur(personneRetrouvee);

        // Une absence de quelques secondes n'en est pas une : la personne s'est tournée, ou est
        // sortie du champ un instant. La conversation n'a jamais été interrompue, et la reprendre
        // à voix haute reviendrait à la couper.
        if (secondesDAbsence >= 0
                && secondesDAbsence < robotConfig().dureeSilenceAuRetourSecondes()) {
            logger.info("{} n'est parti que {} s : rien à dire, la conversation continue",
                    prenom, secondesDAbsence);
            return stopActivity;
        }

        // Une absence courte ne mérite pas un bonjour non plus : lui redire bonjour donnerait au
        // robot l'air de l'avoir oubliée entre-temps. On reprend le fil, sans saluer.
        boolean reprise = secondesDAbsence >= 0
                && secondesDAbsence < robotConfig().dureeRepriseSansSalutationSecondes();

        String phrase = conversationIA.saluerRetrouvailles(personneRetrouvee, secondesDAbsence, reprise);
        if (StringUtils.isBlank(phrase)) {
            phrase = (reprise ? REPRISE_DE_SECOURS : SALUTATION_DE_SECOURS).formatted(prenom);
        }
        logger.info("{} avec {} : {}", reprise ? "Reprise" : "Retrouvailles", prenom, phrase);
        say(phrase);
        return stopActivity;
    }
}
