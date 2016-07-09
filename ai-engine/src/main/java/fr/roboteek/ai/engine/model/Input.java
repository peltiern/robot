package fr.roboteek.ai.engine.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.annotations.SerializedName;

/**
 * Entrée du moteur.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Input {

    public static enum TYPE {PAROLE, ACTION, VISAGE};

    /** Type. */
    @SerializedName("t")
    private TYPE type;

    /** Valeur. */
    @SerializedName("v")
    private String value;

    /** Pattern calculé. */
    private AIPattern pattern;

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AIPattern compilePattern(Map<String, String> mapSets) {
        if (pattern == null) {
            String stringPattern = value;
            // Remplacement des sets par l'ensemble de leurs éléments
            for (Entry<String, String> set : mapSets.entrySet()) {
                stringPattern = stringPattern.replace("[" + set.getKey() + "]", set.getValue());
            }
            
            stringPattern = stringPattern.replace(".", "\\.");

            // Compilation du pattern
            pattern = new AIPattern(stringPattern);
            pattern.compile();
        }
        return pattern;
    }

    @Override
    public String toString() {
        return "Input [type=" + type + ", value=" + value + "]";
    }

    public AIPattern getPattern() {
        return pattern;
    }

}
