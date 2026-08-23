package fr.roboteek.robot.web.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.DemandeActiviteEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce que l'interface envoie au robot sur {@code /app/robotevents} doit continuer d'arriver.
 * <p>
 * Chemin sans le moindre test jusqu'ici, et pourtant le plus fragile : Gson écrit les champs par
 * réflexion, si bien qu'un renommage, un champ rendu final ou un constructeur vide supprimé ne
 * casse <b>rien à la compilation</b> — l'évènement arrive simplement vide, et le robot ne fait
 * rien sans qu'aucune erreur ne le dise.
 * <p>
 * Le Gson est monté exactement comme celui de {@link WebsocketController} : c'est le contrat réel
 * avec la tablette, pas une approximation.
 */
class EvenementsEntrantsTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(RobotEvent.class, new RobotEventAdapter())
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (json, type, context) -> (json == null || json.isJsonNull())
                            ? null
                            : LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    /** Le seul évènement que le HUD envoie aujourd'hui — et il coupe les moteurs. */
    @Test
    void leDeclenchementDArretUrgenceArriveComplet() {
        RobotEvent recu = gson.fromJson(
                "{\"eventType\":\"arret-urgence\",\"actif\":true,\"origine\":\"interface\"}",
                RobotEvent.class);

        ArretUrgenceEvent arret = assertInstanceOf(ArretUrgenceEvent.class, recu);
        assertTrue(arret.isActif(), "l'arrêt d'urgence arriverait désarmé");
        assertEquals("interface", arret.getOrigine());
        assertEquals(ArretUrgenceEvent.EVENT_TYPE, arret.getEventType());
    }

    @Test
    void leRearmementArriveAussi() {
        RobotEvent recu = gson.fromJson(
                "{\"eventType\":\"arret-urgence\",\"actif\":false,\"origine\":\"interface\"}",
                RobotEvent.class);

        assertFalseActif(assertInstanceOf(ArretUrgenceEvent.class, recu));
    }

    @Test
    void uneDemandeDActiviteArriveAvecSonIdentifiant() {
        RobotEvent recu = gson.fromJson(
                "{\"eventType\":\"demande-activite\",\"idActivite\":\"ConversationActivity\"}",
                RobotEvent.class);

        DemandeActiviteEvent demande = assertInstanceOf(DemandeActiviteEvent.class, recu);
        assertEquals("ConversationActivity", demande.getIdActivite());
    }

    /** Les autres portes ouvertes par l'adaptateur, que l'ancienne interface emprunte encore. */
    @Test
    void lesEvenementsDeLAncienneInterfaceArriventAussi() {
        ParoleEvent parole = assertInstanceOf(ParoleEvent.class,
                gson.fromJson("{\"eventType\":\"parole\",\"texte\":\"bonjour\"}", RobotEvent.class));
        assertEquals("bonjour", parole.getTexte());

        MouvementCouEvent cou = assertInstanceOf(MouvementCouEvent.class,
                gson.fromJson("{\"eventType\":\"mouvement-cou\",\"anglePanoramique\":12.5}", RobotEvent.class));
        assertEquals(12.5, cou.getAnglePanoramique());
    }

    /** L'horodatage du robot doit se relire : c'est lui qui date les bulles du fil. */
    @Test
    void lHorodatageSeRelit() {
        RobotEvent recu = gson.fromJson(
                "{\"eventType\":\"arret-urgence\",\"actif\":true,\"dateTime\":\"2026-08-23T10:15:30\"}",
                RobotEvent.class);

        assertNotNull(recu.getDateTime());
        assertEquals(2026, recu.getDateTime().getYear());
    }

    /**
     * L'interface n'envoie pas de {@code dateTime} : c'est le constructeur vide qui l'horodate à
     * l'arrivée. Sans lui, Gson alloue l'objet sans passer par aucun constructeur et l'évènement
     * entre dans le robot sans date — sans que rien ne compile en rouge.
     */
    @Test
    void unEvenementSansDateEstHorodateALArrivee() {
        RobotEvent recu = gson.fromJson(
                "{\"eventType\":\"arret-urgence\",\"actif\":true}", RobotEvent.class);

        assertNotNull(recu.getDateTime(), "évènement entré sans date : le constructeur vide a sauté");
    }

    /** Un type inconnu ne doit pas faire tomber la session websocket. */
    @Test
    void unEvenementInconnuNeCassePas() {
        assertNull(gson.fromJson("{\"eventType\":\"jamais-vu\"}", RobotEvent.class));
    }

    private static void assertFalseActif(ArretUrgenceEvent arret) {
        assertEquals(false, arret.isActif());
        assertEquals("interface", arret.getOrigine());
    }
}
