package fr.roboteek.robot.decisionnel;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class IntelligenceArtificielleGoogleKnowledge {

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(IntelligenceArtificielleGoogleKnowledge.class);

    public IntelligenceArtificielleGoogleKnowledge() {

    }

    /**
     * Répond à une requête.
     *
     * @param requete la requête demandé (une phrase ou un fichier son)
     * @return la réponse issue de l'intelligence artificielle
     */
    public ReponseIntelligenceArtificielle repondreARequete(RequeteIntelligenceArtificielle requete) {

        if (StringUtils.isNotEmpty(requete.getInputText())) {
            return traiterRequeteTexte(requete.getInputText());
        }

        return null;
    }

    private ReponseIntelligenceArtificielle traiterRequeteTexte(String inputText) {
        ReponseIntelligenceArtificielle response = new ReponseIntelligenceArtificielle();
        response.setInputText(inputText);
        try {
            String encodedQuery = URLEncoder.encode(inputText, StandardCharsets.UTF_8.toString());
            Document doc = Jsoup.connect("https://www.google.com/search?q=" + encodedQuery).get();
//            Element element = doc.select("[data-attrid=\"description\"]").first();
//            if (element == null) {
            Element element = doc.select("[data-attrid], [data-tts=\"answers\"], [id=\"cwos\"]").not("[data-attrid=\"image\"]").first();
//            }
            if (element != null) {
                Document cleanDoc = Jsoup.parse(Jsoup.clean(element.html(), Whitelist.none()));
                response.setOutputText(cleanDoc.text());
//            for (TextNode textNode : textNodes) {
//                System.out.println(textNode.text());
//            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
