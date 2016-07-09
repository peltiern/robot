package fr.roboteek.ai.engine.util;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class utilitaire pour les expressions régulières.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public final class RegexUtils {

    /**
     * Récupère la liste des noms des variables globales dans une expression régulière.
     * <br>De la forme ${nomVariable}
     * @param regex l'expression régulière à analyser
     * @return la liste des noms des variables globales
     */
    public static Set<String> getNomsVariablesGlobales(String regex) {
        Set<String> nomsVariablesGlobales = new TreeSet<String>();

        if (regex != null) {
            Matcher m = Pattern.compile("\\(\\$\\{([a-zA-Z][a-zA-Z0-9]*)\\}").matcher(regex);

            while (m.find()) {
                nomsVariablesGlobales.add(m.group(1));
            }
        }

        return nomsVariablesGlobales;
    }

    /**
     * Récupère la liste des noms des variables locales dans une expression régulière (inclut les variables globales).
     * <br>De la forme {nomVariable}
     * @param regex l'expression régulière à analyser
     * @return la liste des noms des variables locales
     */
    public static Set<String> getNomsVariablesLocales(String regex) {
        Set<String> nomsVariablesLocales = new TreeSet<String>();

        if (regex != null) {
            Matcher m = Pattern.compile("[\\$]{0,1}\\{([a-zA-Z][a-zA-Z0-9]*)\\}").matcher(regex);

            while (m.find()) {
                nomsVariablesLocales.add(m.group(1));
            }
        }

        return nomsVariablesLocales;
    }

}
