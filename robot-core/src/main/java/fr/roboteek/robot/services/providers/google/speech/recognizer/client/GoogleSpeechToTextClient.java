package fr.roboteek.robot.services.providers.google.speech.recognizer.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request.RecognizeRequest;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.response.RecognizeResponse;

public interface GoogleSpeechToTextClient {

    @RequestLine("POST /v1/speech:recognize")
    @Headers({"Content-Type: application/json", "X-goog-api-key: {apiKey}"})
    RecognizeResponse recognize(RecognizeRequest request, @Param("apiKey") String apiKey);
}
