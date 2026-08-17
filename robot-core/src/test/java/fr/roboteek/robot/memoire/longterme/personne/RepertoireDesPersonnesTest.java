package fr.roboteek.robot.memoire.longterme.personne;

import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.memoire.longterme.BaseMemoireDeTest;
import fr.roboteek.robot.memoire.longterme.conversation.ConversationRepository;
import fr.roboteek.robot.memoire.longterme.rencontre.JournalDesRencontres;
import fr.roboteek.robot.memoire.longterme.rencontre.RencontreRepository;
import fr.roboteek.robot.memoire.longterme.visage.ApprentissageParPhoto;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Vérifie qu'une personne se manipule d'un seul geste, et surtout qu'elle s'efface <b>partout</b>.
 * <p>
 * C'est tout l'objet de ce répertoire : une personne vit dans quatre tables et dans la tête du
 * robot. Une suppression qui en oublie un morceau laisse une empreinte orpheline, et le robot
 * « reconnaît » alors quelqu'un qui n'existe plus.
 */
class RepertoireDesPersonnesTest {

    @TempDir
    File dossierTemp;

    private PersonneRepository personneRepository;

    private VisageConnuRepository visageConnuRepository;

    private ConversationRepository conversationRepository;

    private JournalDesRencontres journalDesRencontres;

    private MemoireCourtTerme memoireCourtTerme;

    private RepertoireDesPersonnes repertoire;

    private Personne marie;

    private Personne paul;

    @BeforeEach
    void setUp() {
        DataSource memoire = BaseMemoireDeTest.dans(dossierTemp);
        personneRepository = new PersonneRepository(memoire);
        visageConnuRepository = new VisageConnuRepository(memoire);
        conversationRepository = new ConversationRepository(memoire);
        journalDesRencontres = new JournalDesRencontres(new RencontreRepository(memoire));
        memoireCourtTerme = mock(MemoireCourtTerme.class);
        repertoire = new RepertoireDesPersonnes(personneRepository, visageConnuRepository,
                conversationRepository, journalDesRencontres, memoireCourtTerme,
                new ApprentissageParPhoto(visageConnuRepository));

        marie = Personne.nouvelle("Marie");
        paul = Personne.nouvelle("Paul");
        personneRepository.enregistrer(marie);
        personneRepository.enregistrer(paul);
    }

    @Test
    void laListeCompteLesVisagesEtLesRencontresSansAllerLesChercherUnParUn() {
        visageConnuRepository.ajouter(marie.id(), new float[]{1f, 2f});
        visageConnuRepository.ajouter(marie.id(), new float[]{3f, 4f});
        journalDesRencontres.inscrirePremiereRencontre(marie);

        List<RepertoireDesPersonnes.FichePersonne> fiches = repertoire.toutes();

        assertEquals(2, fiches.size());
        RepertoireDesPersonnes.FichePersonne ficheMarie = ficheDe(fiches, marie);
        assertEquals(2, ficheMarie.nombreDeVisages());
        assertEquals(1, ficheMarie.nombreDeRencontres());
        // Paul est connu mais ne sera jamais reconnu : c'est exactement ce que zéro visage veut dire.
        assertEquals(0, ficheDe(fiches, paul).nombreDeVisages());
        assertEquals(0, ficheDe(fiches, paul).nombreDeRencontres());
    }

    @Test
    void supprimerEffaceLaFicheLesVisagesLesRencontresEtLaConversation() {
        peuplerToutPour(marie);
        peuplerToutPour(paul);

        assertTrue(repertoire.supprimer(marie.id()));

        assertNull(personneRepository.parId(marie.id()));
        assertTrue(visageConnuRepository.parPersonne(marie.id()).isEmpty());
        assertTrue(journalDesRencontres.pourPersonne(marie.id()).isEmpty());
        assertTrue(repertoire.conversation(marie.id()).isEmpty());

        // Et rien de ce qui appartient à Paul n'a bougé.
        assertEquals(1, visageConnuRepository.parPersonne(paul.id()).size());
        assertEquals(1, journalDesRencontres.pourPersonne(paul.id()).size());
        assertEquals(2, repertoire.conversation(paul.id()).size());
    }

    /**
     * La base ne peut pas savoir ce que le robot a en tête : sans cet oubli, il continuerait un
     * moment de croire la personne devant lui, et de chercher en base une identité disparue.
     */
    @Test
    void supprimerSortAussiLaPersonneDeLaTeteDuRobot() {
        repertoire.supprimer(marie.id());

        verify(memoireCourtTerme).oublier(marie.id());
    }

    @Test
    void supprimerQuelquunQuiNexistePasNeToucheARien() {
        assertFalse(repertoire.supprimer("inexistant"));

        verifyNoInteractions(memoireCourtTerme);
        assertEquals(2, personneRepository.toutes().size());
    }

    /**
     * Le cas qui motive tout le reste : corriger un prénom mal compris ne doit rien coûter.
     * La clé de conversation a longtemps contenu le prénom — renommer effaçait alors la mémoire
     * que le robot avait de la personne, en silence.
     */
    @Test
    void renommerNeFaitPerdreNiVisagesNiHistoireNiConversation() {
        peuplerToutPour(marie);

        Personne renommee = repertoire.renommer(marie.id(), "Marion");

        assertEquals("Marion", renommee.prenom());
        assertEquals("Marion", personneRepository.parId(marie.id()).prenom());
        assertEquals(1, visageConnuRepository.parPersonne(marie.id()).size());
        assertEquals(1, journalDesRencontres.pourPersonne(marie.id()).size());
        assertEquals(2, repertoire.conversation(marie.id()).size());
    }

    @Test
    void renommerRefuseUnPrenomVide() {
        assertThrows(IllegalArgumentException.class, () -> repertoire.renommer(marie.id(), "   "));
    }

    @Test
    void renommerQuelquunQuiNexistePasNeRendRien() {
        assertNull(repertoire.renommer("inexistant", "Marion"));
    }

    @Test
    void oublierLaConversationLaisseLaPersonneConnueEtReconnue() {
        peuplerToutPour(marie);

        assertTrue(repertoire.oublierLaConversation(marie.id()));

        assertTrue(repertoire.conversation(marie.id()).isEmpty());
        assertEquals("Marie", personneRepository.parId(marie.id()).prenom());
        assertEquals(1, visageConnuRepository.parPersonne(marie.id()).size());
        assertEquals(1, journalDesRencontres.pourPersonne(marie.id()).size());
    }

    @Test
    void laConversationSeRelitDansLordre() {
        peuplerToutPour(marie);

        List<Message> fil = repertoire.conversation(marie.id());

        assertEquals(List.of("Bonjour", "Salut !"), fil.stream().map(Message::getText).toList());
    }

    private void peuplerToutPour(Personne personne) {
        visageConnuRepository.ajouter(personne.id(), new float[]{1f, 2f});
        journalDesRencontres.inscrirePremiereRencontre(personne);
        conversationRepository.saveAll(ConversationRepository.idConversationDe(personne.id()),
                List.of(new UserMessage("Bonjour"), new AssistantMessage("Salut !")));
    }

    private static RepertoireDesPersonnes.FichePersonne ficheDe(
            List<RepertoireDesPersonnes.FichePersonne> fiches, Personne personne) {
        return fiches.stream()
                .filter(fiche -> fiche.personne().id().equals(personne.id()))
                .findFirst()
                .orElseThrow();
    }
}
