package fr.roboteek.robot.systemenerveux.event;

import java.util.Map;

/**
 * Évènement de télémétrie générique porté par n'importe quel organe : une carte plate
 * {@code id → valeur} des grandeurs qui varient dans le temps, mêmes identifiants que ceux
 * exposés par la ressource REST {@code /api/organes} pour cet organe — la position courante
 * d'une articulation d'actionneur (ex. {@code Cou}, {@code Yeux}) aussi bien qu'une mesure de
 * capteur (ex. {@code CapteurMateriel}).
 * <p>
 * Un seul type d'évènement pour toute télémétrie « valeur(s) courante(s) d'un organe », plutôt
 * qu'une classe dédiée par organe : la découverte de capacités ({@code /api/organes}) reste la
 * source de vérité pour la structure (libellés, unités, bornes) — cet évènement ne porte que les
 * valeurs qui varient. Plusieurs organes peuvent publier ce même évènement ; ils partagent alors
 * le même topic WebSocket ({@code /events/telemetrie-organe}, routé génériquement par
 * {@code WebsocketBroadcaster} d'après {@link #getEventType()}), un client filtrant par
 * {@code organeId} ou simplement par les identifiants de valeur qu'il connaît.
 */
public class TelemetrieOrganeEvent extends RobotEvent {

    public static final String EVENT_TYPE = "telemetrie-organe";

    /** Identifiant de l'organe émetteur (ex. {@code cou}, {@code yeux}, {@code materiel}). */
    private String organeId;

    /** Carte {@code id → valeur} (mêmes identifiants que dans {@code /api/organes}). */
    private Map<String, Double> valeurs;

    public TelemetrieOrganeEvent() {
        super(EVENT_TYPE);
    }

    public TelemetrieOrganeEvent(String organeId, Map<String, Double> valeurs) {
        super(EVENT_TYPE);
        this.organeId = organeId;
        this.valeurs = valeurs;
    }

    public String getOrganeId() {
        return organeId;
    }

    public void setOrganeId(String organeId) {
        this.organeId = organeId;
    }

    public Map<String, Double> getValeurs() {
        return valeurs;
    }

    public void setValeurs(Map<String, Double> valeurs) {
        this.valeurs = valeurs;
    }
}
