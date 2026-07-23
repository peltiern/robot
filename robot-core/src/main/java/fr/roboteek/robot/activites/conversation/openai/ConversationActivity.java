package fr.roboteek.robot.activites.conversation.openai;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.main.ReponseIntelligenceArtificielle;
import fr.roboteek.robot.activites.main.RequeteIntelligenceArtificielle;
import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Activité de conversation avec OpenAI.
 * <p>
 * Bean Spring singleton : le listener n'agit que lorsque l'activité est active
 * (voir {@link AbstractActivity}).
 */
@Component
public class ConversationActivity extends AbstractActivity {

    private OpenAIConversation openAIConversation;

    @Override
    public void init() {
        initialized = false;
        openAIConversation = new OpenAIConversation();
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

    @Override
    public void stop() {
        openAIConversation.close();
        super.stop();
    }

    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (isActive() && reconnaissanceVocaleEvent.isProcessedByBrain()) {
            final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

            if (texteReconnu != null && !texteReconnu.equals("")) {
                // Conversation
                RequeteIntelligenceArtificielle requete = new RequeteIntelligenceArtificielle();
                requete.setInputText(texteReconnu);
                ReponseIntelligenceArtificielle reponse = openAIConversation.repondreARequete(requete);
                ReponseIntelligenceArtificielle firstResponse = reponse;
                playAnimation(Animation.NEUTRAL);
                say(reponse.getOutputText());
            }
        }
    }
}
