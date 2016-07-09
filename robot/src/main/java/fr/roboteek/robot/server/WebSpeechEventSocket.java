package fr.roboteek.robot.server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 * Websocket permettant la communication avec la page HTML implémentant la Web Speech API.
 * @author Java Developer
 */
public class WebSpeechEventSocket extends WebSocketAdapter {

    private static final String PREFIXE_RESULTAT = "[RECO-RESULTAT]";

    public WebSpeechEventSocket() {
        super();
    }

    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        System.out.println("Socket Connected: " + sess);

        // Une fois connectée, la socket est définie au niveau du serveur
        WebSpeechServer.getInstance().setWebSpeechEventSocket(this);
    }

    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        System.out.println("Received TEXT message: " + message);

        if (message != null) {
            if (message.startsWith(PREFIXE_RESULTAT)) {
                // Si évènement [RECO-RESULTAT] : envoi de l'évènement "Résultat"
                String resultat = message.substring(message.indexOf("=") + 1);
                // Envoi du message au serveur
                WebSpeechServer.getInstance().onSpeechResult(resultat);
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}
