package fr.roboteek.robot.sandbox.openimaj;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openimaj.feature.FloatFV;
import org.openimaj.feature.FloatFVComparison;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.alignment.CLMAligner;
import org.openimaj.image.processing.face.detection.CLMDetectedFace;
import org.openimaj.image.processing.face.feature.LocalLBPHistogram;
import org.openimaj.image.processing.face.feature.comparison.FaceFVComparator;
import org.openimaj.image.processing.face.feature.comparison.FacialFeatureComparator;
import org.openimaj.image.processing.face.recognition.AnnotatorFaceRecogniser;
import org.openimaj.image.processing.face.tracking.clm.CLMFaceTracker;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.io.IOUtils;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.ml.annotation.AnnotatedObject;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.ml.annotation.basic.KNNAnnotator;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;

public class VideoFaceRecognition extends KeyAdapter implements VideoDisplayListener<MBFImage> {
    private VideoCapture capture;
    private VideoDisplay<MBFImage> videoFrame;

    private AnnotatorFaceRecogniser<CLMDetectedFace, String> recogniser;
    private CLMFaceTracker engine;
    private FImage currentFrame;

    public VideoFaceRecognition() throws Exception {
        capture = new VideoCapture(320, 240);
        engine = new CLMFaceTracker();
        engine.fpd = 120;
        // engine.fcheck = true;

        videoFrame = VideoDisplay.createVideoDisplay(capture);
        videoFrame.addVideoListener(this);
        SwingUtilities.getRoot(videoFrame.getScreen()).addKeyListener(this);

        final LocalLBPHistogram.Extractor<CLMDetectedFace> extractor = new
                LocalLBPHistogram.Extractor<CLMDetectedFace>(
                new CLMAligner(), 20, 20, 8, 1);
        final FacialFeatureComparator<LocalLBPHistogram> comparator = new
                FaceFVComparator<LocalLBPHistogram, FloatFV>(
                FloatFVComparison.EUCLIDEAN);
        final KNNAnnotator<CLMDetectedFace, String, LocalLBPHistogram> knn = KNNAnnotator.create(extractor, comparator,
                1, 5f);
        // final CLMShapeFeature.Extractor extractor = new
        // CLMShapeFeature.Extractor();
        // final FacialFeatureComparator<CLMShapeFeature> comparator = new
        // FaceFVComparator<CLMShapeFeature, DoubleFV>(
        // DoubleFVComparison.EUCLIDEAN);
        //
        // final KNNAnnotator<CLMDetectedFace, String,
        // CLMShapeFeature.Extractor, CLMShapeFeature> knn =
        // KNNAnnotator.create(extractor, comparator, 1, 5f);

        recogniser = AnnotatorFaceRecogniser.create(knn);
    }

    @Override
    public synchronized void keyPressed(KeyEvent key) {
        if (key.getKeyCode() == KeyEvent.VK_SPACE) {
            this.videoFrame.togglePause();
        } else if (key.getKeyChar() == 'c') {
            // if (!this.videoFrame.isPaused())
            // this.videoFrame.togglePause();

            final String person = JOptionPane.showInputDialog(this.videoFrame.getScreen(), "", "",
                    JOptionPane.QUESTION_MESSAGE);

            final List<CLMDetectedFace> faces = detectFaces();
            if (faces.size() == 1) {
                recogniser.train(new AnnotatedObject<CLMDetectedFace, String>(faces.get(0), person));
            } else {
                System.out.println("Wrong number of faces found");
            }

            // this.videoFrame.togglePause();
        } else if (key.getKeyChar() == 'd') {
            engine.reset();
        }
        // else if (key.getKeyChar() == 'q') {
        // if (!this.videoFrame.isPaused())
        // this.videoFrame.togglePause();
        //
        // final List<CLMDetectedFace> faces = detectFaces();
        // if (faces.size() == 1) {
        // System.out.println("Looks like: " +
        // recogniser.annotate(faces.get(0)));
        // } else {
        // System.out.println("Wrong number of faces found");
        // }
        //
        // this.videoFrame.togglePause();
        // }
        else if (key.getKeyChar() == 's') {
            try {
                final File f = new File("rec.bin");
                f.delete();
                IOUtils.writeBinaryFull(f, this.recogniser);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        } else if (key.getKeyChar() == 'l') {
            try {
                final File f = new File("rec.bin");
                this.recogniser = IOUtils.read(f);
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private List<CLMDetectedFace> detectFaces() {
        return CLMDetectedFace.convert(engine.model.trackedFaces, currentFrame);
    }

    @Override
    public void afterUpdate(VideoDisplay<MBFImage> display) {
        // do nothing
    }

    @Override
    public synchronized void beforeUpdate(MBFImage frame) {
        this.currentFrame = frame.flatten();
        engine.track(frame);
        engine.drawModel(frame, true, true, true, true, true);

        if (recogniser != null && recogniser.listPeople().size() >= 1) {
            for (final CLMDetectedFace f : detectFaces()) {
                final List<ScoredAnnotation<String>> name = recogniser.annotate(f);

                if (name.size() > 0) {
                    final Point2d r = f.getBounds().getTopLeft();
                    frame.drawText(name.get(0).annotation, r, HersheyFont.ROMAN_SIMPLEX, 15, RGBColour.GREEN);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new VideoFaceRecognition();
    }
}
