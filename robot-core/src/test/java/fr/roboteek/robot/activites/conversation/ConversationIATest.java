package fr.roboteek.robot.activites.conversation;

import fr.roboteek.robot.memoire.longterme.personne.Personne;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    /**
     * La clé ne tient qu'à l'identifiant.
     * <p>
     * Le prénom y a figuré, en simple étiquette, pour s'y retrouver en ouvrant la base. Il en a été
     * retiré le 2026-08-16 : renommer quelqu'un changeait sa clé, et le robot repartait d'une page
     * blanche avec lui sans que rien ne le signale — précisément au moment où l'on corrige un
     * prénom mal compris, donc où l'on tient le plus à ce qu'il se souvienne.
     */
    @Test
    void laCleNeTientQuALIdentifiant() {
        assertEquals("personne-id-marie", idConversation(new Personne("id-marie", "Marie", null)));
    }

    @Test
    void renommerQuelquunNeChangePasSonFil() {
        Personne marie = new Personne("id-marie", "Marie", null);

        assertEquals(idConversation(marie), idConversation(marie.renommee("Marion")));
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
