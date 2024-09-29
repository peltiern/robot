package fr.roboteek.robot.configuration;

/**
 * Accès aux clés d'API par variables d'environnements.
 */
public class ApiKeys {
    public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    public static final String GOOGLE_SPEECH_API_KEY = System.getenv("GOOGLE_SPEECH_API_KEY");
}
