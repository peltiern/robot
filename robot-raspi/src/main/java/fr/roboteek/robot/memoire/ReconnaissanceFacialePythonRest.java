package fr.roboteek.robot.memoire;

import fr.roboteek.robot.Constantes;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.io.File;
import java.io.IOException;

public class ReconnaissanceFacialePythonRest {

    private String PYTHON_CMD = "/usr/bin/python3.8";
    private String FACE_RECOGNITION_PYTHON_SERVER_FILE = Constantes.DOSSIER_RECONNAISSANCE_FACIALE + File.separator + "python-server" + File.separator + "robot_ai_api.py";
    private String KNOWN_FACES_FOLDER = Constantes.DOSSIER_RECONNAISSANCE_FACIALE + File.separator + "known-faces";
    private String IMAGE_TEMP_FOLDER = Constantes.DOSSIER_RECONNAISSANCE_FACIALE + File.separator + "tmp-img";
    private String FACE_RECOGNITION_ENDPOINT_URL = "http://localhost:5001/face-recognition";
    private String FACE_DETECTION_ENDPOINT_URL = "http://localhost:5001/face-detection";
    private String OBJECT_DETECTION_ENDPOINT_URL = "http://localhost:5001/object-detection";

    public ReconnaissanceFacialePythonRest() {

        final Thread threadServer = new Thread("FacialRecognitionServer") {
            @Override
            public void run() {
                // Lancement du serveur Python de reconnaissance faciale
                String[] params = {PYTHON_CMD, FACE_RECOGNITION_PYTHON_SERVER_FILE, KNOWN_FACES_FOLDER};
                try {
                    Process p = Runtime.getRuntime().exec(params);
                    p.waitFor();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (true) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        threadServer.start();
    }

//    public FacialRecognitionResponse recognizeFaces(MBFImage image) {
//        return processFacialImage(image, FACE_RECOGNITION_ENDPOINT_URL, FacialRecognitionResponse.class);
//    }
//
//    public FacialRecognitionResponse detectFaces(MBFImage image) {
//        return processFacialImage(image, FACE_DETECTION_ENDPOINT_URL, FacialRecognitionResponse.class);
//    }
//
//    public void apprendreVisagesPersonne(List<FImage> listeVisages, String prenomPersonne) {
//
//    }
//
//    public ObjectDetectionResponse detectObjects(MBFImage image) {
//        return processFacialImage(image, OBJECT_DETECTION_ENDPOINT_URL, ObjectDetectionResponse.class);
//    }
//
//    private <T> T processFacialImage(MBFImage image, String endpointUrl, Class<T> responseClass) {
//        long timestamp = System.currentTimeMillis();
//        File file = new File(IMAGE_TEMP_FOLDER + File.separator + "face_" + timestamp + ".jpg");
//        try {
//            ImageUtilities.write(image, file);
//            T response = Unirest.post(endpointUrl)
//                    .field("file", file)
//                    .asObject(responseClass).getBody();
//            asyncDeleteFile(file);
//            return response;
//        } catch (UnirestException | IOException e) {
//            e.printStackTrace();
//            asyncDeleteFile(file);
//        }
//        return null;
//    }
//
//    private void asyncDeleteFile(final File file) {
//        final Thread thread = new Thread("DeleteFileThread") {
//            @Override
//            public void run() {
//                if (file.exists()) {
//                    file.delete();
//                }
//            }
//        };
//        thread.start();
//    }
//
//    public static void main(String[] args) throws InterruptedException, IOException {
//        ReconnaissanceFacialePythonRest rf = new ReconnaissanceFacialePythonRest();
//        Thread.sleep(5000);
//        long start = System.currentTimeMillis();
//        FacialRecognitionResponse response = rf.recognizeFaces(ImageUtilities.readMBF(new File("/home/npeltier/Robot/Programme/reconnaissance-visage/known-faces/penny.jpg")));
//        long stop = System.currentTimeMillis();
//        System.out.println(" Temps = " +(stop - start) + ", response = " + response);
//        start = System.currentTimeMillis();
//        response = rf.recognizeFaces(ImageUtilities.readMBF(new File("/home/npeltier/Robot/Programme/reconnaissance-visage/known-faces/penny.jpg")));
//        stop = System.currentTimeMillis();
//        System.out.println(" Temps = " +(stop - start) + ", response = " + response);
//        start = System.currentTimeMillis();
//        response = rf.recognizeFaces(ImageUtilities.readMBF(new File("/home/npeltier/Robot/Programme/reconnaissance-visage/known-faces/penny.jpg")));
//        stop = System.currentTimeMillis();
//        System.out.println(" Temps = " +(stop - start) + ", response = " + response);
//        start = System.currentTimeMillis();
//        response = rf.recognizeFaces(ImageUtilities.readMBF(new File("/home/npeltier/Robot/Programme/reconnaissance-visage/known-faces/penny.jpg")));
//        stop = System.currentTimeMillis();
//        System.out.println(" Temps = " +(stop - start) + ", response = " + response);
//    }
}
