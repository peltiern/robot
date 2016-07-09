package fr.roboteek.robot.systemenerveux.event;

import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.image.processing.face.detection.DetectedFace;

/**
 * Evènement de détection de visages.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class VisagesEvent extends RobotEvent {
    
    /** Image d'origine. */
    private FImage imageOrigine;
    

    /** Liste des visages détectés. */
    private List<DetectedFace> listeVisages;
    
    /**
     * Récupère la valeur de imageOrigine.
     * @return la valeur de imageOrigine
     */
    public FImage getImageOrigine() {
        return imageOrigine;
    }

    /**
     * Définit la valeur de imageOrigine.
     * @param imageOrigine la nouvelle valeur de imageOrigine
     */
    public void setImageOrigine(FImage imageOrigine) {
        this.imageOrigine = imageOrigine;
    }

    public List<DetectedFace> getListeVisages() {
        return listeVisages;
    }

    public void setListeVisages(List<DetectedFace> listeVisages) {
        this.listeVisages = listeVisages;
    }

}
