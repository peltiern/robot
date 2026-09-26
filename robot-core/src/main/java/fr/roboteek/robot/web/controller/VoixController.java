package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.OrganeParole;
import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;
import fr.roboteek.robot.organes.actionneurs.voix.VoixDuRobot;
import fr.roboteek.robot.systemenerveux.event.EssaiDeVoixEvent;
import fr.roboteek.robot.web.controller.dto.EssaiDeVoix;
import fr.roboteek.robot.web.controller.dto.EtatVoix;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La voix du robot : la lire, l'adopter, et l'essayer sur le robot avant de l'adopter.
 * <p>
 * L'essai passe par le bus plutôt que par un appel direct : dire une phrase prend plusieurs
 * secondes, et c'est l'organe qui sait mettre l'écoute en pause le temps de parler.
 */
@RestController
@RequestMapping("/api/voix")
@CrossOrigin(origins = "*")
public class VoixController {

    private final VoixDuRobot voix;

    private final OrganeParole organeParole;

    private final ApplicationEventPublisher publieur;

    public VoixController(VoixDuRobot voix, OrganeParole organeParole, ApplicationEventPublisher publieur) {
        this.voix = voix;
        this.organeParole = organeParole;
        this.publieur = publieur;
    }

    @GetMapping
    public EtatVoix etat() {
        return new EtatVoix(voix.reglages(), ReglagesVoix.ORIGINE, organeParole.voixReglable());
    }

    /** Adopte une voix. Répond les réglages gardés, qui ont pu être bornés. */
    @PutMapping
    public ResponseEntity<ReglagesVoix> adopter(@RequestBody ReglagesVoix reglages) {
        if (reglages == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(voix.adopter(reglages));
    }

    /** 202 : la phrase est demandée, le robot la dira dès qu'il aura fini la précédente. */
    @PostMapping("/essai")
    public ResponseEntity<Void> essayer(@RequestBody EssaiDeVoix essai) {
        if (essai == null || essai.texte() == null || essai.texte().isBlank() || essai.reglages() == null) {
            return ResponseEntity.badRequest().build();
        }
        publieur.publishEvent(new EssaiDeVoixEvent(essai.texte(), essai.reglages().bornes()));
        return ResponseEntity.accepted().build();
    }
}
