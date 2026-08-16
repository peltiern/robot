package fr.roboteek.robot.activites.retrouvailles;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.conversation.ConversationIA;
import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Retrouver quelqu'un qu'on connaît : le saluer par son prénom, en se rappelant de quoi on avait
 * parlé la dernière fois.
 * <p>
 * <b>Trois réactions selon la durée de l'absence</b>, et non deux : se taire si elle se compte en
 * secondes (la conversation n'a jamais été interrompue), reprendre le fil sans saluer si elle se
 * compte en minutes, saluer au-delà. Les deux seuils sont dans {@code robot.properties}.
 * <p>
 * <b>Il n'y a pas de résumé à stocker</b>, contrairement à ce qui était prévu au départ : la
 * mémoire de conversation est déjà tenue <i>par personne</i> et persistée
 * ({@link ConversationIA}, cent messages par fil en base). Le souvenir est donc déjà là ; tout
 * ce qui manquait, c'était de dire au robot qui il a en face et de lui demander d'en tirer une
 * phrase. Le champ {@code resumeDerniereConversation} prévu à l'origine sur {@link Personne} n'a
 * donc jamais servi, et a été retiré.
 * <p>
 * Une activité et non un simple écouteur de {@code RencontreEvent} : c'est ce qui la fait passer
 * par l'arbitrage du cerveau, donc jamais un « bonjour Marie » par-dessus une présentation en
 * cours, et une temporisation si quelqu'un fait des allers-retours devant la caméra.
 * <p>
 * Elle dure le temps d'une phrase, puis rend la main à la conversation — qui reprend en sachant
 * à qui elle parle, sur le fil de mémoire de cette personne.
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
     * La personne à saluer, posée par {@code DeclencheurAccueil} <b>avant</b> de réclamer
     * l'activité — comme l'interlocuteur de la conversation. Une demande d'activité ne transporte
     * qu'un identifiant, elle ne peut pas la porter elle-même.
     */
    private volatile Personne personne;

    /** Temps écoulé depuis la dernière rencontre, en secondes ; {@code -1} si on ne sait pas. */
    private volatile long secondesDAbsence = -1;

    @Autowired
    public RetrouvaillesActivity(MemoireCourtTerme memoireCourtTerme, ConversationIA conversationIA) {
        this.memoireCourtTerme = memoireCourtTerme;
        this.conversationIA = conversationIA;
    }

    public void setPersonneRetrouvee(Personne personne, long secondesDAbsence) {
        this.personne = personne;
        this.secondesDAbsence = secondesDAbsence;
    }

    @Override
    public void init() {
        initialized = true;
    }

    @Override
    public boolean run() {
        Personne personneRetrouvee = personne;
        if (personneRetrouvee == null || StringUtils.isBlank(personneRetrouvee.prenom())) {
            logger.warn("Retrouvailles demandées sans personne : rien à dire");
            return stopActivity;
        }

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
