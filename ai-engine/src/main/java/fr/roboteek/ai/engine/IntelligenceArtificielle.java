package fr.roboteek.ai.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.google.gson.Gson;

import fr.roboteek.ai.engine.model.AIPattern;
import fr.roboteek.ai.engine.model.AIUnity;
import fr.roboteek.ai.engine.model.AIUnityList;
import fr.roboteek.ai.engine.model.Response;

public class IntelligenceArtificielle {

    /** Chemin du dossier contenant les ressources nécessaires à l'intelligence artificielle. */
    private String cheminRessources;

    /** Nom. */
    private String nom;

    /** Liste des unités d'intelligence artificielle. */
    private AIUnityList aiUnityList;

    /** Sets (clé : nom du set, valeur : liste des éléments du set sous forme de chaine de caractères ([e1|e2|...]). */
    private Map<String, String> mapSets = new HashMap<String, String>();

    /** Map des variables de la discussion. */
    private Map<String, String> mapVariables = new HashMap<String, String>();


    public IntelligenceArtificielle(String cheminRessources) {

        this.cheminRessources = cheminRessources;

        aiUnityList = new AIUnityList();

        // Lecture des fichiers
        System.out.println("Chargement des fichiers en cours (" + cheminRessources + ") ...");
        long debut = System.currentTimeMillis();
        initialisationProprietes();
        initialisationUnites();
        initialisationSets();
        long fin = System.currentTimeMillis();
        System.out.println("Temps = " + (fin - debut));

    }

    private void initialisationProprietes() {
        // Fichier des propriétés
        Properties properties = new Properties();
        try {
            FileInputStream in = new FileInputStream(cheminRessources + File.separator + "ai.properties");
            properties.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to load config file.");
        }
        // Récupération du nom
        nom = properties.getProperty("nom", "Samy");
        System.out.println("Nom = " + nom);
    }

    private void initialisationUnites() {
        // Parcours des fichiers d'unités (de type JSON)
        try {
            Gson gson = new Gson();
            Path repertoireUnites = Paths.get(cheminRessources + File.separator + "unities");
            DirectoryStream<Path> stream = Files.newDirectoryStream(repertoireUnites, "*.json");
            try { 
                Iterator<Path> iterator = stream.iterator();
                while(iterator.hasNext()) {
                    Path p = iterator.next();
                    System.out.println(p.getFileName());
                    BufferedReader in = Files.newBufferedReader(p,Charset.forName("UTF-8"));
                    aiUnityList.addAiUnityList(gson.fromJson(in, AIUnityList.class));
                }
            } finally { 
                stream.close(); 
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to load unity files.");
        }

        System.out.println(aiUnityList);

    }

    private void initialisationSets() {
        // Parcours des fichiers de sets (de type txt)

        try {
            Path repertoireSets = Paths.get(cheminRessources + File.separator + "sets");
            DirectoryStream<Path> stream = Files.newDirectoryStream(repertoireSets, "*.txt");
            Iterator<Path> iterator = stream.iterator();
            while(iterator.hasNext()) {
                Path p = iterator.next();

                // Récupération du nom du fichier
                final String nomFichier = FilenameUtils.getBaseName(p.getFileName().toString());

                // Création d'un Set pour l'ajout des valeurs contenues dans le fichier
                final StringBuffer sb = new StringBuffer("");

                // Lecture des lignes du fichier et construction de la liste sous la forme [e1|e2|...|en]
                sb.append(StringUtils.join(Files.readAllLines(p, Charset.forName("UTF-8")), "|"));

                // Ajout du set à la map
                mapSets.put(nomFichier, sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to load unity files.");
        }

        System.out.println("Sets = " + mapSets);
    }

    public String action(String texte) {
        String reponse = null;

        // Parcours des unités d'AI pour trouver une unité qui correspond
        AIUnity matchedAiUnity = null;
        Matcher matcher = null;

        if (aiUnityList != null && aiUnityList.getListeUnities() != null && !aiUnityList.getListeUnities().isEmpty()) {
            for (AIUnity aiUnity: aiUnityList.getListeUnities()) {
                // Compilation du pattern correspondant à l'input
                AIPattern aiPattern = aiUnity.getInput().compilePattern(mapSets);
                // Récupération du matcher
                matcher = aiPattern.matcher(texte);
                if (matcher.find()) {
                    matchedAiUnity = aiUnity;
                    break;
                }
            }
        }

        // Si une unité a été trouvée
        if (matchedAiUnity != null && matcher != null) {
            // Récupération des noms des variables présentes dans le pattern
            final Set<String> nomsVariablesLocales = matchedAiUnity.getInput().getPattern().getListeNomsVariablesLocales();
            final Set<String> nomsVariablesGlobales = matchedAiUnity.getInput().getPattern().getListeNomsVariablesGlobales();
            
            // Récupération de la valeur des variables
            final Map<String, String> mapValeursVariablesLocales = new HashMap<String, String>();
            if (nomsVariablesLocales != null && !nomsVariablesLocales.isEmpty()) {
                for (String nomVariableLocale : nomsVariablesLocales) {
                    String valeurVariable = matcher.group(nomVariableLocale);
                    mapValeursVariablesLocales.put(nomVariableLocale, valeurVariable);
                    // Sauvegarde en global si c'est aussi une variable globale
                    if (nomsVariablesGlobales != null && nomsVariablesGlobales.contains(nomVariableLocale)) {
                        mapVariables.put(nomVariableLocale, valeurVariable);
                    }
                }
            }

            // Récupération d'une réponse
            Response response = null;
            if (matchedAiUnity.getListeResponses() != null && !matchedAiUnity.getListeResponses().isEmpty()) {
                if (matchedAiUnity.getListeResponses().size() == 1) {
                    response = matchedAiUnity.getListeResponses().get(1);
                } else {
                    // Tirage aléatoire d'une réponse
                    response = matchedAiUnity.getListeResponses().get(RandomUtils.nextInt(matchedAiUnity.getListeResponses().size()));
                }
            }

            // Construction de la réponse
            if (response != null) {
                reponse = response.answer(mapValeursVariablesLocales, mapVariables);
            }
        }

        return reponse;
    }

    public static void main(String[] args) {
        String cheminRessources = "C:/Users/Nicolas/Documents/Robot/Programme/ai";
        final IntelligenceArtificielle ai = new IntelligenceArtificielle(cheminRessources);
        
        
        long debut = System.currentTimeMillis();
        
        String test = "Je m'appelle Nicolas et j'aime la couleur bleu et mon ami s'appelle Adeline";
        System.out.println("MOI : " + test);
        String reponse = ai.action(test);
        System.out.println("AI : " + reponse);
        test = "Comment je m'appelle ?";
        System.out.println("MOI : " + test);
        reponse = ai.action(test);
        System.out.println("AI : " + reponse);
        test = "Je m'appelle Adeline et j'aime la couleur vert et mon ami s'appelle Amanda";
        System.out.println("MOI : " + test);
        reponse = ai.action(test);
        System.out.println("AI : " + reponse);
        test = "Comment je m'appelle ?";
        System.out.println("MOI : " + test);
        reponse = ai.action(test);
        System.out.println("AI : " + reponse);
        
        long fin = System.currentTimeMillis();
        
        System.out.println("Temps = " + (fin - debut));

    }
}
