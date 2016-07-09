package fr.roboteek.ai.engine.model;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.roboteek.ai.engine.util.RegexUtils;

/**
 * Objet regroupants les éléments nécessaires à un pattern.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class AIPattern {

    /** L'expression régulière. */
    private String regex;

    /** Le pattern compilé. */
    private Pattern pattern;

    /** La liste des noms des variables globales utilisées dans l'expression régulière. */
    private Set<String> listeNomsVariablesGlobales;
    
    /** La liste des noms des variables locales utilisées dans l'expression régulière. */
    private Set<String> listeNomsVariablesLocales;

    /**
     * Constructeur.
     * @param regex expression régulière.
     */
    public AIPattern(String regex) {
        this.regex = regex;
    }

    /**
     * Compile le pattern.
     */
    public void compile() {
        if (pattern == null) {
            
            String regexModifiee = regex;
            
            // Récupération des noms variables globales
            listeNomsVariablesLocales = RegexUtils.getNomsVariablesLocales(regex);
            listeNomsVariablesGlobales = RegexUtils.getNomsVariablesGlobales(regex);
            
            // Remplacement du nom des variables par la regex appropriée
            if (listeNomsVariablesLocales != null && !listeNomsVariablesLocales.isEmpty()) {
                for (String nomVariable : listeNomsVariablesLocales) {
                    // Remplacement des variables globales
                    regexModifiee = regexModifiee.replace("${"+ nomVariable + "}", "?<" + nomVariable + ">");
                 // Remplacement des variables locales
                    regexModifiee = regexModifiee.replace("{"+ nomVariable + "}", "?<" + nomVariable + ">");
                }
            }

            // Compilation du pattern
            pattern = Pattern.compile(regexModifiee, Pattern.CASE_INSENSITIVE);
        }
    }

    /**
     * Match d'un texte sur le pattern.
     * @param texte le texte à analyser
     * @return le matcher correspondant
     */
    public Matcher matcher(String texte) {
        // Compilation du pattern si ce n'est pas fait
        if (pattern == null) {
            compile();
        }
        return pattern.matcher(texte);
    }

    public Set<String> getListeNomsVariablesGlobales() {
        return listeNomsVariablesGlobales;
    }

    public Set<String> getListeNomsVariablesLocales() {
        return listeNomsVariablesLocales;
    }
}
