package fr.roboteek.robot.sandbox.detection.visage;

import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

/**
 * Face detector using haar classifier cascades
 *
 * @author Imesha Sudasingha
 */
public class HaarFaceDetecteur implements Detecteur<Map<CvRect, Mat>> {

    private opencv_objdetect.CvHaarClassifierCascade haarClassifierCascade;
    private CvMemStorage storage;

    public HaarFaceDetecteur() {
        try {
            File haarCascade = new File("/home/npeltier/Robot/Programme/detection-visage/haar-cascade/haarcascade_frontalface_alt.xml");
            haarClassifierCascade = new opencv_objdetect.CvHaarClassifierCascade(cvLoad(haarCascade.getAbsolutePath()));
        } catch (Exception e) {
            throw new IllegalStateException("Error when trying to get the haar cascade", e);
        }
        storage = CvMemStorage.create();
    }


    @Override
    public void finalize() {
        cvReleaseMemStorage(storage);
    }

    @Override
    public Map<CvRect, Mat> detecter(Mat matImage) {
        Map<CvRect, Mat> detectedFaces = new HashMap<>();

        IplImage iplImage = new IplImage(matImage);

        /*
         * return a CV Sequence (kind of a list) with coordinates of rectangle face area.
         * (returns coordinates of left top corner & right bottom corner)
         */
        CvSeq detectObjects = cvHaarDetectObjects(iplImage, haarClassifierCascade, storage, 1.5, 3, CV_HAAR_DO_CANNY_PRUNING);

        int numberOfPeople = detectObjects.total();
        for (int i = 0; i < numberOfPeople; i++) {
            CvRect rect = new CvRect(cvGetSeqElem(detectObjects, i));
            Mat croppedMat = matImage.apply(new Rect(rect.x(), rect.y(), rect.width(), rect.height()));
            detectedFaces.put(rect, croppedMat);
        }

        return detectedFaces;
    }
}
