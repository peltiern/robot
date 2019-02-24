package fr.roboteek.robot.sandbox.detection.visage;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacv.Frame;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_dnn.*;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * The class responsible for recognizing gender. This class use the concept of CNN (Convolution Neural Networks) to
 * identify the gender of a detected face.
 *
 * @author Imesha Sudasingha
 */
public class CNNGenderDetector {

    private Net genderNet;

    public CNNGenderDetector() {
            genderNet = new Net();
            genderNet = readNetFromCaffe("/home/npeltier/Robot/Programme/detection-visage/reseaux-neurones/deploy_gendernet.prototxt", "/home/npeltier/Robot/Programme/detection-visage/reseaux-neurones/gender_net.caffemodel");
    }

    /**
     * Predicts gender of a given cropped face
     *
     * @param face  the cropped face as a {@link Mat}
     * @param frame the original frame where the face was cropped from
     * @return Gender
     */
    public Gender predictGender(Mat face, Frame frame) {
        try {
            Mat croppedMat = new Mat();
            resize(face, croppedMat, new Size(256, 256));
            normalize(croppedMat, croppedMat, 0, Math.pow(2, frame.imageDepth), NORM_MINMAX, -1, null);

//            Blob inputBlob = new Blob(croppedMat);
//            genderNet.setBlob(".data", inputBlob);
            Mat blob = blobFromImage(croppedMat);
            genderNet.setInput(blob);
            Mat predictions = genderNet.forward();
//            Blob prob = genderNet.getBlob("prob");

            Indexer indexer = predictions.createIndexer();
            if (indexer.getDouble(0, 0) > indexer.getDouble(0, 1)) {
                System.out.println("Male detected");
                return Gender.MALE;
            } else {
                System.out.println("Female detected");
                return Gender.FEMALE;
            }
        } catch (Exception e) {
        }
        return Gender.NOT_RECOGNIZED;
    }

    public enum Gender {
        MALE,
        FEMALE,
        NOT_RECOGNIZED
    }
}
