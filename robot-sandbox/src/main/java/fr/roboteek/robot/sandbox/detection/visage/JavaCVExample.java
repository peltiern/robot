package fr.roboteek.robot.sandbox.detection.visage;

import fr.roboteek.robot.sandbox.detection.visage.util.ImageUtils;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.Point;
import static org.bytedeco.javacpp.opencv_core.Scalar;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * An example to demonstrate JavaCV's frame grabbing and other features
 *
 * @author Imesha Sudasingha
 */
public class JavaCVExample {

    private FFmpegFrameGrabber frameGrabber;
    private OpenCVFrameConverter.ToMat toMatConverter = new OpenCVFrameConverter.ToMat();
    private volatile boolean running = false;

    private HaarFaceDetector faceDetector = new HaarFaceDetector();
    private CNNAgeDetector ageDetector = new CNNAgeDetector();
    private CNNGenderDetector genderDetector = new CNNGenderDetector();

    private JFrame window;
    private JPanel videoPanel;

    public JavaCVExample() {
        window = new JFrame();
        videoPanel = new JPanel();

        window.setLayout(new BorderLayout());
        window.setSize(new Dimension(1280, 720));
        window.add(videoPanel, BorderLayout.CENTER);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
    }

    /**
     * Starts the frame grabbers and then the frame processing. Grabbed and processed frames will be displayed in the
     * {@link #videoPanel}
     */
    public void start() {
        frameGrabber = new FFmpegFrameGrabber("/dev/video0");
        frameGrabber.setFormat("video4linux2");
        frameGrabber.setImageWidth(640);
        frameGrabber.setImageHeight(480);

        System.out.println("Starting frame grabber");
        try {
            frameGrabber.start();
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException("Unable to start the FrameGrabber", e);
        }

        SwingUtilities.invokeLater(() -> {
            window.setVisible(true);
        });

        process();

    }

    /**
     * Private method which will be called to star frame grabbing and carry on processing the grabbed frames
     */
    private void process() {
        running = true;
        int i = 0;
        while (running) {
            try {
                // Here we grab frames from our camera
                final Frame frame = frameGrabber.grab();
                Mat mat = toMatConverter.convert(frame);
//                if (i % 20 == 0) {

                    Map<CvRect, Mat> detectedFaces = faceDetector.detect(frame);

                    detectedFaces.entrySet().forEach(rectMatEntry -> {
                        String age = ageDetector.predictAge(rectMatEntry.getValue(), frame);
                        CNNGenderDetector.Gender gender = genderDetector.predictGender(rectMatEntry.getValue(), frame);

                        String caption = String.format("%s:[%s]", gender, age);
                        System.out.println("Face's caption : " + caption);

                        rectangle(mat, new Point(rectMatEntry.getKey().x(), rectMatEntry.getKey().y()),
                                new Point(rectMatEntry.getKey().width() + rectMatEntry.getKey().x(), rectMatEntry.getKey().height() + rectMatEntry.getKey().y()),
                                Scalar.RED, 2, CV_AA, 0);

                        int posX = Math.max(rectMatEntry.getKey().x() - 10, 0);
                        int posY = Math.max(rectMatEntry.getKey().y() - 10, 0);
                        putText(mat, caption, new Point(posX, posY), CV_FONT_HERSHEY_PLAIN, 1.0,
                                new Scalar(255, 255, 255, 2.0));
                    });
//                }
                i++;

                // Show the processed mat in UI
                Frame processedFrame = toMatConverter.convert(mat);

                Graphics graphics = videoPanel.getGraphics();
                BufferedImage resizedImage = ImageUtils.getResizedBufferedImage(processedFrame, videoPanel);
                SwingUtilities.invokeLater(() -> {
                    graphics.drawImage(resizedImage, 0, 0, videoPanel);
                });
            } catch (FrameGrabber.Exception e) {
            } catch (Exception e) {
            }
        }
    }

    /**
     * Stops and released resources attached to frame grabbing. Stops frame processing and,
     */
    public void stop() {
        running = false;
        try {
            System.out.println("Releasing and stopping FrameGrabber");
            frameGrabber.release();
            frameGrabber.stop();
        } catch (FrameGrabber.Exception e) {
        }

        window.dispose();
    }

    public static void main(String[] args) {
        JavaCVExample javaCVExample = new JavaCVExample();

        System.out.println("Starting javacv example");
        new Thread(javaCVExample::start).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping javacv example");
            javaCVExample.stop();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {
        }
    }
}
