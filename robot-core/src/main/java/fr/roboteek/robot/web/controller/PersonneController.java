package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.RepertoireDesPersonnes;
import fr.roboteek.robot.memoire.longterme.rencontre.Rencontre;
import fr.roboteek.robot.memoire.longterme.visage.ApprentissageParPhoto;
import fr.roboteek.robot.web.controller.dto.FichePersonneDto;
import org.springframework.http.CacheControl;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ressource REST « personnes » : ce que le robot connaît de ceux qu'il a rencontrés, et ce qu'on
 * peut y changer depuis l'interface.
 * <p>
 * Le contrôleur ne parle qu'à {@link RepertoireDesPersonnes} et ne porte aucune règle : une
 * personne vit dans quatre tables et dans la tête du robot, et savoir combien de morceaux cela
 * fait n'est pas l'affaire d'une couche HTTP.
 * <p>
 * Pas de verbe dans les URL : {@code visages}, {@code vignette} et {@code conversation} sont des
 * sous-ressources d'une personne, et {@code POST /api/personnes} crée tout d'un coup.
 */
@RestController
@RequestMapping("/api/personnes")
@CrossOrigin(origins = "*")
public class PersonneController {

    private final RepertoireDesPersonnes repertoire;

    public PersonneController(RepertoireDesPersonnes repertoire) {
        this.repertoire = repertoire;
    }

    /** Tout le monde, sans les timelines ni les conversations : une liste n'en a pas besoin. */
    @GetMapping
    public List<FichePersonneDto> toutes() {
        return repertoire.toutes().stream()
                .map(fiche -> new FichePersonneDto(fiche.personne().id(),
                        fiche.personne().prenom(),
                        instant(fiche.personne()),
                        fiche.nombreDeVisages(),
                        fiche.nombreDeRencontres(),
                        fiche.aUneVignette(),
                        null,
                        null))
                .toList();
    }

    /** Une fiche entière : l'entête, la timeline d'apparition et le fil de conversation. */
    @GetMapping("/{id}")
    public ResponseEntity<FichePersonneDto> parId(@PathVariable String id) {
        RepertoireDesPersonnes.FichePersonne fiche = repertoire.ficheDe(id);
        if (fiche == null) {
            return ResponseEntity.notFound().build();
        }
        Personne personne = fiche.personne();
        List<Rencontre> rencontres = repertoire.rencontres(id);
        return ResponseEntity.ok(new FichePersonneDto(personne.id(),
                personne.prenom(),
                instant(personne),
                fiche.nombreDeVisages(),
                fiche.nombreDeRencontres(),
                fiche.aUneVignette(),
                rencontres.stream()
                        .map(rencontre -> new FichePersonneDto.RencontreDto(rencontre.instant().toString(),
                                rencontre.type().name(),
                                rencontre.secondesDAbsence()))
                        .toList(),
                repertoire.conversation(id).stream()
                        .map(message -> new FichePersonneDto.MessageDto(message.getMessageType().name(),
                                message.getText()))
                        .toList()));
    }

    /**
     * Le portrait d'une personne.
     * <p>
     * Ressource à part, et mise en cache par le navigateur : une liste de trente personnes
     * demanderait sinon trente images à chaque affichage. La vignette ne change qu'à un nouvel
     * enrôlement, un cache d'une heure est donc sans risque — et {@code no-cache} sur un flux
     * d'images ferait ramer la tablette.
     */
    @GetMapping(value = "/{id}/vignette", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> vignette(@PathVariable String id) {
        byte[] vignette = repertoire.vignette(id);
        if (vignette == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(vignette);
    }

    /**
     * Crée quelqu'un à partir d'une ou plusieurs photos.
     * <p>
     * Tout d'un coup, en multipart : une personne sans visage appris ne serait jamais reconnue,
     * la créer d'abord et l'apprendre ensuite n'aurait donc pas de sens. Plusieurs photos valent
     * mieux qu'une — une seule prise de trois quarts et le robot ne la reconnaît plus.
     */
    @PostMapping
    public ResponseEntity<?> creer(@RequestParam String prenom, @RequestParam("photos") List<MultipartFile> photos) {
        try {
            Personne creee = repertoire.creerDepuisPhotos(prenom, octets(photos));
            return parId(creee.id());
        } catch (IllegalArgumentException | ApprentissageParPhoto.PhotoInexploitable e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "photo illisible"));
        }
    }

    /** Ajoute des photos à quelqu'un de déjà connu : d'autres angles, ou une tête qui a changé. */
    @PostMapping("/{id}/visages")
    public ResponseEntity<?> ajouterDesVisages(@PathVariable String id,
                                               @RequestParam("photos") List<MultipartFile> photos) {
        try {
            Personne personne = repertoire.ajouterDesVisages(id, octets(photos));
            return personne == null ? ResponseEntity.notFound().build() : parId(id);
        } catch (ApprentissageParPhoto.PhotoInexploitable e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "photo illisible"));
        }
    }

    private static List<byte[]> octets(List<MultipartFile> photos) throws IOException {
        List<byte[]> lues = new ArrayList<>();
        for (MultipartFile photo : photos) {
            lues.add(photo.getBytes());
        }
        return lues;
    }

    /** Corrige un prénom mal compris. Rien d'autre ne bouge : ni les visages, ni l'histoire, ni le fil. */
    @PutMapping("/{id}")
    public ResponseEntity<FichePersonneDto> renommer(@PathVariable String id, @RequestBody Map<String, String> corps) {
        String prenom = corps.get("prenom");
        if (prenom == null || prenom.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Personne renommee = repertoire.renommer(id, prenom);
        return renommee == null ? ResponseEntity.notFound().build() : parId(renommee.id());
    }

    /** Oublie la personne partout à la fois. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        return repertoire.supprimer(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Oublie le fil de conversation sans oublier la personne : elle reste connue et reconnue, mais
     * le robot repart d'une page blanche avec elle.
     */
    @DeleteMapping("/{id}/conversation")
    public ResponseEntity<Void> oublierLaConversation(@PathVariable String id) {
        return repertoire.oublierLaConversation(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static String instant(Personne personne) {
        return personne.derniereRencontre() == null ? null : personne.derniereRencontre().toString();
    }
}
