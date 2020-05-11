package fr.roboteek.robot.memoire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fr.roboteek.robot.Constantes;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ReconnaissanceFacialePython {

    private String PYTHON_CMD = "/usr/bin/python3.6";
    private String FACE_RECOGNITION_PYTHON_SERVER_FILE = Constantes.DOSSIER_RECONNAISSANCE_FACIALE + File.separator + "python-server" + File.separator + "face_recognition_server.py";
    private String KNOWN_FACES_FOLDER = Constantes.DOSSIER_RECONNAISSANCE_FACIALE + File.separator + "known-faces";
    private String IMAGE_TEMP_FOLDER = Constantes.DOSSIER_RECONNAISSANCE_FACIALE + File.separator + "tmp-img";
    private String FACE_RECOGNITION_ENDPOINT_URL = "http://localhost:5001/face-recognition";
    private String FACE_DETECTION_ENDPOINT_URL = "http://localhost:5001/face-detection";

    /**
     * Classe GSON permettant la création des objets JSON.
     */
    private Gson gson;

    public ReconnaissanceFacialePython() {

        gson = new GsonBuilder().create();

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

    public FacialRecognitionResponse recognizeFaces(MBFImage image) {
        return processImage(image, FACE_RECOGNITION_ENDPOINT_URL);
    }

    public FacialRecognitionResponse detectFaces(MBFImage image) {
        return processImage(image, FACE_DETECTION_ENDPOINT_URL);
    }

    public void apprendreVisagesPersonne(List<FImage> listeVisages, String prenomPersonne) {

    }

    private FacialRecognitionResponse processImage(MBFImage image, String endpointUrl) {
        long timestamp = System.currentTimeMillis();
        File file = new File(IMAGE_TEMP_FOLDER + File.separator + "face_" + timestamp + ".jpg");
        try {
            ImageUtilities.write(image, file);
            FacialRecognitionResponse response = gson.fromJson(Unirest.post(endpointUrl)
                    .field("file", file)
                    .asString().getBody(), FacialRecognitionResponse.class);
            asyncDeleteFile(file);
            return response;
        } catch (UnirestException | IOException e) {
            e.printStackTrace();
            asyncDeleteFile(file);
        }
        return null;
    }

    private void asyncDeleteFile(final File file) {
        final Thread thread = new Thread("DeleteFileThread") {
            @Override
            public void run() {
                if (file.exists()) {
                    file.delete();
                }
            }
        };
        thread.start();
    }

    public static void main(String[] args) throws InterruptedException {
        ReconnaissanceFacialePython rf = new ReconnaissanceFacialePython();
        Thread.sleep(10000);
        rf.recognizeFaces(null);
    }
}
