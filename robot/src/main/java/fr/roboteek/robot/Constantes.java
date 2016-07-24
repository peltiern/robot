package fr.roboteek.robot;

import java.io.File;

/** Constantes du projet. */
public class Constantes {
	
	/** Clé de la variable d'environnement contenant la clé de l'API Google. */
	public static final String ENV_VAR_ROBOT_HOME = "ROBOT_HOME";
	
	/** Chemin vers le dossier de reconnaissance vocale. */
	public static final String DOSSIER_RECONNAISSANCE_VOCALE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "reconnaissanceVocale";

}
