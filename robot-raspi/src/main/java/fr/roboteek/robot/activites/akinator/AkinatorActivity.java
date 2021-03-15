package fr.roboteek.robot.activites.akinator;

import com.google.common.eventbus.Subscribe;
import com.markozajc.akiwrapper.Akiwrapper;
import com.markozajc.akiwrapper.AkiwrapperBuilder;
import com.markozajc.akiwrapper.core.entities.Guess;
import com.markozajc.akiwrapper.core.entities.Question;
import com.markozajc.akiwrapper.core.entities.Server;
import com.markozajc.akiwrapper.core.entities.impl.immutable.ApiKey;
import com.markozajc.akiwrapper.core.exceptions.ServerNotFoundException;
import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleGoogle;
import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.organes.capteurs.CapteurVocalAvecReconnaissance;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AkinatorActivity extends AbstractActivity {

    public static final double PROBABILITY_THRESHOLD = 0.85;

    /**
     * Akinator engine.
     */
    private Akiwrapper akinator;

    /**
     * Flag to indicate if the game is finished.
     */
    private boolean finish;

    /**
     * Waiting response.
     */
    private String waitingResponse;

    @Override
    public void init() {

        initialized = false;

        // TODO Gérer l'âge de la personne
        Boolean filterProfanity = Boolean.TRUE;

        // TODO mettre une propriété dans le fichier de config
        Server.Language language = Server.Language.FRENCH;

        // TODO Pouvoir choisir le type d'entité recherchée
        Server.GuessType guessType = Server.GuessType.OBJECT;

        try {
            akinator = new AkiwrapperBuilder().setFilterProfanity(filterProfanity)
                    .setLanguage(language)
                    .setGuessType(guessType)
                    .build();
            initialized = true;
        } catch (ServerNotFoundException e) {
            System.err.println("Invalid combination of language and guess type. Try a different guess type.");
        }
    }

    @Override
    public boolean run() {
        playAnimation(Animation.RANDOM);
        say("Commençons à jouer !");

        // A list of rejected guesses, used to prevent them from repeating.
        List<Long> declined = new ArrayList<>();

        // Iterates while there are still questions left.
        while (akinator.getCurrentQuestion() != null && !finish && !stopActivity) {

            Question question = akinator.getCurrentQuestion();
            // Breaks the loop if question is null; /should/ not occur, but safety is still
            // first.
            if (question == null)
                break;

            // Say question
            System.out.println("Question #" + (question.getStep() + 1));
            System.out.println("\t" + question.getQuestion());
            playAnimation(Animation.NEUTRAL);
            say(question.getQuestion());

            // Wait for answer
            answerQuestion();

            finish = reviewGuesses(declined);
        }

        if (!finish && !stopActivity) {
            for (Guess guess : akinator.getGuesses()) {
                if (reviewGuess(guess)) {
                    // Reviews all final guesses.
                    finish(true);
                    return stopActivity;
                }
            }

            finish(false);
        }

        say("A bientôt pour une nouvelle partie.");
        return stopActivity;
    }

    private void answerQuestion() {
        boolean answered = false;
        waitingResponse = null;
        playAnimation(Animation.RANDOM);
        while (!answered && !stopActivity) {
            // Iterates while the questions remains unanswered.

            String answer = waitingResponse;

            if (StringUtils.isNotBlank(answer)) {
                if (answer.equalsIgnoreCase("répète")) {
                    if (akinator.getCurrentQuestion() != null) {
                        say(akinator.getCurrentQuestion().getQuestion());
                        answered = false;
                        waitingResponse = null;
                    }

                } else {
                    if (answer.equalsIgnoreCase("oui")) {
                        akinator.answerCurrentQuestion(Akiwrapper.Answer.YES);

                    } else if (answer.equalsIgnoreCase("non")) {
                        akinator.answerCurrentQuestion(Akiwrapper.Answer.NO);

                    } else if (answer.equalsIgnoreCase("je ne sais pas")) {
                        akinator.answerCurrentQuestion(Akiwrapper.Answer.DONT_KNOW);

                    } else if (answer.equalsIgnoreCase("probablement")) {
                        akinator.answerCurrentQuestion(Akiwrapper.Answer.PROBABLY);

                    } else if (answer.equalsIgnoreCase("probablement pas")) {
                        akinator.answerCurrentQuestion(Akiwrapper.Answer.PROBABLY_NOT);

                    } else if (answer.equalsIgnoreCase("annuler")) {
                        akinator.undoAnswer();

                    } else if (answer.equalsIgnoreCase("resetkey")) {
                        ApiKey.accquireApiKey();

                    } else if (answer.equals("debug")) {
                        System.out.println("Debug information:\n\tCurrent API server: "
                                + akinator.getServer().getUrl()
                                + "\n\tCurrent guess count: "
                                + akinator.getGuesses().size());
                        continue;
                        // Displays some debug information.

                    } else {
                        continue;
                    }

                    answered = true;
                    // Answers the question.
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean reviewGuesses(List<Long> declined) {
        for (Guess guess : akinator.getGuessesAboveProbability(PROBABILITY_THRESHOLD)) {
            if (guess.getProbability() > 0.25d && !declined.contains(guess.getIdLong())) {
                // Checks if this guess complies with the conditions.

                if (reviewGuess(guess)) {
                    // If the user accepts this guess.
                    finish(true);
                    return true;
                }

                declined.add(guess.getIdLong());
                // Registers this guess as rejected.
            }

        }
        return false;
    }

    private boolean reviewGuess(Guess guess) {
        // Displays the guess.
        say(guess.getName());

        boolean answered = false;
        boolean isCharacter = false;
        waitingResponse = null;
        say("Est-ce que j'ai trouvé ?");
        while (!answered && !stopActivity) {
            // Asks the user if that is his character.
            System.out.println("reviewGuess : answered = " + answered + ", stopActivity" + stopActivity);
            String answer = StringUtils.isNotBlank(waitingResponse) ? waitingResponse.toLowerCase() : "";
            if (StringUtils.isNotBlank(answer)) {
                switch (answer) {
                    case "oui":
                        // If the user has responded positively.
                        System.out.println("REPONSE TROUVEE");
                        answered = true;
                        isCharacter = true;
                        break;

                    case "non":
                        // If the user has responded negatively.
                        answered = true;
                        isCharacter = false;
                        break;

                    default:
                        System.out.println("reviewGuess DEFAULT");
                        break;
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("reviewGuess RETURN " + isCharacter);
        return isCharacter;
    }

    private void finish(boolean win) {
        if (win) {
            // If Akinator has won.
            System.out.println("ANIMATION VICTOIRE");
            playAnimation(Animation.AMAZED);
            say("Cool ! J'ai trouvé !");
        } else {
            // If the user has won.
            playAnimation(Animation.SAD);
            say("Bravo ! Tu as réussi à me battre !");
        }
    }

    /**
     * Intercepts speech recognition events
     *
     * @param speechRecognitionEvent the speech recognition event
     */
    @Subscribe
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent speechRecognitionEvent) {
        if (speechRecognitionEvent.isProcessedByBrain() && StringUtils.isNotEmpty(speechRecognitionEvent.getTexteReconnu())) {
            // Recognize speech
            waitingResponse = speechRecognitionEvent.getTexteReconnu();
            System.out.println(Thread.currentThread().getName() + " " + this + " --Réponse reconnue = " + waitingResponse);
        }
    }

    public static void main(String[] args) {
        AkinatorActivity activity = new AkinatorActivity();
        activity.init();
        RobotEventBus.getInstance().subscribe(activity);

        CapteurVocalAvecReconnaissance capteurVocalAvecReconnaissance = new CapteurVocalAvecReconnaissance();
        capteurVocalAvecReconnaissance.initialiser();
        capteurVocalAvecReconnaissance.start();
        OrganeParoleGoogle organeParoleGoogle = new OrganeParoleGoogle();
        organeParoleGoogle.initialiser();

        RobotEventBus.getInstance().subscribe(capteurVocalAvecReconnaissance);
        RobotEventBus.getInstance().subscribe(organeParoleGoogle);
        activity.run();
    }
}
