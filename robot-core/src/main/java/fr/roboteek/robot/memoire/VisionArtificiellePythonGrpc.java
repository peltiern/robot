package fr.roboteek.robot.memoire;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import fr.roboteek.robot.services.visionartificielle.dto.image.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class VisionArtificiellePythonGrpc {

    public static final String FACE_RECOGNITION = "face-recognition";
    public static final String FACE_DETECTION = "face-detection";

    public static final String OBJECT_DETECTION = "object-detection";

    private final ImageProcessingServiceGrpc.ImageProcessingServiceBlockingStub blockingStub;

    private final Gson gson;

    public VisionArtificiellePythonGrpc() {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        blockingStub = ImageProcessingServiceGrpc.newBlockingStub(channel);

        gson = new Gson();
    }

    public FacialRecognitionResponse recognizeFaces(byte[] image) {
        return traiterImage(image, FacialRecognitionResponse.class, FACE_RECOGNITION);
    }

    public FacialRecognitionResponse detectFaces(byte[] image) {
        return traiterImage(image, FacialRecognitionResponse.class, FACE_DETECTION);
    }

    public void apprendreVisagesPersonne(byte[] image, String prenomPersonne) {
        try {
            AddFaceRequest request = AddFaceRequest.newBuilder()
                    .setImage(ByteString.copyFrom(image))
                    .setName(prenomPersonne)
                    .build();
            AddFaceResponse response = blockingStub.addFace(request);

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().value() != Status.UNAVAILABLE.getCode().value()) {
                e.printStackTrace();
                throw new RuntimeException("Erreur lors de l'apprentissage d'une personne");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'apprentissage d'une personne");
        }

    }

    public ObjectDetectionResponse detectObjects(byte[] image) {
        return traiterImage(image, ObjectDetectionResponse.class, OBJECT_DETECTION);
    }

    private <T> T traiterImage(byte[] image, Class<T> responseClass, String type) {
        try {
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
