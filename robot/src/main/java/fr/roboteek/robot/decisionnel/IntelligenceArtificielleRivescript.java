package fr.roboteek.robot.decisionnel;

import com.rivescript.exception.ReplyNotMatchedException;
import org.apache.log4j.Logger;

import com.rivescript.Config;
import com.rivescript.RiveScript;

import fr.roboteek.robot.Constantes;

/**
 * Intelligence artificielle du robot permettant notamment de faire la conversation.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class IntelligenceArtificielleRivescript {

	/** Système conversationnel. */
	private RiveScript  bot;

	/** Logger. */
	private Logger logger = Logger.getLogger(IntelligenceArtificielleRivescript.class);

	public IntelligenceArtificielleRivescript() {
		bot = new RiveScript(Config.Builder
		        .utf8()
		        .unicodePunctuation("[.,!?;:]")
				.throwExceptions(true)
		        .build());
		bot.loadDirectory(Constantes.DOSSIER_RIVESCRIPT);
		bot.sortReplies();
	}

	/**
	 * Répond à une phrase.
	 * @param phrase la phrase
	 * @return la réponse issue de l'intelligence artificielle
	 */
	public String repondreAPhrase(String phrase) {
		try {
			if (phrase != null) {
				return bot.reply("humain", phrase.toLowerCase().replace("'", " "));
			} else {
				return "";
			}
		} catch (ReplyNotMatchedException e) {
			StringBuilder reponseNonTrouvee = new StringBuilder();
			reponseNonTrouvee.append("Désolé !").append(" Je suis incapable de répondre à la phrase : ").append(phrase);
			return reponseNonTrouvee.toString();
		}
	}

}
