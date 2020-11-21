package fr.roboteek.robot;

import java.io.File;

/** Constantes du projet. */
public class Constantes {
	
	/** Clé de la variable d'environnement contenant la clé de l'API Google. */
	public static final String ENV_VAR_ROBOT_HOME = "ROBOT_HOME";
	
	/** Chemin vers le dossier de reconnaissance vocale. */
	public static final String DOSSIER_RECONNAISSANCE_VOCALE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "reconnaissanceVocale";

	/** Chemin vers le dossier de synthèse vocale. */
	public static final String DOSSIER_SYNTHESE_VOCALE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "synthese-vocale";
	
	/** Chemin vers le dossier de gestion du visage. */
	public static final String DOSSIER_VISAGE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "visage";
	
	/** Chemin vers le dossier du système conversationnel Rivescript. */
	public static final String DOSSIER_RIVESCRIPT = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "rivescript";

	/** Chemin vers le dossier des sons. */
	public static final String DOSSIER_SONS = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "sounds";

	/** Chemin vers le dossier de reconnaissance faciale. */
	public static final String DOSSIER_RECONNAISSANCE_FACIALE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "reconnaissance-visage";

	/** Chemin vers le dossier de tests. */
	public static final String DOSSIER_TESTS = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "tests";

}
