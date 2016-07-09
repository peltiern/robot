package fr.roboteek.ai.engine.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Liste d'unités d'intelligence artificielle
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class AIUnityList {

    /** Liste des unités. */
    @SerializedName("lu")
    private List<AIUnity> listeUnities;
    
    public List<AIUnity> getListeUnities() {
        if (listeUnities == null) {
            listeUnities = new ArrayList<AIUnity>();
        }
        return listeUnities;
    }

    public void setListeUnities(List<AIUnity> listeUnities) {
        this.listeUnities = listeUnities;
    }
    
    public void addAiUnityList(AIUnityList aiUnityList) {
        if (aiUnityList != null) {
            getListeUnities().addAll(aiUnityList.getListeUnities());
        }
    }

    @Override
    public String toString() {
        return "AIUnityList [listeUnities=" + listeUnities + "]";
    }

}
