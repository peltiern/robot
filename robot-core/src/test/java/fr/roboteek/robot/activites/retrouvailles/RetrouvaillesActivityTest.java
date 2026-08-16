package fr.roboteek.robot.activites.retrouvailles;

import fr.roboteek.robot.activites.conversation.ConversationActivity;
import fr.roboteek.robot.activites.conversation.ConversationIA;
import fr.roboteek.robot.memoire.personne.Personne;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Vérifie ce que le robot dit en retrouvant quelqu'un, et surtout qu'il dise <b>toujours</b>
 * quelque chose : l'IA peut ne pas répondre, un bonjour ne doit pas en dépendre.
 */
class RetrouvaillesActivityTest {

    private static final Personne MARIE = new Personne("id-marie", "Marie", null);

    private ConversationActivity conversationActivity;
    private ConversationIA conversationIA;
    private RetrouvaillesActivity activite;

    private final List<String> phrasesDites = new ArrayList<>();

    @BeforeEach
    void setUp() {
        conversationActivity = mock(ConversationActivity.class);
        conversationIA = mock(ConversationIA.class);
        activite = new RetrouvaillesActivity(conversationActivity, conversationIA);

        ApplicationEventPublisher publieur = evenement -> {
            if (evenement instanceof ParoleEvent parole) {
                phrasesDites.add(parole.getTexte());
            }
        };
        ReflectionTestUtils.setField(activite, "applicationEventPublisher", publieur);
        activite.activer();
    }

    @Test
    void laPhraseComposeeParLIaEstDite() {
        when(conversationIA.saluerRetrouvailles(MARIE, 3600, false)).thenReturn("Salut Marie ! Alors, ce chat ?");
        activite.setPersonneRetrouvee(MARIE, 3600);

        activite.run();

        assertEquals(List.of("Salut Marie ! Alors, ce chat ?"), phrasesDites);
    }

    /**
     * Une longue absence se salue. Le seuil par défaut est de 300 s (aucun {@code robot.properties}
     * n'est lisible en test, Owner retombe sur les {@code @DefaultValue}).
     */
    @Test
    void uneLongueAbsenceDemandeUneSalutation() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn("Salut Marie !");
        activite.setPersonneRetrouvee(MARIE, 3600);

        activite.run();

        verify(conversationIA).saluerRetrouvailles(MARIE, 3600, false);
    }

    /** Une absence de quelques minutes se reprend, elle ne se salue pas. */
    @Test
    void uneCourteAbsenceDemandeUneReprise() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn("On en était où ?");
        activite.setPersonneRetrouvee(MARIE, 120);

        activite.run();

        verify(conversationIA).saluerRetrouvailles(MARIE, 120, true);
    }

    /**
     * Une absence de quelques secondes n'en est pas une : la personne s'est tournée, la
     * conversation n'a jamais été interrompue. Le robot ne dit rien du tout.
     */
    @Test
    void uneAbsenceDeQuelquesSecondesNeFaitRienDire() {
        activite.setPersonneRetrouvee(MARIE, 40);

        activite.run();

        assertTrue(phrasesDites.isEmpty(), "phrases dites : " + phrasesDites);
        verifyNoInteractions(conversationIA);
        // L'interlocuteur est tout de même posé : la conversation qui continue doit savoir à qui.
        verify(conversationActivity).setInterlocuteur(MARIE);
    }

    /** Quelqu'un jamais rencontré (-1) est salué, pas repris : il n'y a rien à reprendre. */
    @Test
    void uneDerniereRencontreInconnueDemandeUneSalutation() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn("Salut Marie !");
        activite.setPersonneRetrouvee(MARIE, -1);

        activite.run();

        verify(conversationIA).saluerRetrouvailles(MARIE, -1, false);
    }

    @Test
    void lInterlocuteurEstPoseAvantDeParler() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn("Coucou Marie !");
        activite.setPersonneRetrouvee(MARIE, 1000);

        activite.run();

        verify(conversationActivity).setInterlocuteur(MARIE);
    }

    @Test
    void unePhraseDeSecoursEstDiteSiLIaNeRepondPas() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn(null);
        activite.setPersonneRetrouvee(MARIE, 1000);

        activite.run();

        assertEquals(1, phrasesDites.size());
        assertTrue(phrasesDites.getFirst().contains("Marie"), "phrase dite : " + phrasesDites);
    }

    @Test
    void sansPersonneRienNEstDit() {
        activite.setPersonneRetrouvee(null, -1);

        activite.run();

        assertTrue(phrasesDites.isEmpty());
        verifyNoInteractions(conversationIA);
        verifyNoInteractions(conversationActivity);
    }
}
