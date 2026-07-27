package fr.roboteek.robot.spring.server.websocket;

import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.organes.actionneurs.Yeux;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diffuse en continu les positions courantes des articulations vers les clients, pour que leurs
 * curseurs de contrôle suivent en direct les mouvements <b>physiques</b> du robot (manette,
 * animations, comportements) et pas seulement les commandes qu'ils envoient eux-mêmes.
 * <p>
 * Télémétrie légère : on pousse une carte {@code id → position (degrés)} sur {@code /events/positions}
 * (préfixe déjà routé par le broker), <b>uniquement</b> quand une position varie sensiblement — rien
 * n'est émis à l'arrêt, ni tant que les organes ne sont pas démarrés (positions indisponibles).
 * <p>
 * Les identifiants et le repère logique sont les mêmes que ceux exposés par la ressource REST
 * {@code /api/organes}, de sorte qu'un client peut réconcilier directement les deux.
 */
@Component
public class DiffusionPositionsArticulations {

    /** Destination STOMP (le préfixe {@code /events} est déjà routé par le broker). */
    private static final String DESTINATION = "/events/positions";

    /** Variation minimale (degrés) pour rediffuser : filtre l'immobilité sans hacher le mouvement. */
    private static final double SEUIL_VARIATION = 0.2;

    private final Cou cou;
    private final Yeux yeux;
    private final SimpMessagingTemplate template;

    /** Dernières positions diffusées, pour n'émettre que sur variation réelle. */
    private Map<String, Double> dernieresPositions = Map.of();

    public DiffusionPositionsArticulations(Cou cou, Yeux yeux, SimpMessagingTemplate template) {
        this.cou = cou;
        this.yeux = yeux;
        this.template = template;
    }

    /**
     * Lit et diffuse (best-effort) les positions courantes si elles ont varié depuis la dernière
     * émission. Un échec de sérialisation/envoi ne doit jamais interrompre la planification.
     */
    @Scheduled(fixedRate = 50)
    public void diffuser() {
        Map<String, Double> positions = positionsCourantes();
        if (positions.isEmpty() || !aVarie(positions)) {
            return;
        }
        dernieresPositions = positions;
        template.convertAndSend(DESTINATION, positions);
    }

    /**
     * Positions courantes des articulations démarrées (les getters renvoient {@code null} tant que
     * l'organe n'est pas actif). Mêmes identifiants que {@code /api/organes}.
     */
    private Map<String, Double> positionsCourantes() {
        Map<String, Double> positions = new LinkedHashMap<>();
        ajouter(positions, "oeilGauche", yeux.getPositionOeilGaucheCourante());
        ajouter(positions, "oeilDroit", yeux.getPositionOeilDroitCourante());
        ajouter(positions, "pan", cou.getPositionPanoramiqueCourante());
        ajouter(positions, "tilt", cou.getPositionInclinaisonCourante());
        ajouter(positions, "upDown", cou.getPositionMonterDescendreCourante());
        return positions;
    }

    private static void ajouter(Map<String, Double> positions, String id, Double valeur) {
        if (valeur != null) {
            positions.put(id, valeur);
        }
    }

    /** Vrai si l'ensemble des articulations a changé, ou si l'une a bougé de plus de {@link #SEUIL_VARIATION}. */
    private boolean aVarie(Map<String, Double> positions) {
        if (!positions.keySet().equals(dernieresPositions.keySet())) {
            return true;
        }
        for (Map.Entry<String, Double> entree : positions.entrySet()) {
            Double precedente = dernieresPositions.get(entree.getKey());
            if (precedente == null || Math.abs(entree.getValue() - precedente) > SEUIL_VARIATION) {
                return true;
            }
        }
        return false;
    }
}
