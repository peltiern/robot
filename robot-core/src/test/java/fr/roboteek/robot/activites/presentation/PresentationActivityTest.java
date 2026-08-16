package fr.roboteek.robot.activites.presentation;

import fr.roboteek.robot.activites.conversation.ConversationActivity;
import fr.roboteek.robot.memoire.personne.Personne;
import fr.roboteek.robot.memoire.personne.PersonneRepository;
import fr.roboteek.robot.systemenerveux.event.DemandeEnrolementEvent;
import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleTermineeEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.VisagePercu;
import fr.roboteek.robot.systemenerveux.event.VisagePercuEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Vérifie le déroulé de la rencontre : ce que le robot dit, ce qu'il enregistre, et surtout ce
 * qu'il <b>n'</b>enregistre <b>pas</b>.
 * <p>
 * Le robot est ici entouré d'un faux monde : la parole répond aussitôt qu'elle est terminée (comme
 * le fait {@code OrganeParole}), et le test glisse les réponses vocales au moment voulu. Sans quoi
 * l'activité attendrait ses délais réels — huit secondes par question.
 */
class PresentationActivityTest {

    private static final Personne NICOLAS = new Personne("id-nicolas", "Nicolas", null);

    @TempDir
    File dossierTemp;

    private PersonneRepository personneRepository;
    private ConversationActivity conversationActivity;
    private ExtractionPrenomIA extractionPrenomIA;
    private PresentationActivity activite;

    /** Tout ce que le robot a dit, dans l'ordre. */
    private final List<String> phrasesDites = new CopyOnWriteArrayList<>();

    /** Réponses vocales à servir dès que le robot a fini de dire une phrase contenant la clé. */
    private final Map<String, String> reponsesProgrammees = new ConcurrentHashMap<>();

    /** Réussite de l'enrôlement simulé. */
    private boolean enrolementReussi = true;

    private final List<String> enrolementsDemandes = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        personneRepository = new PersonneRepository(new File(dossierTemp, "personnes.db").getAbsolutePath());
        conversationActivity = mock(ConversationActivity.class);
        extractionPrenomIA = mock(ExtractionPrenomIA.class);
        // 200 ms au lieu de 8 s : les cas sans réponse attendraient sinon une demi-minute.
        activite = new PresentationActivity(conversationActivity, extractionPrenomIA, personneRepository, 200);

