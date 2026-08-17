package fr.roboteek.robot.web.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suit les abonnements STOMP en cours, pour permettre aux organes de ne produire un flux
 * coûteux que si quelqu'un l'écoute réellement.
 * <p>
 * Sans ce registre, le capteur de vision encode une image JPEG en base64 (~120 Ko) dix fois
 * par seconde même quand aucun navigateur n'est ouvert : le courtier « simple » de Spring
 * jette bien le message faute d'abonné, mais le coût CPU de l'encodage a déjà été payé.
 * <p>
 * Les trois évènements de cycle de vie sont nécessaires : {@code UNSUBSCRIBE} pour une
 * fermeture propre de la page, {@code DISCONNECT} pour un onglet fermé ou un WiFi coupé
 * (aucun {@code UNSUBSCRIBE} n'est alors reçu, la session part avec ses abonnements).
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Component
public class RegistreAbonnesWebsocket {

    private static final Logger logger = LoggerFactory.getLogger(RegistreAbonnesWebsocket.class);

    /**
     * Destinations abonnées, par identifiant de session puis d'abonnement. Une map imbriquée
     * (plutôt qu'une clé composite) pour purger une session entière en une seule opération.
     */
    private final Map<String, Map<String, String>> destinationsParSession = new ConcurrentHashMap<>();

    @EventListener
    public void surAbonnement(SessionSubscribeEvent evenement) {
        StompHeaderAccessor entetes = StompHeaderAccessor.wrap(evenement.getMessage());
        String destination = entetes.getDestination();
        String idSession = entetes.getSessionId();
        if (destination == null || idSession == null) {
            return;
        }
        destinationsParSession
                .computeIfAbsent(idSession, cle -> new ConcurrentHashMap<>())
                .put(idAbonnement(entetes, destination), destination);
        logger.debug("Abonnement à {} (session {})", destination, idSession);
    }

    @EventListener
    public void surDesabonnement(SessionUnsubscribeEvent evenement) {
        StompHeaderAccessor entetes = StompHeaderAccessor.wrap(evenement.getMessage());
        String idSession = entetes.getSessionId();
        if (idSession == null) {
            return;
        }
        Map<String, String> abonnements = destinationsParSession.get(idSession);
        if (abonnements != null) {
            // La trame UNSUBSCRIBE ne porte pas la destination, seulement l'id d'abonnement.
            abonnements.remove(idAbonnement(entetes, null));
        }
    }

    @EventListener
    public void surDeconnexion(SessionDisconnectEvent evenement) {
        destinationsParSession.remove(evenement.getSessionId());
        logger.debug("Session {} déconnectée : abonnements purgés", evenement.getSessionId());
    }

    /**
     * Indique si au moins un client est abonné à la destination donnée.
     *
     * @param destination destination STOMP exacte (par exemple {@code /video})
     * @return true si au moins un abonné écoute cette destination
     */
    public boolean aAuMoinsUnAbonne(String destination) {
        for (Map<String, String> abonnements : destinationsParSession.values()) {
            if (abonnements.containsValue(destination)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identifiant d'abonnement, avec repli sur la destination : la spécification STOMP impose
     * l'entête {@code id} sur SUBSCRIBE, mais ce repli évite qu'un client exotique fasse
     * grossir la map indéfiniment sous une clé nulle.
     */
    private static String idAbonnement(StompHeaderAccessor entetes, String destination) {
        String id = entetes.getSubscriptionId();
        return id != null ? id : String.valueOf(destination);
    }
}
