package fr.roboteek.robot.sandbox.ai.google;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GoogleSearchParser {

    public static void main(String[] args) {

        GoogleSearchParser gsp = new GoogleSearchParser();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Ask a question : ");
        String query = scanner.nextLine();
        while (!"q".equalsIgnoreCase(query)) {
            String response = gsp.getResultsFromGoogle(query);
            System.out.println("Response = " + response);
            System.out.println("Ask a question : ");
            query = scanner.nextLine();
        }
        System.out.println("Exit");
    }

    private String getResultsFromGoogle(String query) {

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            Document doc = Jsoup.connect("https://www.google.com/search?q=" + encodedQuery).get();
            // Extract relevant information in "knowledge" tags
            // Weather infos
            Element element = doc.select("[id=\"wob_wc\"]").first();
            if (element != null) {
                return processWeatherInfos(element);
            }

            element = doc.select("[data-attrid], [data-tts=\"answers\"], [id=\"cwos\"]").not("[data-attrid=\"image\"]").first();
            if (element != null) {
                // Clean all HTML tag of the selected node
                Document cleanDoc = Jsoup.parse(Jsoup.clean(element.html(), Whitelist.none()));
                return cleanDoc.text();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
