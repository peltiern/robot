package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.LecteurAnimation;
import fr.roboteek.robot.organes.actionneurs.animation.LimitesMoteur;
import fr.roboteek.robot.organes.actionneurs.animation.VerificateurAnimation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.web.controller.dto.Lecture;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Ressource REST « animations » : consulter la bibliothèque et faire jouer une animation.
 * <p>
 * Volontairement réduite à la lecture et à la commande. L'enregistrement, la suppression et le
 * scrub viendront avec l'éditeur, qui est le seul à en avoir besoin ; les ajouter d'avance
 * demanderait de figer un contrat que rien n'exerce encore.
 * <p>
 * Ce qui a motivé cette ressource : sans elle, le lecteur d'animation n'était déclenchable par
 * <b>rien</b> — ni bus, ni HTTP — et ne pouvait donc pas être essayé sur le robot. Une animation
 * écrite à la main dans {@code $ROBOT_HOME/animations} et un {@code curl} suffisent maintenant à
 * juger de la chose : la courbe, la cadence, et si le mouvement est beau.
 */
@RestController
@RequestMapping("/api/animations")
@CrossOrigin(origins = "*")
public class AnimationController {

    private final BibliothequeDesAnimations bibliotheque;

    private final LecteurAnimation lecteur;

    public AnimationController(BibliothequeDesAnimations bibliotheque, LecteurAnimation lecteur) {
        this.bibliotheque = bibliotheque;
        this.lecteur = lecteur;
    }

    /** Les noms des animations disponibles. */
    @GetMapping
    public List<String> noms() {
        return bibliotheque.noms();
    }

    /** Une animation entière, telle qu'elle est sur le disque. */
    @GetMapping("/{nom}")
    public ResponseEntity<Animation> animation(@PathVariable String nom) {
        return bibliotheque.charger(nom)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Lance une animation.
     * <p>
     * La réponse porte les avertissements du vérificateur — position hors butée, transition trop
     * rapide pour le servo. Ils n'empêchent pas de jouer : c'est délibéré, une animation trop
     * ambitieuse s'écrête d'elle-même et le voir bouger en dit plus qu'un refus. Mais ils
     * expliquent l'écart entre ce que l'éditeur montre et ce que le robot fait, et sans eux on
     * chercherait la panne ailleurs.
     */
    @PostMapping("/{nom}/jouer")
    public ResponseEntity<Lecture> jouer(@PathVariable String nom) {
        Optional<Animation> animation = bibliotheque.charger(nom);
        if (animation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<String> avertissements = new VerificateurAnimation(
                LimitesMoteur.parAxe(Configurations.phidgetsConfig()))
                .controler(animation.get())
                .stream()
                .map(avertissement -> "%s (%d à %d ms) : %s".formatted(
                        avertissement.axe().libelle(), avertissement.instantDebut(),
                        avertissement.instantFin(), avertissement.message()))
                .toList();

        if (!lecteur.jouer(animation.get())) {
            // 409 et non 500 : le robot va bien, il refuse — arrêt d'urgence armé, ou organe pas
            // encore démarré. Un 500 enverrait chercher une panne qui n'existe pas.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Lecture(nom, false, avertissements));
        }
        return ResponseEntity.ok(new Lecture(nom, true, avertissements));
    }

    /** Interrompt l'animation en cours. Sans effet s'il n'y en a pas. */
    @PostMapping("/stopper")
    public ResponseEntity<Void> stopper() {
        lecteur.stopper();
        return ResponseEntity.noContent().build();
    }
}
