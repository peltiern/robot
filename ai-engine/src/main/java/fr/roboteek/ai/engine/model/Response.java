package fr.roboteek.ai.engine.model;

import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import fr.roboteek.ai.engine.util.RegexUtils;

/**
 * Réponse.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Response {

    /** Type. */
    @SerializedName("t")
    private String type;

    /** Valeur. */
    @SerializedName("v")
    private String value;

    /** Flag indiquant que la réponse a été compilée. */
    private boolean compile = false;

    /** La liste des noms des variables utilisées dans la réponse. */
    private Set<String> listeNomsVariables;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Response [type=" + type + ", value=" + value + "]";
    }

    private void compile() {
        if (!compile) {
            // Récupération du noms des variables utilisées dans la réponse
            listeNomsVariables = RegexUtils.getNomsVariablesLocales(value);
            compile = true;
        }
    }

    /**
     * Construit la réponse en fonction des variables.
     * @param mapVariablesLocales la map des variables locales
     * @param mapVariablesGlobales la map des variables globales
     * @return la réponse
     */
    public String answer(Map<String, String> mapVariablesLocales, Map<String, String> mapVariablesGlobales) {
        String reponse = "PROBLEME";
        if (value != null) {
            // Compîlation
            compile();
            reponse = value;
            // Remplacement de l'ensemble des variables de la réponse
            if (listeNomsVariables != null && !listeNomsVariables.isEmpty()) {
                for (String nomVariable : listeNomsVariables) {
                    // Recherche de la valeur dans les variables locales
                    String valeurVariable = mapVariablesLocales.get(nomVariable);
                    // Si non trouvée, recherche de la valeur dans les variables globales
                    if (valeurVariable == null) {
                        valeurVariable = mapVariablesGlobales.get(nomVariable);
                    }
                    // Remplacement de la variable par sa valeur si trouvée
                    if (valeurVariable != null) {
                        reponse = reponse.replace("{" + nomVariable + "}", valeurVariable);
                    } else {
                        // TODO PROBLEME = ERREUR
                    }
                }
            }
        }
        return reponse;
    }

}
