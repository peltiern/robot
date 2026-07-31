package fr.roboteek.robot.spring.server.controller;

import fr.roboteek.robot.securite.ArretUrgence;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ressource REST « arrêt d'urgence » : uniquement l'<b>état courant</b>.
 * <p>
 * Le déclenchement et le réarmement passent par le Websocket, comme tous les autres ordres
 * ({@code ArretUrgenceEvent} sur {@code /app/robotevents}). Cette ressource ne sert qu'à répondre
 * à la question qu'un client se pose en arrivant : « est-ce que le robot est déjà en arrêt
 * d'urgence ? ». Sans elle, une interface ouverte après coup afficherait un robot normal, alors
 * qu'il refuse tout mouvement — le plus sûr moyen de croire à une panne.
 * <p>
 * Ensuite, l'état se suit tout seul sur {@code /events/arret-urgence}.
 */
@RestController
@RequestMapping("/api/arret-urgence")
@CrossOrigin(origins = "*")
public class ArretUrgenceController {

    private final ArretUrgence arretUrgence;

    public ArretUrgenceController(ArretUrgence arretUrgence) {
        this.arretUrgence = arretUrgence;
    }

    @GetMapping
    public Map<String, Boolean> etat() {
        return Map.of("actif", arretUrgence.estActif());
    }
}
