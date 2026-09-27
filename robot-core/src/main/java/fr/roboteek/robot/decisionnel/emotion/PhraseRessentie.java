package fr.roboteek.robot.decisionnel.emotion;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Une réponse de l'IA : la phrase que le robot dit, et ce que la phrase de son interlocuteur lui a
 * fait ressentir.
 * <p>
 * L'IA écrit l'émotion en tête de sa réponse, entre crochets : {@code [joie 0.7] Je m'appelle
 * Wall-E !}. Une étiquette plutôt qu'une réponse JSON : la réponse est gardée telle quelle dans le
 * fil de conversation de la personne, que l'onglet « QUI ? » affiche — une étiquette s'y lit, et se
 * retire d'un geste ; du JSON rendrait tout le fil illisible. Et l'IA, qui relit ses propres
 * réponses dans l'historique, y retrouve le format à tenir.
 *
 * @param texte     la phrase à dire, sans l'étiquette
 * @param emotion   ce que le robot ressent ; {@link Emotion#NEUTRE} si l'IA n'a rien dit d'utilisable
 * @param intensite de 0 à 1
 */
public record PhraseRessentie(String texte, Emotion emotion, double intensite) {

    /** {@code [joie 0.7]}, {@code [joie]}, {@code [ joie , 0,7 ]} : l'IA n'est pas toujours rigoureuse. */
    private static final Pattern ETIQUETTE = Pattern.compile("^\\s*\\[\\s*([\\p{L}]+)\\s*[, ]?\\s*([0-9]+(?:[.,][0-9]+)?)?\\s*]\\s*");

    /** Intensité supposée quand l'IA donne une émotion sans dire à quel point. */
    private static final double INTENSITE_PAR_DEFAUT = 0.5;

    /**
     * La consigne à ajouter au prompt système : la liste vient de {@link Emotion}, elle ne peut donc
     * pas diverger de ce que le robot sait exprimer.
     */
    public static String consigne() {
        String cles = Arrays.stream(Emotion.values()).map(Emotion::cle).collect(Collectors.joining(", "));
        return "Commence toujours ta réponse par l'émotion que la phrase de ton interlocuteur te fait ressentir, "
                + "et son intensité de 0 à 1, entre crochets, par exemple [joie 0.7] ou [neutre 0]. "
                + "Émotions possibles, écrites exactement ainsi : " + cles + ". "
                + "Réserve les intensités fortes à ce qui te touche vraiment. "
                + "Après les crochets, la phrase que tu dis, sans rien d'autre.";
    }

    /**
     * Lit une réponse de l'IA. Ne rend jamais {@code null} ni une phrase vide à partir d'un texte
     * qui en contenait une : une étiquette absente, inconnue ou mal écrite laisse la phrase
     * entière, dite en {@link Emotion#NEUTRE}. Le robot ne doit pas se taire pour une émotion.
     */
    public static PhraseRessentie lire(String reponse) {
        if (reponse == null) {
            return new PhraseRessentie("", Emotion.NEUTRE, 0);
        }
        Matcher m = ETIQUETTE.matcher(reponse);
        if (!m.find()) {
            return new PhraseRessentie(reponse.trim(), Emotion.NEUTRE, 0);
        }
        String texte = reponse.substring(m.end()).trim();
        Emotion emotion = Emotion.depuisCle(sansAccent(m.group(1).toLowerCase()));
        if (emotion == null) {
            return new PhraseRessentie(texte, Emotion.NEUTRE, 0);
        }
        double intensite = m.group(2) == null ? INTENSITE_PAR_DEFAUT
                : Math.clamp(Double.parseDouble(m.group(2).replace(',', '.')), 0, 1);
        return new PhraseRessentie(texte, emotion, intensite);
    }

    /** La phrase seule, pour l'afficher : l'onglet « QUI ? » n'a que faire de l'étiquette. */
    public static String sansEtiquette(String reponse) {
        return reponse == null ? null : ETIQUETTE.matcher(reponse).replaceFirst("");
    }

    /** L'IA écrit parfois « colère » pour « colere » : la clé, elle, n'a pas d'accent. */
    private static String sansAccent(String mot) {
        return Normalizer.normalize(mot, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
