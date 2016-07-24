package fr.roboteek.robot.organes.capteurs.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.AudioRequest;
import com.google.cloud.speech.v1.InitialRecognizeRequest;
import com.google.cloud.speech.v1.InitialRecognizeRequest.AudioEncoding;
import com.google.cloud.speech.v1.NonStreamingRecognizeResponse;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.ClientAuthInterceptor;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;

public class GoogleRecognizer {
	
	 private static final Logger logger =
		      Logger.getLogger(GoogleRecognizer.class.getName());

	private static final List<String> OAUTH2_SCOPES =
			Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

	private static final String host = "speech.googleapis.com";
	private static final int port = 443;
	private static final int samplingRate = 44100;

	private ManagedChannel channel;
	private SpeechGrpc.SpeechBlockingStub blockingStub;
	
	  /**
	   * Construct client connecting to Cloud Speech server at {@code host:port}.
	   */
	  public GoogleRecognizer() throws IOException {

	    GoogleCredentials creds = GoogleCredentials.getApplicationDefault();
	    creds = creds.createScoped(OAUTH2_SCOPES);
	    channel = NettyChannelBuilder.forAddress(host, port)
	        .negotiationType(NegotiationType.TLS)
	        .intercept(new ClientAuthInterceptor(creds, Executors.newSingleThreadExecutor()))
	        .build();
	    blockingStub = SpeechGrpc.newBlockingStub(channel);
	    logger.info("Created blockingStub for " + host + ":" + port);
	  }

	  private AudioRequest createAudioRequest(String fichier) throws IOException {
	    Path path = Paths.get(fichier);
	    return AudioRequest.newBuilder()
	        .setContent(ByteString.copyFrom(Files.readAllBytes(path)))
	        .build();
	  }

	  public void shutdown() throws InterruptedException {
	    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	  }

	  /** Send a non-streaming-recognize request to server. */
	  public String recognize(String fichierFlac) {
	    AudioRequest audio;
	    try {
	      audio = createAudioRequest(fichierFlac);
	    } catch (IOException e) {
	      logger.log(Level.WARNING, "Failed to read audio file: " + fichierFlac);
	      return null;
	    }
	    logger.info("Sending " + audio.getContent().size() + " bytes from audio file: " + fichierFlac);
	    InitialRecognizeRequest initial = InitialRecognizeRequest.newBuilder()
	        .setEncoding(AudioEncoding.FLAC)
	        .setSampleRate(samplingRate)
	        .setLanguageCode("FR")
	        .build();
	    RecognizeRequest request = RecognizeRequest.newBuilder()
	        .setInitialRequest(initial)
	        .setAudioRequest(audio)
	        .build();
	    NonStreamingRecognizeResponse response;
	    try {
	      response = blockingStub.nonStreamingRecognize(request);
	      if (response.getResponsesCount() > 0) {
	    	  RecognizeResponse resp = response.getResponses(0);
	    	  if (resp.getResultsCount() > 0) {
	    		  SpeechRecognitionResult result = resp.getResults(0);
	    		  if (result.getAlternativesCount() > 0) {
	    			  SpeechRecognitionAlternative alternative = result.getAlternatives(0);
	    			  return alternative.getTranscript();
	    		  }
	    	  }
	      }
	    } catch (StatusRuntimeException e) {
	      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
	      return null;
	    }
	    logger.info("Received response: " +  TextFormat.printToString(response));
	    return null;
	  }
}
