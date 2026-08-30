package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.animation.Avertissements;
import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Ressource REST « animations » : la bibliothèque, et rien d'autre.
 * <p>
 * <b>Aucun verbe dans les chemins.</b> Ce qui se jouait par {@code POST /{nom}/jouer} est devenu
 * une ressource à part entière, {@code /api/animation-en-cours} — voir
 * {@link AnimationEnCoursController}. Et rien d'autre ne s'ajoutera sous {@code /api/animations/} :
 * le segment suivant est le <b>nom</b> d'une animation, donc y placer un mot réservé rendrait
 * inaccessible l'animation qui porterait ce nom-là.
 * <p>
 * Les écritures renvoient les avertissements du vérificateur : une animation dont une transition
 * dépasse ce que le servo sait faire est enregistrée quand même — c'est un brouillon légitime —
 * mais l'éditeur doit pouvoir le dire.
 */
@RestController
@RequestMapping("/api/animations")
@CrossOrigin(origins = "*")
public class AnimationController {

    private final BibliothequeDesAnimations bibliotheque;

    public AnimationController(BibliothequeDesAnimations bibliotheque) {
        this.bibliotheque = bibliotheque;
    }

    /** Les noms des animations enregistrées. */
    @GetMapping
    public List<String> noms() {
        return bibliotheque.noms();
    }

    @GetMapping("/{nom}")
    public ResponseEntity<Animation> animation(@PathVariable String nom) {
        return bibliotheque.charger(nom)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Crée une animation. Refuse d'écraser : pour cela, {@link #remplacer} est explicite. */
    @PostMapping
    public ResponseEntity<List<String>> creer(@RequestBody Animation animation) {
        if (animation.nom() == null || bibliotheque.existe(animation.nom())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        bibliotheque.enregistrer(animation);
        return ResponseEntity.created(URI.create("/api/animations/" + animation.nom()))
                .body(Avertissements.de(animation));
    }

    /**
     * Crée ou remplace l'animation portant ce nom. <b>C'est le nom de l'URL qui fait foi</b>, pas
     * celui du corps : c'est ce qui permet d'enregistrer sous un autre nom sans que l'éditeur ait
     * à se souvenir de modifier son brouillon, et ça reste vrai après un renommage de fichier.
     */
    @PutMapping("/{nom}")
    public ResponseEntity<List<String>> remplacer(@PathVariable String nom, @RequestBody Animation animation) {
        Animation aEnregistrer = conserverLesSons(animation.avecNom(nom));
        bibliotheque.enregistrer(aEnregistrer);
        return ResponseEntity.ok(Avertissements.de(aEnregistrer));
    }

    @DeleteMapping("/{nom}")
    public ResponseEntity<Void> supprimer(@PathVariable String nom) {
        return bibliotheque.supprimer(nom)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Garde la piste son de la version enregistrée quand celle qui arrive n'en porte aucune.
     * <p>
     * L'éditeur ne connaît pas encore les sons : il relit une animation, la renvoie, et les
     * effacerait sans que personne ne l'ait demandé. Le jour où il saura les éditer, il enverra
     * une liste — fût-elle vide après une suppression volontaire — et cette précaution deviendra
     * inutile sans devenir gênante.
     */
    private Animation conserverLesSons(Animation animation) {
        if (!animation.sons().isEmpty()) {
            return animation;
        }
        return bibliotheque.charger(animation.nom())
                .filter(existante -> !existante.sons().isEmpty())
                .map(existante -> new Animation(animation.nom(), animation.dureeTotale(),
                        animation.pistes(), existante.sons()))
                .orElse(animation);
    }

}
