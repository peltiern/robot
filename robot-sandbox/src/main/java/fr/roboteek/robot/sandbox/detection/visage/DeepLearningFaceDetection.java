package fr.roboteek.robot.sandbox.detection.visage;

import org.bytedeco.javacpp.indexer.FloatIndexer;

import static org.bytedeco.javacpp.opencv_core.CV_32F;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_dnn.Net;

import static org.bytedeco.javacpp.opencv_dnn.blobFromImage;
import static org.bytedeco.javacpp.opencv_dnn.readNetFromCaffe;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_videoio.*;

import org.bytedeco.javacpp.opencv_videoio.VideoCapture;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.util.Map;

/**
 * Created on Jul 28, 2018
 *
 * @author Taha Emara
 * Email : taha@emaraic.com
 * <p>
 * This example does face detection using deep learning model which provides a
 * great accuracy compared to OpenCV face detection using Haar cascades.
 * <p>
 * This example is based on this code
 * https://github.com/opencv/opencv/blob/master/modules/dnn/misc/face_detector_accuracy.py
 * <p>
 * To run this example you need two files: deploy.prototxt can be downloaded
 * from
 * https://github.com/opencv/opencv/blob/master/samples/dnn/face_detector/deploy.prototxt
 * <p>
 * and res10_300x300_ssd_iter_140000.caffemodel
 * https://github.com/opencv/opencv_3rdparty/blob/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel
 */
public class DeepLearningFaceDetection {

    private static final String PROTO_FILE = "/home/npeltier/Robot/Programme/detection-visage/reseaux-neurones/deploy.prototxt";
    private static final String CAFFE_MODEL_FILE = "/home/npeltier/Robot/Programme/detection-visage/reseaux-neurones/res10_300x300_ssd_iter_140000.caffemodel";
    private static final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
    private static Net net = null;

    private static final HaarFaceDetector faceDetector = new HaarFaceDetector();

    static {
        net = readNetFromCaffe(PROTO_FILE, CAFFE_MODEL_FILE);
    }

    private static void detectAndDraw(Mat image) {//detect faces and draw a blue rectangle arroung each face

        resize(image, image, new Size(300, 300));//resize the image to match the input size of the model

        //create a 4-dimensional blob from image with NCHW (Number of images in the batch -for training only-, Channel, Height, Width) dimensions order,
        //for more detailes read the official docs at https://docs.opencv.org/trunk/d6/d0f/group__dnn.html#gabd0e76da3c6ad15c08b01ef21ad55dd8
        Mat blob = blobFromImage(image, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0, 0), false, false, CV_32F);

        net.setInput(blob);//set the input to network model
        Mat output = net.forward();//feed forward the input to the netwrok to get the output matrix

        Mat ne = new Mat(new Size(output.size(3), output.size(2)), CV_32F, output.ptr(0, 0));//extract a 2d matrix for 4d output matrix with form of (number of detections x 7)

        FloatIndexer srcIndexer = ne.createIndexer(); // create indexer to access elements of the matric

        for (int i = 0; i < output.size(3); i++) {//iterate to extract elements
            float confidence = srcIndexer.get(i, 2);
            float f1 = srcIndexer.get(i, 3);
            float f2 = srcIndexer.get(i, 4);
            float f3 = srcIndexer.get(i, 5);
            float f4 = srcIndexer.get(i, 6);
            if (confidence > .6) {
                float tx = f1 * 300;//top left point's x
                float ty = f2 * 300;//top left point's y
                float bx = f3 * 300;//bottom right point's x
                float by = f4 * 300;//bottom right point's y
                rectangle(image, new Rect(new Point((int) tx, (int) ty), new Point((int) bx, (int) by)), new Scalar(255, 0, 0, 0));//print blue rectangle
            }
        }
    }

    private static void detectAndDrawByHaarCascade(Mat image) {//detect faces and draw a blue rectangle arroung each face

        resize(image, image, new Size(300, 300));//resize the image to match the input size of the model

        //create a 4-dimensional blob from image with NCHW (Number of images in the batch -for training only-, Channel, Height, Width) dimensions order,
        //for more detailes read the official docs at https://docs.opencv.org/trunk/d6/d0f/group__dnn.html#gabd0e76da3c6ad15c08b01ef21ad55dd8
        Mat blob = blobFromImage(image, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0, 0), false, false, CV_32F);

        net.setInput(blob);//set the input to network model
        Mat output = net.forward();//feed forward the input to the netwrok to get the output matrix

        Mat ne = new Mat(new Size(output.size(3), output.size(2)), CV_32F, output.ptr(0, 0));//extract a 2d matrix for 4d output matrix with form of (number of detections x 7)

        FloatIndexer srcIndexer = ne.createIndexer(); // create indexer to access elements of the matric

        Map<opencv_core.CvRect, Mat> detectedFaces = faceDetector.detectByMat(image);

        for (Map.Entry<opencv_core.CvRect, Mat> face : detectedFaces.entrySet()) {//iterate to extract elements
            opencv_core.CvRect rect = face.getKey();
            rectangle(image, new Rect(new Point(rect.x(), rect.y()), new Point(rect.width() + rect.x(), rect.height() + rect.y())), new Scalar(255, 0, 0, 0));//print blue rectangle
        }
    }

    public static void main(String[] args) {
        VideoCapture capture = new VideoCapture();
        capture.set(CAP_PROP_FRAME_WIDTH, 640);
        capture.set(CAP_PROP_FRAME_HEIGHT, 480);
        //capture.set(CAP_PROP_FPS, 1);

        if (!capture.open(0)) {
            System.out.println("Can not open the cam !!!");
        }

        Mat colorimg = new Mat();

        CanvasFrame mainframe = new CanvasFrame("Face Detection", CanvasFrame.getDefaultGamma() / 2.2);
        mainframe.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        mainframe.setCanvasSize(640, 480);
        mainframe.setLocationRelativeTo(null);
        mainframe.setVisible(true);

        int i = 0;
        while (true) {
            while (capture.read(colorimg) && mainframe.isVisible()) {
                //if (i % 5 == 0) {
                    detectAndDraw(colorimg);
                    //detectAndDrawByHaarCascade(colorimg);
                    mainframe.showImage(converter.convert(colorimg));
               // }
                i++;
//                try {
//                    Thread.sleep(50);
//                } catch (InterruptedException ex) {
//                    System.out.println(ex.getMessage());
//                }

            }
        }
    }

}
