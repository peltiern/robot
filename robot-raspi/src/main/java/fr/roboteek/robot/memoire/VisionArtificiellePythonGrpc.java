package fr.roboteek.robot.memoire;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import fr.roboteek.robot.Constantes;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.example.dto.image.ImageProcessingRequest;
import org.example.dto.image.ImageProcessingResponse;
import org.example.dto.image.ImageProcessingServiceGrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class VisionArtificiellePythonGrpc {

    public static final String FACE_RECOGNITION = "face-recognition";
    public static final String FACE_DETECTION = "face-detection";

    public static final String OBJECT_DETECTION = "object-detection";
    private final String PYTHON_CMD = "/usr/bin/python3.10";
    private final String VISION_ARTIFICIELLE_PYTHON_SERVER_FILE = Constantes.DOSSIER_VISION_ARTIFICIELLE + File.separator + "serveur-python" + File.separator + "ai-server-grpc.py";
    private final String KNOWN_FACES_FOLDER = Constantes.DOSSIER_VISION_ARTIFICIELLE + File.separator + "known-faces";

    private final ImageProcessingServiceGrpc.ImageProcessingServiceBlockingStub blockingStub;

    private final Gson gson;

    public VisionArtificiellePythonGrpc() {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("192.168.0.102", 50051)
                .usePlaintext()
                .build();
        blockingStub = ImageProcessingServiceGrpc.newBlockingStub(channel);

        gson = new Gson();

//        // Lancement du serveur Python de reconnaissance faciale
//        System.out.println("serveur = " + VISION_ARTIFICIELLE_PYTHON_SERVER_FILE + " " + KNOWN_FACES_FOLDER);
//        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_CMD, VISION_ARTIFICIELLE_PYTHON_SERVER_FILE, KNOWN_FACES_FOLDER);
//        final Process process;
//        final BufferedReader reader;
//        try {
//            process = processBuilder.start();
//            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        final Thread threadServer = new Thread("VisionArtificielleServeur") {
//            @Override
//            public void run() {
//
//                try {
//
//                    // Obligé de lire la sortie standard du processus pour ne pas que le processus JAVA se bloque ???
//                    while (reader.readLine() != null);
//
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//
//            }
//        };
//        threadServer.start();
    }

    public FacialRecognitionResponse recognizeFaces(byte[] image) {
        return traiterImage(image, FacialRecognitionResponse.class, FACE_RECOGNITION);
    }

    public FacialRecognitionResponse detectFaces(byte[] image) {
        return traiterImage(image, FacialRecognitionResponse.class, FACE_DETECTION);
    }

    ////    public void apprendreVisagesPersonne(List<FImage> listeVisages, String prenomPersonne) {
////
////    }

    public ObjectDetectionResponse detectObjects(byte[] image) {
        return traiterImage(image, ObjectDetectionResponse.class, OBJECT_DETECTION);
    }

    private <T> T traiterImage(byte[] image, Class<T> responseClass, String type) {
        try {
//            System.out.println("Traitement image");
            ImageProcessingRequest request = ImageProcessingRequest.newBuilder()
                    .setImage(ByteString.copyFrom(image))
                    .setProcessingType(type)
                    .build();
            ImageProcessingResponse response = blockingStub.processImage(request);

            return gson.fromJson(response.getJsonResponse(), responseClass);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().value() == Status.UNAVAILABLE.getCode().value()) {
                return null;
            } else {
                e.printStackTrace();
                throw new RuntimeException("Erreur lors du traitement de l'image");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement de l'image");
        }
    }

}
