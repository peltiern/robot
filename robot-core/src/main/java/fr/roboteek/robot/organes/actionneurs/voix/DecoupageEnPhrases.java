package fr.roboteek.robot.organes.actionneurs.voix;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Une réponse coupée en phrases, pour que le robot commence à parler avant d'avoir tout synthétisé.
 * <p>
 * Mesuré sur le robot le 2026-09-27 : Piper met 2,5 à 4 s à synthétiser une réponse de 50 à 60
 * caractères, mais va plus vite que la parole (3,9 s de calcul pour 5 s de voix). Dire la première
 * phrase pendant qu'il prépare la suivante ramène l'attente à la synthèse d'une seule phrase.
 */
public final class DecoupageEnPhrases {

    /** Après un signe de fin de phrase suivi d'un blanc — « 3.5 » ou « Wall-E! » collé ne coupent pas. */
    private static final Pattern FIN_DE_PHRASE = Pattern.compile("(?<=[.!?…])\\s+");

    /**
     * Un morceau plus court est joint au suivant. Chaque morceau relance {@code play}, soit environ
     * 0,3 s de pause : un « Ça ? » seul la paierait pour presque rien.
     */
    static final int LONGUEUR_MINIMALE = 15;

    private DecoupageEnPhrases() {
    }

    public static List<String> decouper(String texte) {
        List<String> morceaux = new ArrayList<>();
        if (texte == null || texte.isBlank()) {
            return morceaux;
        }
        StringBuilder enCours = new StringBuilder();
        for (String phrase : FIN_DE_PHRASE.split(texte.trim())) {
            if (!enCours.isEmpty()) {
                enCours.append(' ');
            }
            enCours.append(phrase.trim());
            if (enCours.length() >= LONGUEUR_MINIMALE) {
                morceaux.add(enCours.toString());
                enCours.setLength(0);
            }
        }
        if (!enCours.isEmpty()) {
            // Un reste trop court rejoint le morceau précédent plutôt que de sonner seul.
            if (morceaux.isEmpty()) {
                morceaux.add(enCours.toString());
            } else {
                morceaux.set(morceaux.size() - 1, morceaux.getLast() + " " + enCours);
            }
        }
        return morceaux;
    }
}
