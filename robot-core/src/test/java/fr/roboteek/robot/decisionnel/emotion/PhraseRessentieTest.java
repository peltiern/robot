package fr.roboteek.robot.decisionnel.emotion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La lecture de l'émotion que l'IA écrit en tête de sa réponse. */
class PhraseRessentieTest {

    @Test
    void lEtiquetteDonneEmotionEtIntensiteEtLaPhraseResteSeule() {
        assertEquals(new PhraseRessentie("Je m'appelle Wall-E !", Emotion.JOIE, 0.7),
                PhraseRessentie.lire("[joie 0.7] Je m'appelle Wall-E !"));
    }

    /** L'IA n'est pas toujours rigoureuse : virgule, accent, espaces, majuscules. */
    @Test
    void uneEtiquetteApproximativeSeLitQuandMeme() {
        assertEquals(new PhraseRessentie("Grr !", Emotion.COLERE, 0.4), PhraseRessentie.lire("[ Colère , 0,4 ] Grr !"));
        assertEquals(new PhraseRessentie("Tiens donc.", Emotion.CURIOSITE, 0.5), PhraseRessentie.lire("[curiosité] Tiens donc."));
        assertEquals(1.0, PhraseRessentie.lire("[peur 3] Au secours !").intensite());
    }

    /** Le robot ne doit jamais se taire à cause d'une émotion : la phrase est dite, en neutre. */
    @Test
    void sansEtiquetteLaPhraseEntiereEstDiteEnNeutre() {
        assertEquals(new PhraseRessentie("Bonjour !", Emotion.NEUTRE, 0), PhraseRessentie.lire("Bonjour !"));
        assertEquals(new PhraseRessentie("Hmm.", Emotion.NEUTRE, 0), PhraseRessentie.lire("[nostalgie 0.9] Hmm."));
        assertEquals(new PhraseRessentie("", Emotion.NEUTRE, 0), PhraseRessentie.lire(null));
    }

    /** Des crochets au milieu de la phrase ne sont pas une étiquette. */
    @Test
    void seulsLesCrochetsDeTeteComptent() {
        assertEquals(new PhraseRessentie("Je dis [joie] au milieu.", Emotion.NEUTRE, 0),
                PhraseRessentie.lire("Je dis [joie] au milieu."));
    }

    @Test
    void lAffichageRetireLEtiquette() {
        assertEquals("Salut !", PhraseRessentie.sansEtiquette("[amusement 0.3] Salut !"));
        assertEquals("Salut !", PhraseRessentie.sansEtiquette("Salut !"));
    }

    /** La consigne donne à l'IA la liste exacte, tirée de l'enum. */
    @Test
    void laConsigneCiteToutesLesEmotions() {
        String consigne = PhraseRessentie.consigne();
        for (Emotion e : Emotion.values()) {
            assertTrue(consigne.contains(e.cle()), e.cle());
        }
    }
}
