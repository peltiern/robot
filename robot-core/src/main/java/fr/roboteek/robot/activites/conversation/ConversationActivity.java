package fr.roboteek.robot.activites.conversation;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.main.ReponseIntelligenceArtificielle;
import fr.roboteek.robot.activites.main.RequeteIntelligenceArtificielle;
import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
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

    private final Logger logger = LoggerFactory.getLogger(ConversationActivity.class);

    public ConversationActivity(ConversationIA conversationIA) {
        this.conversationIA = conversationIA;
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
        say("OK. On arrête de parler.");
        return stopActivity;
    }

    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (isActive() && reconnaissanceVocaleEvent.isProcessedByBrain()) {
            final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

            if (texteReconnu != null && !texteReconnu.isEmpty()) {
                try {
                    // Conversation
                    RequeteIntelligenceArtificielle requete = new RequeteIntelligenceArtificielle();
                    requete.setInputText(texteReconnu);
                    ReponseIntelligenceArtificielle reponse = conversationIA.repondreARequete(requete);
                    playAnimation(Animation.NEUTRAL);
                    say(reponse.getOutputText());
                } catch (Exception e) {
                    // Retour vocal en cas d'échec : sans ce catch, l'exception partirait dans
                    // l'AsyncUncaughtExceptionHandler et le robot resterait muet (quota épuisé,
                    // réseau coupé, clé absente...)
                    logger.error("Échec de l'appel à l'IA pour le texte « {} »", texteReconnu, e);
                    say("Je n'arrive pas à réfléchir.");
                }
            }
        }
    }
}
