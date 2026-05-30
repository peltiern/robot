package fr.roboteek.robot.spring.server.controller;

import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationRepository;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationValidator;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationValidator.ValidationWarning;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.List;

@RestController
@RequestMapping("/api/animations")
@CrossOrigin(origins = "*")
public class AnimationController {

    private static final Logger logger = LoggerFactory.getLogger(AnimationController.class);

    private final AnimationRepository repository;
    private final AnimationValidator validator;

    public AnimationController(AnimationRepository repository, AnimationValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /** Liste les noms de toutes les animations sauvegardées. */
    @GetMapping
    public List<String> listNames() {
        return repository.listNames();
    }

    /** Retourne une animation par son nom. 404 si introuvable. */
    @GetMapping("/{name}")
    public ResponseEntity<Animation> get(@PathVariable String name) {
        return repository.load(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Sauvegarde une nouvelle animation. Retourne l'animation + les avertissements de validation. */
    @PostMapping
    public ResponseEntity<SaveResponse> create(@RequestBody Animation animation) {
        if (animation.getName() == null || animation.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<ValidationWarning> warnings = validator.validate(animation);
        warnings.forEach(w -> logger.warn("Validation [{}] : {}", w.trackId(), w.message()));
        repository.save(animation);
        return ResponseEntity.ok(new SaveResponse(animation, warnings));
    }

    /** Met à jour une animation existante. 404 si le nom dans l'URL ne correspond pas au body. */
    @PutMapping("/{name}")
    public ResponseEntity<SaveResponse> update(@PathVariable String name, @RequestBody Animation animation) {
        animation.setName(name);
        List<ValidationWarning> warnings = validator.validate(animation);
        warnings.forEach(w -> logger.warn("Validation [{}] : {}", w.trackId(), w.message()));
        repository.save(animation);
        return ResponseEntity.ok(new SaveResponse(animation, warnings));
    }

    /** Supprime une animation. 404 si introuvable. */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        if (!repository.exists(name)) {
            return ResponseEntity.notFound().build();
        }
        repository.delete(name);
        return ResponseEntity.noContent().build();
    }

    /** Joue une animation sauvegardée. 404 si introuvable. */
    @PostMapping("/{name}/play")
    public ResponseEntity<Void> play(@PathVariable String name) {
        if (!repository.exists(name)) {
            return ResponseEntity.notFound().build();
        }
        PlayAnimationEvent event = new PlayAnimationEvent();
        event.setAnimationName(name);
        RobotEventBus.getInstance().publishAsync(event);
        logger.info("Lecture de l'animation '{}'", name);
        return ResponseEntity.ok().build();
    }

    /** Joue une animation passée dans le body sans la sauvegarder (live preview). */
    @PostMapping("/preview")
    public ResponseEntity<List<ValidationWarning>> preview(@RequestBody Animation animation) {
        List<ValidationWarning> warnings = validator.validate(animation);
        warnings.forEach(w -> logger.warn("Preview [{}] : {}", w.trackId(), w.message()));
        PlayAnimationEvent event = new PlayAnimationEvent();
        event.setAnimation(animation);
        RobotEventBus.getInstance().publishAsync(event);
        return ResponseEntity.ok(warnings);
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public record SaveResponse(Animation animation, List<ValidationWarning> warnings) {}
}
