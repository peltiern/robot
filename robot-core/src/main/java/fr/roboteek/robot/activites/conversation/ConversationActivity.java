package fr.roboteek.robot.activites.conversation;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.main.ReponseIntelligenceArtificielle;
import fr.roboteek.robot.activites.main.RequeteIntelligenceArtificielle;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import org.apache.commons.lang3.StringUtils;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Activité de conversation avec l'IA (Claude via Spring AI).
 * <p>
 * Bean Spring singleton : le listener n'agit que lorsque l'activité est active
 * (voir {@link AbstractActivity}).
 */
@Component
public class ConversationActivity extends AbstractActivity {

    private final ConversationIA conversationIA;

    /**
     * Personne avec qui le robot converse, {@code null} tant qu'elle n'est pas identifiée.
     * Détermine le fil de mémoire employé : chacun a le sien.
     */
    private volatile String interlocuteur;

    private final Logger logger = LoggerFactory.getLogger(ConversationActivity.class);

    public ConversationActivity(ConversationIA conversationIA) {
        this.conversationIA = conversationIA;
    }

    /**
     * Désigne la personne à qui le robot parle. Volontairement pas remis à zéro par
     * {@link #init()} : l'interlocuteur est posé <i>avant</i> de rendre la main à cette
     * activité (par celle qui vient de le reconnaître ou de faire sa connaissance).
     */
    public void setInterlocuteur(String interlocuteur) {
        this.interlocuteur = interlocuteur;
    }

    @Override
    public void init() {
        // La conversation (ChatClient + mémoire persistante) est un bean injecté : rien à initialiser
        initialized = true;
    }

    @Override
    public boolean run() {
        //playAnimation(Animation.RANDOM);
        while (!stopActivity) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Plus de « OK. On arrête de parler. » ici : cette sortie n'est plus seulement l'extinction
        // du robot (qui dit déjà « Au revoir. », les deux phrases se chevauchaient), c'est aussi le
        // passage à une autre activité — annoncer la fin de la conversation y serait à contretemps.
        return stopActivity;
    }

    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (isActive() && reconnaissanceVocaleEvent.isProcessedByBrain()) {
            final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

            if (StringUtils.isNotBlank(texteReconnu)) {
                // Pause de la reconnaissance dès le début de la réflexion : sinon, ce qui est dit
                // pendant que l'IA réfléchit s'accumule et le robot répond à chaque demande.
                // La reconnaissance est relancée par OrganeParole à la fin de la réponse vocale.
                publierControleReconnaissance(ReconnaissanceVocaleControleEvent.CONTROLE.METTRE_EN_PAUSE);
                try {
                    // Conversation
                    RequeteIntelligenceArtificielle requete = new RequeteIntelligenceArtificielle();
                    requete.setInputText(texteReconnu);
                    ReponseIntelligenceArtificielle reponse = conversationIA.repondreARequete(requete, interlocuteur);
                    if (reponse != null && StringUtils.isNotBlank(reponse.getOutputText())) {
                        // Pas d'Animation.NEUTRAL avant de répondre : elle recentrait cou et yeux à
                        // chaque phrase, effaçant la pose du robot — y compris, désormais, le regard
                        // posé sur son interlocuteur, qu'il perdrait de vue en lui répondant.
                        say(reponse.getOutputText());
                    } else {
                        // Rien à dire : relancer la reconnaissance nous-mêmes, sinon le robot reste sourd
                        publierControleReconnaissance(ReconnaissanceVocaleControleEvent.CONTROLE.DEMARRER);
                    }
                } catch (Exception e) {
                    // Retour vocal en cas d'échec : sans ce catch, l'exception partirait dans
                    // l'AsyncUncaughtExceptionHandler et le robot resterait muet (quota épuisé,
                    // réseau coupé, clé absente...). La parole relance aussi la reconnaissance.
                    logger.error("Échec de l'appel à l'IA pour le texte « {} »", texteReconnu, e);
                    say("Je n'arrive pas à réfléchir.");
                }
            }
        }
    }

    /**
     * Publie un évènement de contrôle de la reconnaissance vocale (pause/reprise).
     * La publication est synchrone : le capteur vocal est mis en pause immédiatement.
     */
    private void publierControleReconnaissance(ReconnaissanceVocaleControleEvent.CONTROLE controle) {
        ReconnaissanceVocaleControleEvent event = new ReconnaissanceVocaleControleEvent();
        event.setControle(controle);
        applicationEventPublisher.publishEvent(event);
    }
}
