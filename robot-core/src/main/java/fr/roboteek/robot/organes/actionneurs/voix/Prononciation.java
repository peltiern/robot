package fr.roboteek.robot.organes.actionneurs.voix;

import java.util.regex.Pattern;

/**
 * Ce que Piper doit lire autrement qu'il n'est écrit.
 * <p>
 * Le texte reste « Wall-E » partout — dialogue affiché, journaux, conversation — ; seule la phrase
 * passée à Piper change, au dernier moment. Nicolas écrivait déjà « Wally » dans la phrase d'essai de
 * la page Voix pour que le nom sonne juste.
 */
public final class Prononciation {

    /** « Wall-E », « WALL-E », « Wall E », « Walle » : un mot entier, pas le début de « wallon ». */
    private static final Pattern WALL_E = Pattern.compile("\\bwall[- ]?e\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private Prononciation() {
    }

    public static String pourPiper(String texte) {
        return texte == null ? null : WALL_E.matcher(texte).replaceAll("Wally");
    }
}