        ApplicationEventPublisher publieur = evenement -> {
            if (evenement instanceof ParoleEvent parole) {
                phrasesDites.add(parole.getTexte());
                // Le monde réel : la parole signale sa fin, puis la personne répond.
                activite.handleParoleTermineeEvent(new ParoleTermineeEvent(parole.getTexte()));
                repondreSiProgramme(parole.getTexte());
            } else if (evenement instanceof DemandeEnrolementEvent demande) {
                enrolementsDemandes.add(demande.getIdPersonne());
                activite.handleEnrolementTermineEvent(
                        new EnrolementTermineEvent(demande.getIdPersonne(), enrolementReussi ? 5 : 0, enrolementReussi));
            }
        };
        ReflectionTestUtils.setField(activite, "applicationEventPublisher", publieur);
        activite.activer();
    }

    @AfterEach
    void tearDown() {
        personneRepository.close();
    }

    @Test
    @Timeout(10)
    void unePersonneQuiSePresenteEtConfirmeEstEnregistree() {
        repondreApres("comment tu t'appelles", "je m'appelle marie");
        repondreApres("quel est ton prénom", "je m'appelle marie");
        repondreApres("c'est bien ça", "oui");
        when(extractionPrenomIA.extraire("je m'appelle marie")).thenReturn("Marie");

        activite.run();

        Personne enregistree = personneEnregistree();
        assertNotNull(enregistree, "la personne doit être en mémoire");
        assertEquals("Marie", enregistree.prenom());
        assertNotNull(enregistree.derniereRencontre(), "la rencontre doit être datée");
        assertEquals(List.of(enregistree.id()), enrolementsDemandes, "le visage appris est bien le sien");
        verify(conversationActivity).setInterlocuteur(enregistree);
        assertTrue(phrasesDites.stream().anyMatch(phrase -> phrase.contains("Enchanté Marie")));
    }

    @Test
    @Timeout(10)
    void leVisageEstAppelAvantQueLaPersonneNeSoitEnregistree() {
        // Un visage non appris ne doit laisser aucune trace : le robot se souviendrait d'un
        // prénom qu'il ne saurait jamais rattacher à personne.
        enrolementReussi = false;
        repondreApres("comment tu t'appelles", "moi c'est paul");
        repondreApres("quel est ton prénom", "moi c'est paul");
        repondreApres("c'est bien ça", "oui");
        when(extractionPrenomIA.extraire("moi c'est paul")).thenReturn("Paul");

        activite.run();

        assertNull(personneEnregistree(), "rien ne doit être enregistré");
        verify(conversationActivity, org.mockito.Mockito.never()).setInterlocuteur(any());
        assertTrue(phrasesDites.stream().anyMatch(phrase -> phrase.contains("pas réussi à bien te regarder")));
    }

    @Test
    @Timeout(10)
    void sansReponseLeRobotRelanceDeuxFoisPuisRenonce() {
        // Aucune réponse programmée : chaque question tombe dans le vide.
        activite.run();

        assertNull(personneEnregistree());
        assertTrue(enrolementsDemandes.isEmpty(), "rien à apprendre sans prénom");
        assertEquals(3, phrasesDites.size() - 1, "une question puis deux relances");
        assertTrue(phrasesDites.getLast().contains("une autre fois"));
    }

    @Test
    @Timeout(10)
    void unPrenomNonConfirmeNEstJamaisEnregistre() {
        repondreApres("comment tu t'appelles", "je m'appelle marie");
        repondreApres("quel est ton prénom", "je m'appelle marie");
        repondreApres("répéter ton prénom", "je m'appelle marie");
        repondreApres("ton prénom, c'est quoi", "je m'appelle marie");
        repondreApres("je n'ai pas bien entendu", "je m'appelle marie");
        repondreApres("c'est bien ça", "non pas du tout");
        when(extractionPrenomIA.extraire("je m'appelle marie")).thenReturn("Marie");

        activite.run();

        assertNull(personneEnregistree(), "on n'enregistre que sur accord explicite");
        assertTrue(enrolementsDemandes.isEmpty());
        assertTrue(phrasesDites.getLast().contains("une autre fois"));
    }

    @Test
    @Timeout(10)
    void uneReponseSansPrenomFaitRelancer() {
        repondreApres("comment tu t'appelles", "il fait beau aujourd'hui");
        repondreApres("quel est ton prénom", "il fait beau aujourd'hui");
        when(extractionPrenomIA.extraire("il fait beau aujourd'hui")).thenReturn(null);

        activite.run();

        assertNull(personneEnregistree());
        assertTrue(phrasesDites.getLast().contains("une autre fois"));
    }

    @Test
    @Timeout(10)
    void uneActiviteArreteeNEnregistreRien() {
        activite.stop();

        activite.run();

        assertNull(personneEnregistree());
        verifyNoInteractions(extractionPrenomIA);
    }

    /**
     * Le rattrapage : la personne finit par être reconnue pendant qu'on lui demande son prénom.
     * Le robot doit s'excuser, et surtout ne rien apprendre ni enregistrer.
     */
    @Test
    @Timeout(10)
    void unePersonneReconnueEnCoursDeRouteRecoitDesExcuses() {
        personneRepository.enregistrer(NICOLAS);
        activite.handleVisagePercuEvent(visagePercu(NICOLAS.id(), NICOLAS.prenom()));

        activite.run();

        assertTrue(phrasesDites.stream().anyMatch(phrase -> phrase.contains("Excuse-moi Nicolas")),
                "le robot doit reconnaître son erreur, phrases dites : " + phrasesDites);
        assertEquals(List.of(NICOLAS), List.copyOf(personneRepository.toutes()),
                "aucune personne ne doit être créée : celle-là était déjà connue");
        assertTrue(enrolementsDemandes.isEmpty(), "aucun visage à apprendre");
        // La personne relue en base : c'est elle, et non le visage perçu, qui désigne le fil.
        verify(conversationActivity).setInterlocuteur(NICOLAS);
    }

    /** Un visage connu qui n'est pas celui qu'on aborde — plus petit — ne doit rien déclencher. */
    @Test
    @Timeout(10)
    void unVisageConnuPlusPetitQueLInterlocuteurNInterromptRien() {
        VisagePercuEvent evenement = new VisagePercuEvent();
        evenement.setLargeurImage(640);
        evenement.setHauteurImage(480);
        evenement.setVisages(List.of(
                new VisagePercu("id-einstein", "Einstein", 0, 0, 40, 40),
                new VisagePercu(null, null, 200, 200, 200, 200)));

        activite.handleVisagePercuEvent(evenement);
        activite.run();

        assertFalse(phrasesDites.stream().anyMatch(phrase -> phrase.contains("Excuse-moi")),
                "phrases dites : " + phrasesDites);
    }

    private static VisagePercuEvent visagePercu(String idPersonne, String prenom) {
        VisagePercuEvent evenement = new VisagePercuEvent();
        evenement.setLargeurImage(640);
        evenement.setHauteurImage(480);
        evenement.setVisages(List.of(new VisagePercu(idPersonne, prenom, 200, 150, 120, 120)));
        return evenement;
    }

    /** Programme une réponse vocale, servie dès qu'une phrase dite contient {@code cle}. */
    private void repondreApres(String cle, String reponse) {
        reponsesProgrammees.put(cle, reponse);
    }

    private void repondreSiProgramme(String phraseDite) {
        String phrase = phraseDite.toLowerCase();
        reponsesProgrammees.entrySet().stream()
                .filter(entree -> phrase.contains(entree.getKey().toLowerCase()))
                .findFirst()
                .ifPresent(entree -> {
                    ReconnaissanceVocaleEvent evenement = new ReconnaissanceVocaleEvent();
                    evenement.setProcessedByBrain(true);
                    evenement.setTexteReconnu(entree.getValue());
                    activite.handleReconnaissanceVocaleEvent(evenement);
                });
    }

    private Personne personneEnregistree() {
        List<Personne> personnes = new ArrayList<>(personneRepository.toutes());
        assertFalse(personnes.size() > 1, "un seul enregistrement attendu");
        return personnes.isEmpty() ? null : personnes.getFirst();
    }
}
