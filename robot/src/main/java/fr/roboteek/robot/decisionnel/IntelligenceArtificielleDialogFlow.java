package fr.roboteek.robot.decisionnel;

import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import org.apache.log4j.Logger;

import java.io.IOException;

public class IntelligenceArtificielleDialogFlow {

    /** Session DialogFlow. */
    private SessionsClient sessionsClient;

    private SessionName session;

    /** Logger. */
    private Logger logger = Logger.getLogger(IntelligenceArtificielleDialogFlow.class);

    public IntelligenceArtificielleDialogFlow() {
        try {
            sessionsClient = SessionsClient.create();
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            session = SessionName.of("robot-1248", "1234567890"/*UUID.randomUUID().toString()*/);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Répond à une phrase.
     * @param phrase la phrase
     * @return la réponse issue de l'intelligence artificielle
     */
    public String repondreAPhrase(String phrase) {
        // Set the text and language code for the query
        TextInput.Builder textInput = TextInput.newBuilder().setText(phrase).setLanguageCode("fr-FR");

        // Build the query with the TextInput
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

        // Performs the detect intent request
        DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

        // Display the query result
        QueryResult queryResult = response.getQueryResult();

        String responseText = queryResult.getFulfillmentText();

        logger.debug("Query Text: " + queryResult.getQueryText());
        logger.debug("Detected Intent: " + queryResult.getIntent().getDisplayName() + " (confidence: " + queryResult.getIntentDetectionConfidence() + ")");
        logger.debug("Fulfillment Text: " + queryResult.getFulfillmentText());

        return responseText;
    }
}
