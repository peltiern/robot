package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.SoundPlayer;
import fr.roboteek.robot.organes.actionneurs.son.BibliothequeDesSons;
import fr.roboteek.robot.web.controller.dto.SonEnCours;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Ressource REST « son en cours » : ce que le robot fait entendre, là, maintenant.
 * <p>
 * Construite comme {@link AnimationEnCoursController}, et pour les mêmes raisons : une ressource
 * plutôt que des verbes, de premier niveau parce que sous {@code /api/sons/} le segment suivant est
 * le <b>nom</b> d'un son, et un {@code PUT} parce que le robot n'a qu'une bouche — demander deux
 * fois le même son doit laisser le même état, pas deux lectures superposées.
 * <p>
 * Le Studio écoute d'abord dans le navigateur ; ce chemin-ci sert à entendre le son <b>là où il
 * sera joué</b>, sur le haut-parleur du robot, qui ne sonne pas comme un poste de travail.
 */
@RestController
@RequestMapping("/api/son-en-cours")
@CrossOrigin(origins = "*")
public class SonEnCoursController {

    private final BibliothequeDesSons bibliotheque;

    private final SoundPlayer lecteur;

    public SonEnCoursController(BibliothequeDesSons bibliotheque, SoundPlayer lecteur) {
        this.bibliotheque = bibliotheque;
        this.lecteur = lecteur;
    }

    /** Le son en cours, ou 204 si le robot se tait. */
    @GetMapping
    public ResponseEntity<SonEnCours> enCours() {
        return lecteur.sonEnCours()
                .map(nom -> ResponseEntity.ok(new SonEnCours(nom)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Joue un son de la bibliothèque, en coupant celui qui se jouait. */
    @PutMapping
    public ResponseEntity<SonEnCours> jouer(@RequestBody SonEnCours demande) {
        Optional<Path> fichier = bibliotheque.audio(demande.nom());
        if (fichier.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!lecteur.jouer(demande.nom(), fichier.get())) {
            // 409 et non 500 : le robot va bien, il ne peut pas — organe pas encore démarré, ou
            // `play` absent de l'image. Un 500 enverrait chercher une panne qui n'existe pas.
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok(new SonEnCours(demande.nom()));
    }

    /** Coupe le son en cours. Sans effet si le robot se taisait déjà. */
    @DeleteMapping
    public ResponseEntity<Void> couper() {
        lecteur.arreterLecture();
        return ResponseEntity.noContent().build();
    }
}
