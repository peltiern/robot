package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.securite.ArretUrgence;
import fr.roboteek.robot.systemenerveux.event.TrajetRectiligneEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les chenilles commandées en distance : « avance de 90 cm ».
 * <p>
 * L'ordre passe par le bus : c'est la conduite qui suit les encodeurs et décide de l'arrêt, et tout
 * ordre de la manette l'interrompt.
 */
@RestController
@RequestMapping("/api/conduite")
@CrossOrigin(origins = "*")
public class ConduiteController {

    /** Au-delà, mieux vaut un robot qui sait où sont les murs : il n'a pas encore de capteur pour ça. */
    static final double DISTANCE_MAX_CM = 300;

    private final ArretUrgence arretUrgence;

    private final ApplicationEventPublisher publieur;

    public ConduiteController(ArretUrgence arretUrgence, ApplicationEventPublisher publieur) {
        this.arretUrgence = arretUrgence;
        this.publieur = publieur;
    }

    /** Distance en centimètres, négative pour reculer. */
    public record Trajet(double cm) {
    }

    /** 202 : le trajet est lancé ; 409 en arrêt d'urgence, où la conduite refuserait tout mouvement. */
    @PostMapping("/trajet")
    public ResponseEntity<Void> avancer(@RequestBody Trajet trajet) {
        if (trajet == null || !Double.isFinite(trajet.cm()) || trajet.cm() == 0
                || Math.abs(trajet.cm()) > DISTANCE_MAX_CM) {
            return ResponseEntity.badRequest().build();
        }
        if (arretUrgence.estActif()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        publieur.publishEvent(new TrajetRectiligneEvent(trajet.cm() * 10));
        return ResponseEntity.accepted().build();
    }
}
