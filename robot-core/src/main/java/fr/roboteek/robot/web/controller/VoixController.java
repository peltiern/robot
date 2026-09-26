package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.actionneurs.OrganeParole;
import fr.roboteek.robot.organes.actionneurs.voix.CatalogueDesModeles;
import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;
import fr.roboteek.robot.organes.actionneurs.voix.Voix;
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

    private final CatalogueDesModeles catalogue;

    private final OrganeParole organeParole;

    private final ApplicationEventPublisher publieur;

    public VoixController(VoixDuRobot voix, CatalogueDesModeles catalogue, OrganeParole organeParole,
                          ApplicationEventPublisher publieur) {
        this.voix = voix;
        this.catalogue = catalogue;
        this.organeParole = organeParole;
        this.publieur = publieur;
    }

    @GetMapping
    public EtatVoix etat() {
        return new EtatVoix(voix.voix(), ReglagesVoix.ORIGINE, organeParole.voixReglable(),
                catalogue.modeles(), Configurations.piperSpeechSynthesisConfig().voiceModelFileName());
    }

    /** Adopte une voix. Répond la voix gardée, dont les réglages ont pu être bornés. */
    @PutMapping
    public ResponseEntity<Voix> adopter(@RequestBody Voix nouvelle) {
        if (!acceptable(nouvelle)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(voix.adopter(nouvelle));
    }

    /** 202 : la phrase est demandée ; le robot l'ignore s'il parle déjà. */
    @PostMapping("/essai")
    public ResponseEntity<Void> essayer(@RequestBody EssaiDeVoix essai) {
        if (essai == null || essai.texte() == null || essai.texte().isBlank() || !acceptable(essai.voix())) {
            return ResponseEntity.badRequest().build();
        }
        publieur.publishEvent(new EssaiDeVoixEvent(essai.texte(), essai.voix().bornee()));
        return ResponseEntity.accepted().build();
    }

    /**
     * Des réglages, et un modèle déposé sur le robot — ou aucun, pour celui par défaut. Le nom du
     * modèle finit dans un chemin de fichier : seul un nom de la liste passe.
     */
    private boolean acceptable(Voix demandee) {
        return demandee != null && demandee.reglages() != null
                && (demandee.modele() == null || catalogue.contient(demandee.modele(), demandee.locuteur()));
    }
}
