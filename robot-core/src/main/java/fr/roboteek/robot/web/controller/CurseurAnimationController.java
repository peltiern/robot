package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.animation.LecteurAnimation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le curseur de la timeline : « place la tête où l'animation en est à cet instant ».
 * <p>
 * <b>Websocket et non REST</b>, contrairement à tout le reste du chantier. Ce n'est pas une
 * inconséquence : tirer un curseur produit un flot d'instants, plusieurs dizaines par seconde,
 * qui n'appellent aucune réponse et dont seul le dernier compte. Une requête HTTP par position
 * paierait un aller-retour complet pour un ordre qui sera périmé avant d'arriver. Et ce n'est pas
 * une ressource : rien n'est créé, lu ni remplacé — c'est un geste.
 * <p>
 * Le lecteur écrête ce flot à sa propre cadence et laisse tomber le reste, sans quoi l'éditeur
 * dépasserait à lui seul les 55 écritures par seconde du contrôleur. On ne compte pas sur le
 * client pour se brider : rien ne l'y oblige, et une page laissée ouverte suffirait.
 * <p>
 * Le décodage est explicite, comme dans {@link WebsocketController} : sous Spring Boot 4, les
 * convertisseurs de messages par défaut ne savent pas construire ce que le front envoie.
 */
@Controller
@CrossOrigin(origins = "*")
public class CurseurAnimationController {

    private static final Logger logger = LoggerFactory.getLogger(CurseurAnimationController.class);

    private final LecteurAnimation lecteur;

    private final ObjectMapper json = JsonMapper.builder().build();

    public CurseurAnimationController(LecteurAnimation lecteur) {
        this.lecteur = lecteur;
    }

    /**
     * Un déplacement du curseur : l'animation en cours d'écriture, et l'instant visé.
     *
     * @param instant position du curseur dans la timeline, en millisecondes
     */
    public record DeplacementCurseur(Animation animation, long instant) {
    }

    @MessageMapping("/animation/curseur")
    public void deplacerLeCurseur(@Payload String corps) {
        DeplacementCurseur deplacement;
        try {
            deplacement = json.readValue(corps, DeplacementCurseur.class);
        } catch (JacksonException e) {
            // Journalisé une fois et abandonné : un message mal formé ne doit pas tuer l'abonnement
            // websocket, et l'éditeur en enverra un autre au prochain mouvement de souris.
            logger.warn("Déplacement de curseur illisible, ignoré : {}", e.getOriginalMessage());
            return;
        }
        if (deplacement == null || deplacement.animation() == null) {
            return;
        }
        lecteur.positionner(deplacement.animation(), deplacement.instant());
    }
}
