package fr.roboteek.robot.sandbox.deeplearning;

import org.datavec.image.loader.Java2DNativeImageLoader;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.layers.objdetect.DetectedObject;
import org.deeplearning4j.nn.layers.objdetect.Yolo2OutputLayer;
import org.deeplearning4j.zoo.model.TinyYOLO;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.*;


public class TinyYoloPrediction {

    private ComputationGraph preTrained;
    private List<DetectedObject> predictedObjects;
    private HashMap<Integer, String> map;

    private TinyYoloPrediction() {
        try {
            preTrained = (ComputationGraph) TinyYOLO.builder().build().initPretrained();
            prepareLabels();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final TinyYoloPrediction INSTANCE = new TinyYoloPrediction();

    public static TinyYoloPrediction getINSTANCE() {
        return INSTANCE;
    }

    public void markWithBoundingBox(MBFImage image, int imageWidth, int imageHeight, boolean newBoundingBOx) throws Exception {
        int width = 416;
        int height = 416;
        int gridWidth = 13;
        int gridHeight = 13;
        double detectionThreshold = 0.5;

        Yolo2OutputLayer outputLayer = (Yolo2OutputLayer) preTrained.getOutputLayer(0);
        if (newBoundingBOx) {
            long debut = System.currentTimeMillis();
            INDArray indArray = prepareImage(image, width, height);
            long fin = System.currentTimeMillis();
            System.out.println("PREPARE = " + (fin - debut));
            INDArray results = preTrained.outputSingle(indArray);
            long fin1 = System.currentTimeMillis();
            System.out.println("Results = " + (fin1 - fin));
            predictedObjects = outputLayer.getPredictedObjects(results, detectionThreshold);
            long fin2 = System.currentTimeMillis();
            System.out.println("Predict = " + (fin2 - fin1));
            if (!predictedObjects.isEmpty()) {
                System.out.println("results = " + predictedObjects);
            }
            markWithBoundingBox(image, gridWidth, gridHeight, imageWidth, imageHeight);
            long fin3 = System.currentTimeMillis();
            System.out.println("MARK = " + (fin3 - fin2));
        } else {
            markWithBoundingBox(image, gridWidth, gridHeight, imageWidth, imageHeight);
        }
    }

    private INDArray prepareImage(MBFImage image, int width, int height) throws IOException {
//        NativeImageLoader loader = new NativeImageLoader(height, width, 3);
        Java2DNativeImageLoader loader = new Java2DNativeImageLoader(height, width, 3);
        ImagePreProcessingScaler imagePreProcessingScaler = new ImagePreProcessingScaler(0, 1);
        BufferedImage bi = ImageUtilities.createBufferedImage(image);
//        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
//        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
//        mat.put(0, 0, data);
//        Mat mat =  Imgcodecs.imdecode(new MatOfByte(image.toByteImage()), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        INDArray indArray = loader.asMatrix(bi, true);
        imagePreProcessingScaler.transform(indArray);
        return indArray;
    }

    private void prepareLabels() {
        if (map == null) {
            String s = "aeroplane\n" + "bicycle\n" + "bird\n" + "boat\n" + "bottle\n" + "bus\n" + "car\n" +
                    "cat\n" + "chair\n" + "cow\n" + "diningtable\n" + "dog\n" + "horse\n" + "motorbike\n" +
                    "person\n" + "pottedplant\n" + "sheep\n" + "sofa\n" + "train\n" + "tvmonitor";
            String[] split = s.split("\\n");
            int i = 0;
            map = new HashMap<>();
            for (String s1 : split) {
                map.put(i++, s1);
            }
        }
    }

    private void markWithBoundingBox(MBFImage image, int gridWidth, int gridHeight, int w, int h) {

        if (predictedObjects == null) {
            return;
        }
        ArrayList<DetectedObject> detectedObjects = new ArrayList<>(predictedObjects);

        while (!detectedObjects.isEmpty()) {
            Optional<DetectedObject> max = detectedObjects.stream().max((o1, o2) -> ((Double) o1.getConfidence()).compareTo(o2.getConfidence()));
            if (max.isPresent()) {
                DetectedObject maxObjectDetect = max.get();
                removeObjectsIntersectingWithMax(detectedObjects, maxObjectDetect);
                detectedObjects.remove(maxObjectDetect);
                markWithBoundingBox(image, gridWidth, gridHeight, w, h, maxObjectDetect);
            }
        }
    }

    private static void removeObjectsIntersectingWithMax(ArrayList<DetectedObject> detectedObjects, DetectedObject maxObjectDetect) {
        double[] bottomRightXY1 = maxObjectDetect.getBottomRightXY();
        double[] topLeftXY1 = maxObjectDetect.getTopLeftXY();
        List<DetectedObject> removeIntersectingObjects = new ArrayList<>();
        for (DetectedObject detectedObject : detectedObjects) {
            double[] topLeftXY = detectedObject.getTopLeftXY();
            double[] bottomRightXY = detectedObject.getBottomRightXY();
            double iox1 = Math.max(topLeftXY[0], topLeftXY1[0]);
            double ioy1 = Math.max(topLeftXY[1], topLeftXY1[1]);

            double iox2 = Math.min(bottomRightXY[0], bottomRightXY1[0]);
            double ioy2 = Math.min(bottomRightXY[1], bottomRightXY1[1]);

            double inter_area = (ioy2 - ioy1) * (iox2 - iox1);

            double box1_area = (bottomRightXY1[1] - topLeftXY1[1]) * (bottomRightXY1[0] - topLeftXY1[0]);
            double box2_area = (bottomRightXY[1] - topLeftXY[1]) * (bottomRightXY[0] - topLeftXY[0]);

            double union_area = box1_area + box2_area - inter_area;
            double iou = inter_area / union_area;


            if (iou > 0.5) {
                removeIntersectingObjects.add(detectedObject);
            }

        }
        detectedObjects.removeAll(removeIntersectingObjects);
    }

    private void markWithBoundingBox(MBFImage image, int gridWidth, int gridHeight, int w, int h, DetectedObject obj) {

        double[] xy1 = obj.getTopLeftXY();
        double[] xy2 = obj.getBottomRightXY();
        int predictedClass = obj.getPredictedClass();
        int x1 = (int) Math.round(w * xy1[0] / gridWidth);
        int y1 = (int) Math.round(h * xy1[1] / gridHeight);
        int x2 = (int) Math.round(w * xy2[0] / gridWidth);
        int y2 = (int) Math.round(h * xy2[1] / gridHeight);
        image.drawPolygon(new Polygon(Arrays.asList(new Point2dImpl(x1, y1), new Point2dImpl(x1, y2), new Point2dImpl(x2, y2), new Point2dImpl(x2, y1))), 3, RGBColour.RED);
        image.drawText(map.get(predictedClass), new Point2dImpl(x1 + 2, y2 - 2), HersheyFont.ROMAN_SIMPLEX, 15, RGBColour.RED);
//        rectangle(file, new Point(x1, y1), new Point(x2, y2), Scalar.RED);
//        putText(file, map.get(predictedClass), new Point(x1 + 2, y2 - 2), FONT_HERSHEY_DUPLEX, 1, Scalar.GREEN);
    }

}