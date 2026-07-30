package fr.roboteek.robot;

import java.io.File;

/**
 * Constantes du projet.
 */
public class Constantes {

    /**
     * Clé de la variable d'environnement contenant la clé de l'API Google.
     */
    public static final String ENV_VAR_ROBOT_HOME = "ROBOT_HOME";

    /**
     * Fréquence d'échantillonnage de la capture audio, en Hz.
     * <p>
     * Source unique de vérité, volontairement partagée par la capture
     * ({@link fr.roboteek.robot.organes.capteurs.AbstractCapteurVocal}) et par les deux moteurs de
     * reconnaissance : toute divergence entre ces trois endroits corrompt silencieusement la
     * reconnaissance, sans erreur ni log.
     * <p>
     * 16 kHz n'est pas un choix mais une contrainte matérielle : le micro (ReSpeaker 4 Mic Array)
     * n'expose que cette fréquence ({@code /proc/asound/cardN/stream0} : {@code Rates: 16000}), et
     * c'est aussi la fréquence native des modèles Vosk. Capturer à 44,1 kHz comme précédemment
     * faisait suréchantillonner ALSA puis sous-échantillonner Java Sound, sans apporter la moindre
     * information (mesuré : -47 dB d'énergie au-dessus de 8 kHz dans une capture à 44,1 kHz).
     */
    public static final int FREQUENCE_ECHANTILLONNAGE_CAPTURE_HZ = 16000;

    /**
     * Chemin vers le dossier de reconnaissance vocale.
     */
    public static final String DOSSIER_RECONNAISSANCE_VOCALE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "reconnaissanceVocale";

    /**
     * Chemin vers le dossier de synthèse vocale.
     */
    public static final String DOSSIER_SYNTHESE_VOCALE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "synthese-vocale";

    /**
     * Chemin vers le dossier de gestion du visage.
     */
    public static final String DOSSIER_VISAGE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "visage";

    /**
     * Chemin vers le dossier du système conversationnel Rivescript.
     */
    public static final String DOSSIER_RIVESCRIPT = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "rivescript";

    /**
     * Chemin vers le dossier des sons.
     */
    public static final String DOSSIER_SONS = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "sounds";

    /**
     * Chemin vers le dossier de vision artificielle.
     */
    public static final String DOSSIER_VISION_ARTIFICIELLE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "vision-artificielle";

    /**
     * Chemin vers le dossier de tests.
     */
    public static final String DOSSIER_TESTS = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "tests";

    /**
     * Chemin vers le dossier de JInput.
     */
    public static final String DOSSIER_JINPUT = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "jinput";

    /**
     * Chemin vers le dossier de mémoire.
     */
    public static final String DOSSIER_MEMOIRE = System.getenv(ENV_VAR_ROBOT_HOME) + File.separator + "memoire";

    /**
     * Chemin vers le dossier de mémoire des conversations.
     */
    public static final String DOSSIER_MEMOIRE_CONVERSATIONS = DOSSIER_MEMOIRE + File.separator + "conversations";

}
