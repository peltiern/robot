package fr.roboteek.robot.activites.retrouvailles;

import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.activites.conversation.ConversationIA;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private MemoireCourtTerme memoireCourtTerme;
    private ConversationIA conversationIA;
    private RetrouvaillesActivity activite;

    private final List<String> phrasesDites = new ArrayList<>();

    @BeforeEach
    void setUp() {
        memoireCourtTerme = mock(MemoireCourtTerme.class);
        conversationIA = mock(ConversationIA.class);
        activite = new RetrouvaillesActivity(memoireCourtTerme, conversationIA);

        ApplicationEventPublisher publieur = evenement -> {
            if (evenement instanceof ParoleEvent parole) {
                phrasesDites.add(parole.getTexte());
            }
        };
        ReflectionTestUtils.setField(activite, "applicationEventPublisher", publieur);
        activite.activer();
    }

    /**
     * Ce que fait le cerveau : on confie les retrouvailles, puis il les remet à l'activité au
     * moment où il la lance — et pas avant.
     */
    private void confierEtLancer(Personne personne, long secondesDAbsence) {
        activite.confier(personne, secondesDAbsence);
        activite.preparer(new DemandeActiviteEvent(
                RetrouvaillesActivity.class.getSimpleName(), personne.id()));
    }

    /**
     * Le défaut qui a motivé la table par personne : deux venues se confirment coup sur coup, et
     * le cerveau ne lance que celle qu'il a acceptée. Avec un champ unique, la seconde écrasait la
     * première et le robot saluait Paul sous le nom de Marie — indiscernable d'une erreur de
     * reconnaissance.
     */
    @Test
    void deuxVenuesCoupSurCoupNeSeMarchentPasDessus() {
        Personne paul = new Personne("id-paul", "Paul", null);
        when(conversationIA.saluerRetrouvailles(MARIE, 3600, false)).thenReturn("Salut Marie !");
        when(conversationIA.saluerRetrouvailles(paul, 3600, false)).thenReturn("Salut Paul !");

        // Les deux sont confiées ; c'est celle de Marie que le cerveau a acceptée.
        activite.confier(MARIE, 3600);
        activite.confier(paul, 3600);
        activite.preparer(new DemandeActiviteEvent(
                RetrouvaillesActivity.class.getSimpleName(), MARIE.id()));

        activite.run();

        assertEquals(List.of("Salut Marie !"), phrasesDites);
    }

    /** Et ce qu'on n'a pas joué se reprend, pour que la venue ne soit pas perdue. */
    @Test
    void cequiNaPasEteJoueSeReprend() {
        activite.confier(MARIE, 3600);

        assertEquals(MARIE, activite.reprendre(MARIE.id()));
        assertNull(activite.reprendre(MARIE.id()), "repris deux fois");
    }

    @Test
    void laPhraseComposeeParLIaEstDite() {
        when(conversationIA.saluerRetrouvailles(MARIE, 3600, false)).thenReturn("Salut Marie ! Alors, ce chat ?");
        confierEtLancer(MARIE, 3600);

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
        confierEtLancer(MARIE, 3600);

        activite.run();

        verify(conversationIA).saluerRetrouvailles(MARIE, 3600, false);
    }

    /** Une absence de quelques minutes se reprend, elle ne se salue pas. */
    @Test
    void uneCourteAbsenceDemandeUneReprise() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn("On en était où ?");
        confierEtLancer(MARIE, 120);

        activite.run();

        verify(conversationIA).saluerRetrouvailles(MARIE, 120, true);
    }

    /**
     * Une absence de quelques secondes n'en est pas une : la personne s'est tournée, la
     * conversation n'a jamais été interrompue. Le robot ne dit rien du tout.
     */
    @Test
    void uneAbsenceDeQuelquesSecondesNeFaitRienDire() {
        confierEtLancer(MARIE, 40);

        activite.run();

        assertTrue(phrasesDites.isEmpty(), "phrases dites : " + phrasesDites);
        verifyNoInteractions(conversationIA);
        // L'interlocuteur est tout de même posé : la conversation qui continue doit savoir à qui.
        verify(memoireCourtTerme).poserInterlocuteur(MARIE);
    }

    /** Quelqu'un jamais rencontré (-1) est salué, pas repris : il n'y a rien à reprendre. */
    @Test
    void uneDerniereRencontreInconnueDemandeUneSalutation() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn("Salut Marie !");
        confierEtLancer(MARIE, -1);

        activite.run();

        verify(conversationIA).saluerRetrouvailles(MARIE, -1, false);
    }

    @Test
    void lInterlocuteurEstPoseAvantDeParler() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn("Coucou Marie !");
        confierEtLancer(MARIE, 1000);

        activite.run();

        verify(memoireCourtTerme).poserInterlocuteur(MARIE);
    }

    @Test
    void unePhraseDeSecoursEstDiteSiLIaNeRepondPas() {
        when(conversationIA.saluerRetrouvailles(any(Personne.class), anyLong(), anyBoolean())).thenReturn(null);
        confierEtLancer(MARIE, 1000);

        activite.run();

        assertEquals(1, phrasesDites.size());
        assertTrue(phrasesDites.getFirst().contains("Marie"), "phrase dite : " + phrasesDites);
    }

    /** Demande sans cible, ou cible qu'on ne nous a jamais confiée : on ne dit rien. */
    @Test
    void sansPersonneRienNEstDit() {
        activite.preparer(new DemandeActiviteEvent(
                RetrouvaillesActivity.class.getSimpleName(), "id-inconnu"));

        activite.run();

        assertTrue(phrasesDites.isEmpty());
        verifyNoInteractions(conversationIA);
        verifyNoInteractions(memoireCourtTerme);
    }
}
