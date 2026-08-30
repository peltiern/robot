package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.animation.Avertissements;
import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.LecteurAnimation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.web.controller.dto.AnimationEnCours;
import fr.roboteek.robot.web.controller.dto.DemandeLecture;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Ressource REST « animation en cours » : ce que le robot est en train de jouer.
 * <p>
 * Une ressource et non des verbes. {@code POST /api/animations/{nom}/jouer} et
 * {@code POST /api/animations/stopper} étaient des appels de procédure déguisés en URL ; ici le
 * robot n'a qu'<b>une</b> animation en cours, et les trois méthodes disent tout : la lire, la
 * remplacer, la retirer.
 * <p>
 * Ressource de premier niveau, et non {@code /api/animations/en-cours} : sous {@code /api/animations/}
 * le segment suivant est le <b>nom</b> d'une animation, et un mot réservé y rendrait inaccessible
 * l'animation qui porterait ce nom.
 * <p>
 * Le {@code GET} existe pour la question qu'un client se pose en arrivant — la page d'édition
 * ouverte pendant qu'une animation tourne doit le montrer, pas afficher un robot au repos.
 */
@RestController
@RequestMapping("/api/animation-en-cours")
@CrossOrigin(origins = "*")
public class AnimationEnCoursController {

    private final BibliothequeDesAnimations bibliotheque;

    private final LecteurAnimation lecteur;

    public AnimationEnCoursController(BibliothequeDesAnimations bibliotheque, LecteurAnimation lecteur) {
        this.bibliotheque = bibliotheque;
        this.lecteur = lecteur;
    }

    /** L'animation en cours, ou 204 si le robot ne joue rien. */
    @GetMapping
    public ResponseEntity<AnimationEnCours> enCours() {
        return lecteur.animationEnCours()
                .map(animation -> ResponseEntity.ok(new AnimationEnCours(animation.nom(), Avertissements.de(animation))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Lance une animation, en remplaçant celle qui se jouait.
     * <p>
     * {@code PUT} et non {@code POST} : le robot n'a qu'une animation en cours, et la relancer
     * deux fois de suite doit donner le même état — pas deux lectures superposées.
     */
    @PutMapping
    public ResponseEntity<AnimationEnCours> lancer(@RequestBody DemandeLecture demande) {
        Optional<Animation> animation = aJouer(demande);
        if (animation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!lecteur.jouer(animation.get())) {
            // 409 et non 500 : le robot va bien, il refuse — arrêt d'urgence armé, ou organe pas
            // encore démarré. Un 500 enverrait chercher une panne qui n'existe pas.
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok(new AnimationEnCours(animation.get().nom(), Avertissements.de(animation.get())));
    }

    /** Interrompt l'animation en cours. Sans effet s'il n'y en a pas : le résultat est le même. */
    @DeleteMapping
    public ResponseEntity<Void> arreter() {
        lecteur.stopper();
        return ResponseEntity.noContent().build();
    }

    /**
     * Une animation nommée est cherchée dans la bibliothèque ; une animation fournie est jouée
     * telle quelle, sans être enregistrée — c'est ce dont l'éditeur a besoin pour essayer un
     * brouillon.
     */
    private Optional<Animation> aJouer(DemandeLecture demande) {
        if (demande.animation() != null) {
            return Optional.of(demande.animation());
        }
        return demande.nom() == null ? Optional.empty() : bibliotheque.charger(demande.nom());
    }
}
