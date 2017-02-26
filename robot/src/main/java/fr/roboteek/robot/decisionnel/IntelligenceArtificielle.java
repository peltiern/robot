package fr.roboteek.robot.decisionnel;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.apache.log4j.Logger;

import fr.roboteek.robot.activites.ChatBotActivite;

/**
 * Intelligence artificielle du robot permettant notamment de faire la conversation.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class IntelligenceArtificielle {
    
    /** Système conversationnel. */
    private Chat chat;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(IntelligenceArtificielle.class);
    
    public IntelligenceArtificielle() {
        // Création du chat
        String botname="amy";
        String path = System.getProperty("robot.dir");
        final Bot bot = new Bot(botname, path);
        chat = new Chat(bot);
    }
    
    /**
     * Répond à une phrase.
     * @param phrase la phrase
     * @return la réponse issue de l'intelligence artificielle
     */
    public String repondreAPhrase(String phrase) {
        // TODO traiter le résultat en cas d'action
        return chat.multisentenceRespond(phrase);
    }

}
