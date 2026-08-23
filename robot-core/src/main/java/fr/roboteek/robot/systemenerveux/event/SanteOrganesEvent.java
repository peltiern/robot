package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.securite.SanteOrgane;

import java.util.List;

/**
 * État vital de tous les organes surveillés, publié périodiquement par le watchdog sur
 * {@code /events/sante-organes}.
 * <p>
 * La ressource REST {@code /api/organes} donne déjà cette santé, mais elle n'est lue qu'à la
 * (re)connexion : des pastilles d'état figées sur un instantané vieux de dix minutes seraient pires
 * que pas de pastilles du tout — elles afficheraient « tout va bien » sur un organe mort depuis
 * longtemps. D'où ce rafraîchissement continu, sur le même relevé que celui qui décide de couper
 * les moteurs.
 */
public class SanteOrganesEvent extends RobotEvent {

    public static final String EVENT_TYPE = "sante-organes";

    /** Santé de chaque organe surveillé, au moment du relevé. */
    private final List<SanteOrgane> organes;

    public SanteOrganesEvent(List<SanteOrgane> organes) {
        super(EVENT_TYPE);
        this.organes = organes;
    }

    public List<SanteOrgane> getOrganes() {
        return organes;
    }

}
