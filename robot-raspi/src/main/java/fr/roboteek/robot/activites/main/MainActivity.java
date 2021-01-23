package fr.roboteek.robot.activites.main;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

public class MainActivity extends AbstractActivity {

    /**
     * Intelligence artificielle (contenant le système conversationnel).
     */
    private IntelligenceArtificielleDialogFlow intelligenceArtificielleDialogFlow;
    private IntelligenceArtificielleGoogleKnowledge intelligenceArtificielleGoogleKnowledge;

    @Override
    public void init() {
        initialized = false;
        intelligenceArtificielleDialogFlow = new IntelligenceArtificielleDialogFlow();
        intelligenceArtificielleGoogleKnowledge = new IntelligenceArtificielleGoogleKnowledge();
        initialized = true;
    }

    @Override
    public boolean run() {
        playAnimation(Animation.RANDOM);
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

    @Subscribe
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent reconnaissanceVocaleEvent) {
        if (reconnaissanceVocaleEvent.isProcessedByBrain()) {
            final String texteReconnu = reconnaissanceVocaleEvent.getTexteReconnu();

            if (texteReconnu != null && !texteReconnu.equals("")) {
                // Conversation
                RequeteIntelligenceArtificielle requete = new RequeteIntelligenceArtificielle();
                requete.setInputText(texteReconnu);
                ReponseIntelligenceArtificielle reponse = intelligenceArtificielleDialogFlow.repondreARequete(requete);
                ReponseIntelligenceArtificielle firstResponse = reponse;
                if (reponse.isFallback()) {
                    reponse = intelligenceArtificielleGoogleKnowledge.repondreARequete(requete);
                }
                if (reponse.isFallback()) {
                    reponse = firstResponse;
                }
                playAnimation(Animation.NEUTRAL);
                say(reponse);
                playAnimation(Animation.RANDOM);
            }
        }
    }

    /**
     * Envoie un évènement pour dire du texte.
     *
     * @param reponse la réponse à dire
     */
    private void say(ReponseIntelligenceArtificielle reponse) {
        System.out.println("Réponse = " + reponse);
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(reponse.getOutputText());
        paroleEvent.setAudioContent(reponse.getOutputAudio());
        RobotEventBus.getInstance().publishAsync(paroleEvent);
    }
}
