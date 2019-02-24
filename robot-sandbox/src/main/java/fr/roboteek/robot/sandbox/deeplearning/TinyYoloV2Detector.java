package fr.roboteek.robot.sandbox.deeplearning;

import org.datavec.image.loader.Java2DNativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.layers.objdetect.Yolo2OutputLayer;
import org.deeplearning4j.zoo.model.TinyYOLO;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Rectangle;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Détecteur d'objets utilisant le réseau de neurones Tiny Yolo V2.
 */
public class TinyYoloV2Detector extends ObjectDetector<String> {

    private static final int INPUT_WIDTH = 416;

    private static final int INPUT_HEIGHT = 416;

    private static final int GRID_WIDTH = 13;

    private static final int GRID_HEIGHT = 13;

    private static final double DETECTION_THRESHOLD = 0.5;

    private static final List<String> labelsClasses = new ArrayList<>();

    static {
        labelsClasses.addAll(Arrays.asList("aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car",
                "cat", "chair", "cow", "diningtable", "dog", "horse", "motorbike",
                "person", "pottedplant", "sheep", "sofa", "train", "tvmonitor"));
    }

    private ComputationGraph preTrained;

    private Yolo2OutputLayer outputLayer;

    private static final TinyYoloV2Detector INSTANCE = new TinyYoloV2Detector();

    private TinyYoloV2Detector() {
        try {
            preTrained = (ComputationGraph) TinyYOLO.builder().build().initPretrained();
            outputLayer = (Yolo2OutputLayer) preTrained.getOutputLayer(0);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static TinyYoloV2Detector getInstance() {
        return INSTANCE;
    }

    @Override
    public List<DetectedObject<String>> detect(MBFImage image) {
        long debut = System.currentTimeMillis();
        INDArray indArray = prepareImage(image);
        long fin = System.currentTimeMillis();
        System.out.println("PREPARE = " + (fin - debut));
        INDArray results = preTrained.outputSingle(indArray);
        long fin1 = System.currentTimeMillis();
        System.out.println("Results = " + (fin1 - fin));
        List<org.deeplearning4j.nn.layers.objdetect.DetectedObject> predictedObjects = outputLayer.getPredictedObjects(results, DETECTION_THRESHOLD);
        long fin2 = System.currentTimeMillis();
        System.out.println("Predict = " + (fin2 - fin1));
        if (!predictedObjects.isEmpty()) {
            System.out.println("results = " + predictedObjects);
        }
        return convertDetectedObjects(predictedObjects, image.getWidth(), image.getHeight());
    }

    private INDArray prepareImage(MBFImage image) {
        try {
            Java2DNativeImageLoader loader = new Java2DNativeImageLoader(INPUT_HEIGHT, INPUT_WIDTH, 3);
            ImagePreProcessingScaler imagePreProcessingScaler = new ImagePreProcessingScaler(0, 1);
            BufferedImage bi = ImageUtilities.createBufferedImage(image);
            // Transform BGR channels to RGB channels
            INDArray indArray = loader.asMatrix(bi, true);
            imagePreProcessingScaler.transform(indArray);
            return indArray;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private List<DetectedObject<String>> convertDetectedObjects(List<org.deeplearning4j.nn.layers.objdetect.DetectedObject> predictedObjects, int imageWidth, int imageHeight) {
        if (predictedObjects != null) {
            return predictedObjects.stream().map(detectedObject -> {
                double[] xy1 = detectedObject.getTopLeftXY();
                double[] xy2 = detectedObject.getBottomRightXY();
                int predictedClass = detectedObject.getPredictedClass();
                int x1 = (int) Math.round(imageWidth * xy1[0] / GRID_WIDTH);
                int y1 = (int) Math.round(imageHeight * xy1[1] / GRID_HEIGHT);
                int x2 = (int) Math.round(imageWidth * xy2[0] / GRID_WIDTH);
                int y2 = (int) Math.round(imageHeight * xy2[1] / GRID_HEIGHT);
                Rectangle bounds = new Rectangle(new Point2dImpl(x1, y1), new Point2dImpl(x2, y2));
                String label = labelsClasses.get(predictedClass);
                return new DetectedObject<>(label, label, bounds, detectedObject.getConfidence());
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }
}
