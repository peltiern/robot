package fr.roboteek.robot.sandbox.ia.dialogflow;

import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.OutputAudioConfig;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.cloud.dialogflow.v2.TextInput.Builder;

import java.io.IOException;
import java.util.UUID;

public class DialogFlowTest {

    public static void main(String[] args) throws IOException {
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            SessionName session = SessionName.of("robot-1248", "1234567890"/*UUID.randomUUID().toString()*/);
            System.out.println("Session Path: " + session.toString());

            String text = "Je m'appelle Nicolas.";

            // Set the text (hello) and language code (en-US) for the query
            Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode("fr-FR");

            // Build the query with the TextInput
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

            // Performs the detect intent request
            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

            // Display the query result
            QueryResult queryResult = response.getQueryResult();

            System.out.println("====================");
            System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
            System.out.format("Detected Intent: %s (confidence: %f)\n",
                    queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
            System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
        }
    }
}
