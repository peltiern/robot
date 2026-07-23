package fr.roboteek.robot.activites.akinator;

import fr.roboteek.robot.activites.AbstractActivity;
import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.eu.zajc.akiwrapper.Akiwrapper;
import org.eu.zajc.akiwrapper.AkiwrapperBuilder;
import org.eu.zajc.akiwrapper.core.entities.Guess;
import org.eu.zajc.akiwrapper.core.entities.Question;
import org.eu.zajc.akiwrapper.core.entities.Server;
import org.eu.zajc.akiwrapper.core.exceptions.ServerNotFoundException;
import org.eu.zajc.akiwrapper.core.utils.ApiKey;
import org.eu.zajc.akiwrapper.core.utils.UnirestUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Activité Akinator (jeu des devinettes).
 * <p>
 * Bean Spring singleton : le listener n'agit que lorsque l'activité est active
 * (voir {@link AbstractActivity}).
 */
@Component
public class AkinatorActivity extends AbstractActivity {

    private static final Logger logger = LoggerFactory.getLogger(AkinatorActivity.class);

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
     * Waiting response (écrite par le thread du listener, lue par la boucle du jeu).
     */
    private volatile String waitingResponse;

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
            logger.error("Combinaison langue/type de devinette invalide pour Akinator", e);
        }
    }

    @Override
    public boolean run() {
        playAnimation(Animation.RANDOM);
        say("Commençons à jouer !");

        // A list of rejected guesses, used to prevent them from repeating.
        List<Long> declined = new ArrayList<>();

        // Iterates while there are still questions left.
        while (akinator.getQuestion() != null && !finish && !stopActivity) {

            Question question = akinator.getQuestion();
            // Breaks the loop if question is null; /should/ not occur, but safety is still
            // first.
            if (question == null)
                break;

            // Say question
            logger.debug("Question #{} : {}", question.getStep() + 1, question.getQuestion());
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
                    if (akinator.getQuestion() != null) {
                        say(akinator.getQuestion().getQuestion());
                        answered = false;
                        waitingResponse = null;
                    }

                } else {
                    if (answer.equalsIgnoreCase("oui")) {
                        akinator.answer(Akiwrapper.Answer.YES);

                    } else if (answer.equalsIgnoreCase("non")) {
                        akinator.answer(Akiwrapper.Answer.NO);

                    } else if (answer.equalsIgnoreCase("je ne sais pas")) {
                        akinator.answer(Akiwrapper.Answer.DONT_KNOW);

                    } else if (answer.equalsIgnoreCase("probablement")) {
                        akinator.answer(Akiwrapper.Answer.PROBABLY);

                    } else if (answer.equalsIgnoreCase("probablement pas")) {
                        akinator.answer(Akiwrapper.Answer.PROBABLY_NOT);

                    } else if (answer.equalsIgnoreCase("annuler")) {
                        akinator.undoAnswer();

                    } else if (answer.equalsIgnoreCase("resetkey")) {
                        ApiKey.accquireApiKey(UnirestUtils.getInstance());

                    } else if (answer.equals("debug")) {
                        logger.debug("Serveur API courant : {} — nombre de propositions : {}",
                                akinator.getServer().getUrl(), akinator.getGuesses().size());
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
            Guess guess = akinator.suggestGuess();
            if (guess != null && guess.getProbability() > 0.25d && !declined.contains(guess.getIdLong())) {
                // Checks if this guess complies with the conditions.

                if (reviewGuess(guess)) {
                    // If the user accepts this guess.
                    finish(true);
                    return true;
                }

                declined.add(guess.getIdLong());
                // Registers this guess as rejected.
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
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug("reviewGuess → {}", isCharacter);
        return isCharacter;
    }

    private void finish(boolean win) {
        if (win) {
            // If Akinator has won.
            logger.debug("Akinator a gagné");
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
    @EventListener
    public void handleReconnaissanceVocalEvent(ReconnaissanceVocaleEvent speechRecognitionEvent) {
        if (isActive() && speechRecognitionEvent.isProcessedByBrain() && StringUtils.isNotEmpty(speechRecognitionEvent.getTexteReconnu())) {
            // Recognize speech
            waitingResponse = speechRecognitionEvent.getTexteReconnu();
        }
    }
}
