package fr.roboteek.robot.activites.conversation.openai;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.activites.main.ReponseIntelligenceArtificielle;
import fr.roboteek.robot.activites.main.RequeteIntelligenceArtificielle;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationPresets;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;

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
                e.printStackTrace();
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

    @Subscribe
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (reconnaissanceVocaleEvent.isProcessedByBrain()) {
            final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

            if (texteReconnu != null && !texteReconnu.equals("")) {
                // Conversation
                RequeteIntelligenceArtificielle requete = new RequeteIntelligenceArtificielle();
                requete.setInputText(texteReconnu);
                ReponseIntelligenceArtificielle reponse = openAIConversation.repondreARequete(requete);
                ReponseIntelligenceArtificielle firstResponse = reponse;
                playAnimation(AnimationPresets.NEUTRAL);
                say(reponse.getOutputText());
            }
        }
    }
}
