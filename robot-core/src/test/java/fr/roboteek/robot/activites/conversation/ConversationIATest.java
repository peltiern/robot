package fr.roboteek.robot.activites.conversation;

import fr.roboteek.robot.memoire.personne.Personne;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la clé du fil de mémoire, sans client d'IA ni réseau : c'est elle qui décide de qui se
 * souvient de quoi, et une erreur y ferait raconter à quelqu'un ce qu'un autre a confié.
 */
class ConversationIATest {

    @Test
    void deuxPersonnesDuMemePrenomOntDesFilsDistincts() {
        Personne premier = new Personne("id-1", "Nicolas", null);
        Personne second = new Personne("id-2", "Nicolas", null);

        assertNotEquals(idConversation(premier), idConversation(second),
                "c'est l'identifiant qui distingue, pas le prénom");
    }

    @Test
    void leFilResteLeMemeDUneRencontreALAutre() {
        Personne marie = new Personne("id-marie", "Marie", null);
        // La même personne, revue plus tard : sa date de rencontre a changé, pas son fil.
        Personne marieRevue = marie.rencontreeLe(java.time.LocalDateTime.now());

        assertEquals(idConversation(marie), idConversation(marieRevue));
    }

    /** Le prénom est dans la clé pour qu'un humain s'y retrouve en ouvrant la base. */
    @Test
    void lePrenomFigureDansLaCle() {
        String cle = idConversation(new Personne("id-marie", "Marie", null));

        assertTrue(cle.startsWith("personne-marie-"), "clé obtenue : " + cle);
        assertTrue(cle.endsWith("id-marie"), "clé obtenue : " + cle);
    }

    @Test
    void unPrenomAccentueOuEspaceDonneUneCleLisible() {
        String cle = idConversation(new Personne("id-1", "Jean-Éric ", null));

        assertEquals("personne-jeaneric-id-1", cle);
    }

    @Test
    void sansInterlocuteurLeFilParDefautEstConserve() {
        // Valeur historique : toute la mémoire écrite avant l'identification des personnes est là.
        assertEquals("wall-e", idConversation(null));
    }

    private static String idConversation(Personne personne) {
        return ReflectionTestUtils.invokeMethod(ConversationIA.class, "idConversation", personne);
    }
}
