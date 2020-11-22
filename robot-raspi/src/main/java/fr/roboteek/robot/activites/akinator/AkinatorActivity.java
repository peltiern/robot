package fr.roboteek.robot.activites.akinator;

import com.markozajc.akiwrapper.Akiwrapper;
import com.markozajc.akiwrapper.AkiwrapperBuilder;
import com.markozajc.akiwrapper.core.entities.Guess;
import com.markozajc.akiwrapper.core.entities.Question;
import com.markozajc.akiwrapper.core.entities.Server;
import com.markozajc.akiwrapper.core.entities.impl.immutable.ApiKey;
import com.markozajc.akiwrapper.core.exceptions.ServerNotFoundException;
import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleGoogle;
import fr.roboteek.robot.organes.capteurs.CapteurVocalSimple;
import fr.roboteek.robot.systemenerveux.event.DetectionVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.speech.recognizer.SpeechRecognizer;
import fr.roboteek.robot.util.speech.recognizer.google.GoogleSpeechRecognizer;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AkinatorActivity extends AbstractActivity {

    public static final double PROBABILITY_THRESHOLD = 0.85;

    /** Speech recognizer. */
    private SpeechRecognizer speechRecognizer;

    /** Akinator engine. */
    private Akiwrapper akinator;

    /** Waiting response. */
    private String waitingResponse;

    @Override
    public void init() {
        // TODO Gestion dynamique de la reconnaisance vocale (par fichier de config à un niveau supérieur)
       speechRecognizer = GoogleSpeechRecognizer.getInstance();

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
        } catch (ServerNotFoundException e) {
            System.err.println("Invalid combination of language and guess type. Try a different guess type.");
        }
    }

    @Override
    public void run() {
        Thread threadActivity = new Thread(this::runActivity);
        threadActivity.start();

    }

    private void runActivity() {
        say("Commençons à jouer !");

        // A list of rejected guesses, used to prevent them from repeating.
        List<Long> declined = new ArrayList<>();

        boolean finish = false;

        // Iterates while there are still questions left.
        while (akinator.getCurrentQuestion() != null && !finish) {

            Question question = akinator.getCurrentQuestion();
            // Breaks the loop if question is null; /should/ not occur, but safety is still
            // first.
            if (question == null)
                break;

            // Say question
            System.out.println("Question #" + (question.getStep() + 1));
            System.out.println("\t" + question.getQuestion());
            say(question.getQuestion());

            // Wait for answer
            answerQuestion();


            finish = reviewGuesses(declined);
        }

        if (!finish) {
            for (Guess guess : akinator.getGuesses()) {
                if (reviewGuess(guess)) {
                    // Reviews all final guesses.
                    finish(true);
                    return;
                }
            }

            finish(false);
        }
    }

    private void answerQuestion() {
        boolean answered = false;
        waitingResponse = null;
        while (!answered) {
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
            if (guess.getProbability() > 0.85d && !declined.contains(guess.getIdLong())) {
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
        while (!answered) {
            // Asks the user if that is his character.

            String answer = StringUtils.isNotBlank(waitingResponse) ? waitingResponse.toLowerCase() : "";
            if (StringUtils.isNotBlank(answer)) {
                switch (answer) {
                    case "oui":
                        // If the user has responded positively.
                        answered = true;
                        isCharacter = true;
                        break;

                    case "non":
                        // If the user has responded negatively.
                        answered = true;
                        isCharacter = false;
                        break;

                    default:
                        break;
                }
            }
        }

        return isCharacter;
    }

    private void finish(boolean win) {
        if (win) {
            // If Akinator has won.
            say("Cool ! J'ai trouvé !");
        } else {
            // If the user has won.
            say("Bravo ! Tu as réussi à me battre !");
        }
    }

    @Override
    public void stop() {

    }

    /**
     * Intercepts speech detection events
     *
     * @param speechDetectionEvent the speech detection event
     */
    @Handler
    public void handleSpeechDetectionEvent(DetectionVocaleEvent speechDetectionEvent) {
        System.out.println("EVENEMENT DETECTION VOCALE" + speechDetectionEvent.getCheminFichier());
        if (speechDetectionEvent != null && StringUtils.isNotEmpty(speechDetectionEvent.getCheminFichier())) {
            // Recognize speech
            waitingResponse = speechRecognizer.recognize(speechDetectionEvent.getCheminFichier());
            System.out.println("Réponse reconnue = " + waitingResponse);
        }
    }

    public static void main(String[] args) {
        AkinatorActivity activity = new AkinatorActivity();
        activity.init();
        RobotEventBus.getInstance().subscribe(activity);

        CapteurVocalSimple capteurVocalSimple = new CapteurVocalSimple();
        capteurVocalSimple.initialiser();
        OrganeParoleGoogle organeParoleGoogle = new OrganeParoleGoogle();
        organeParoleGoogle.initialiser();

        RobotEventBus.getInstance().subscribe(capteurVocalSimple);
        RobotEventBus.getInstance().subscribe(organeParoleGoogle);
        activity.runActivity();
    }
}
