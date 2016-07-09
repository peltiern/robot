package fr.roboteek.ai.engine.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Entité représentant une unité d'intelligence artificielle.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class AIUnity {

    /** Entrée. */
    @SerializedName("i")
    private Input input;
    
    /** Liste de réponses. */
    @SerializedName("lr")
    private List<Response> listeResponses;

    public Input getInput() {
        return input;
    }

    public void setInput(Input input) {
        this.input = input;
    }

    public List<Response> getListeResponses() {
        if (listeResponses == null) {
            listeResponses = new ArrayList<Response>();
        }
        return listeResponses;
    }

    public void setListeResponses(List<Response> listeResponses) {
        this.listeResponses = listeResponses;
    }

    @Override
    public String toString() {
        return "AIUnity [input=" + input + ", listeResponses=" + listeResponses + "]";
    }

}
