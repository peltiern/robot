package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.son.BibliothequeDesSons;
import fr.roboteek.robot.web.controller.dto.SonEnregistre;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import tools.jackson.databind.JsonNode;

import java.util.Base64;
import java.util.List;

/**
 * Ressource REST « sons » : la bibliothèque du Studio, et rien d'autre.
 * <p>
 * Comme pour les animations, <b>aucun verbe dans les chemins</b> : ce qui joue un son sur le robot
 * est une ressource à part. Le segment qui suit {@code /api/sons/} est le <b>nom</b> d'un son, et
 * seul {@code /audio} s'ajoute après ce nom — le son lui-même, qu'un navigateur doit pouvoir
 * charger directement pour l'entendre ou le dessiner.
 */
@RestController
@RequestMapping("/api/sons")
@CrossOrigin(origins = "*")
public class SonController {

    private final BibliothequeDesSons bibliotheque;

    public SonController(BibliothequeDesSons bibliotheque) {
        this.bibliotheque = bibliotheque;
    }

    /** Les noms des sons enregistrés. */
    @GetMapping
    public List<String> noms() {
        return bibliotheque.noms();
    }

    /** La recette d'un son, pour le rouvrir dans le Studio. */
    @GetMapping("/{nom}")
    public ResponseEntity<JsonNode> recette(@PathVariable String nom) {
        return bibliotheque.recette(nom)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Le son lui-même. Servi en fichier et non en base64 : c'est ce que le navigateur sait décoder
     * pour l'écouter, et ce que la piste Son de l'Atelier dessine.
     */
    @GetMapping("/{nom}/audio")
    public ResponseEntity<Resource> audio(@PathVariable String nom) {
        return bibliotheque.audio(nom)
                .map(fichier -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/wav"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nom + ".wav\"")
                        .body((Resource) new FileSystemResource(fichier)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crée un son. <b>Refuse d'écraser</b> : pour cela, {@link #enregistrer} est explicite.
     * <p>
     * C'est ce qui protège l'« Enregistrer sous » d'un brouillon portant, sans qu'on l'ait vu, le
     * nom d'un son déjà là. Même contrat que pour les animations, pour que les deux bibliothèques
     * se manipulent pareil.
     */
    @PostMapping
    public ResponseEntity<String> creer(@RequestBody SonEnregistre son) {
        if (son.nom() == null || bibliotheque.existe(son.nom())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return enregistrer(son.nom(), son);
    }

    /**
     * Crée ou remplace le son portant ce nom, recette et audio d'un seul tenant.
     * <p>
     * <b>C'est le nom de l'URL qui fait foi</b>, comme pour les animations : enregistrer sous un
     * autre nom ne demande rien de plus à l'éditeur.
     * <p>
     * {@code PUT} et non {@code POST} : le Studio renvoie le même son à chaque retouche, et deux
     * envois de suite doivent laisser le robot dans le même état, pas créer deux sons.
     */
    @PutMapping("/{nom}")
    public ResponseEntity<String> enregistrer(@PathVariable String nom, @RequestBody SonEnregistre son) {
        byte[] wav;
        try {
            wav = Base64.getDecoder().decode(son.wav() == null ? "" : son.wav());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Audio du son « " + nom + " » illisible : base64 invalide.");
        }
        boolean existait = bibliotheque.existe(nom);
        try {
            bibliotheque.enregistrer(nom, son.recette(), wav);
        } catch (IllegalArgumentException e) {
            // 400 et non 500 : le robot va bien, c'est l'envoi qui ne convient pas — nom refusé,
            // recette sans version, audio qui n'est pas un WAV. L'éditeur doit pouvoir le dire.
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return existait
                ? ResponseEntity.noContent().build()
                : ResponseEntity.created(java.net.URI.create("/api/sons/" + nom)).build();
    }

    @DeleteMapping("/{nom}")
    public ResponseEntity<Void> supprimer(@PathVariable String nom) {
        return bibliotheque.supprimer(nom)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
