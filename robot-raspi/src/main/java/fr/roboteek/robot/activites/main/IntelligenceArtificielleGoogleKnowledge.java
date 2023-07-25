package fr.roboteek.robot.activites.main;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class IntelligenceArtificielleGoogleKnowledge {

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(IntelligenceArtificielleGoogleKnowledge.class);

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
            // Weather infos
            Element element = doc.select("[id=\"wob_wc\"]").first();
            if (element != null) {
                response.setOutputText(processWeatherInfos(element));
            }
            element = doc.select("[data-attrid], [data-tts=\"answers\"], [id=\"cwos\"]")
                    .not("[data-attrid=\"image\"]")
                    .not("[data-attrid=\"kc:/music/recording_cluster:lyrics\"]")
                    .first();
//            }
            if (element != null) {
                Document cleanDoc = Jsoup.parse(Jsoup.clean(element.html(), Safelist.none()));
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

    private String processWeatherInfos(Element weatherElement) {
        // Location
        String location = weatherElement.select("[id=\"wob_loc\"]").text();
        // Time
        String timestamp = weatherElement.select("[id=\"wob_dts\"]").text();
        // Weather
        String weather = weatherElement.select("[id=\"wob_dc\"]").text();
        // Temperature
        String temperature = weatherElement.select("[id=\"wob_tm\"]").text();

        return String.format("A %s, %s, le temps est %s, avec une température de %s °", location, timestamp, weather, temperature);
    }
}
