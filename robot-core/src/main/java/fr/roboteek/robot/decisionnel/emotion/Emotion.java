package fr.roboteek.robot.decisionnel.emotion;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Ce que le robot peut ressentir, et donc exprimer : le langage commun aux animations, aux sons et
 * à la conversation.
 * <p>
 * Les huit premières sont les humeurs du Studio son, sous les mêmes clés : un son et une animation
 * de « joie » se reconnaissent sans table de correspondance. Les autres ont été choisies avec
 * Nicolas le 2026-09-27 pour un robot comme Wall-E : le rire distinct de la joie, la gêne, le
 * « wooow », l'incompréhension, le dégoût. {@link #NEUTRE} dit explicitement « pas de réaction ».
 * <p>
 * La liste reste courte exprès : plus elle s'allonge, moins l'IA choisit avec constance, et plus il
 * faut d'animations pour la couvrir.
 * <p>
 * C'est la seule définition de la liste : l'appli la reçoit par {@code /api/emotions} et en garde
 * une copie dans le navigateur, pour l'Atelier robot éteint.
 */
public enum Emotion {

    NEUTRE("neutre", "Neutre", "😐"),
    JOIE("joie", "Joie", "😄"),
    TRISTESSE("tristesse", "Tristesse", "😢"),
    COLERE("colere", "Colère", "😠"),
    PEUR("peur", "Peur", "😨"),
    SURPRISE("surprise", "Surprise", "😮"),
    CURIOSITE("curiosite", "Curiosité", "🤔"),
    TENDRESSE("tendresse", "Tendresse", "🥰"),
    FATIGUE("fatigue", "Fatigue", "😴"),
    DEGOUT("degout", "Dégoût", "🤢"),
    AMUSEMENT("amusement", "Amusement", "😆"),
    TIMIDITE("timidite", "Timidité", "😳"),
    EMERVEILLEMENT("emerveillement", "Émerveillement", "🤩"),
    CONFUSION("confusion", "Confusion", "😕");

    private final String cle;
    private final String libelle;
    private final String emoji;

    Emotion(String cle, String libelle, String emoji) {
        this.cle = cle;
        this.libelle = libelle;
        this.emoji = emoji;
    }

    /** Ce qui s'écrit dans les fichiers et circule avec l'appli : sans accent, comme dans le Studio. */
    @JsonValue
    public String cle() {
        return cle;
    }

    public String libelle() {
        return libelle;
    }

    public String emoji() {
        return emoji;
    }

    /**
     * L'émotion d'une clé, ou {@code null} si elle n'en est pas une.
     * <p>
     * {@code null} plutôt qu'une erreur : une animation dont l'émotion aurait été retirée de la liste
     * doit rester lisible, simplement sans émotion — la refuser la ferait disparaître de la
     * bibliothèque.
     */
    @JsonCreator
    public static Emotion depuisCle(String cle) {
        return Arrays.stream(values()).filter(e -> e.cle.equals(cle)).findFirst().orElse(null);
    }
}
