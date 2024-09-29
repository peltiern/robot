package fr.roboteek.robot.services.providers.google.speech.synthesizer.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.request.SynthesizeRequest;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.response.SynthesizeResponse;

public interface GoogleTextToSpeechClient {

    @RequestLine("POST /v1/text:synthesize")
    @Headers({"Content-Type: application/json", "X-goog-api-key: {apiKey}"})
    SynthesizeResponse synthesizeText(SynthesizeRequest request, @Param("apiKey") String apiKey);
}
