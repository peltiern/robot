package fr.roboteek.robot.util.gamepad.input4j;

import de.gurkenlabs.input4j.ControllerType;
import de.gurkenlabs.input4j.InputDevice;
import de.gurkenlabs.input4j.InputDevicePlugin;
import de.gurkenlabs.input4j.InputDevices;
import fr.roboteek.robot.util.gamepad.shared.AbstractGamepadManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Découverte, scrutation et <b>reconnexion à chaud</b> de la manette Logitech F710 via input4j
 * (mode XInput). Remplace l'ancien {@code GamepadManager} jinput (natifs + hack librarypath).
 * <p>
 * Sélectionne la première manette de type XBOX (la F710 en mode X est vue comme une manette Xbox
 * par le noyau) ou dont le nom évoque une F710, y branche le {@link LogitechControllerInput4j} et
 * la scrute à ~60 Hz dans un thread dédié. Les callbacks input4j se déclenchent pendant
 * {@code poll()}.
 *
 * <h2>Branchement à chaud : pourquoi c'est fait ici</h2>
 * input4j sait faire du hot-plug — {@code AbstractInputDevicePlugin} porte tout le mécanisme
 * ({@code refreshDevices()}, intervalle configurable, évènements de connexion/déconnexion) — mais
 * <b>seul le plugin XInput (Windows) s'en sert</b>. Côté Linux, celui qui tourne sur le robot,
 * {@code LinuxEventDevicePlugin.refreshInputDevices()} est un {@code // TODO: implement refresh
 * support} qui renvoie la liste inchangée, et personne n'appelle {@code refreshDevices()}. Les
 * évènements de déconnexion ne se déclenchent donc jamais.
 * <p>
 * D'où la reprise en main, en deux temps :
 * <ul>
 *   <li><b>Constater la perte</b> : le plugin Linux construit l'{@link InputDevice} avec le chemin
 *   absolu du nœud evdev comme identifiant, que le noyau supprime dès que le périphérique
 *   disparaît. Surveiller {@code /dev/input/eventX} revient donc à surveiller la manette.</li>
 *   <li><b>Rouvrir</b> : refaire un cycle complet ({@code plugin.close()} puis
 *   {@link InputDevices#init()}), parce que le descripteur ouvert au démarrage est mort avec le
 *   débranchement et qu'input4j n'en rouvre jamais d'autre. Une découverte neuve est d'ailleurs
 *   indispensable : au rebranchement, le noyau peut attribuer un tout autre numéro d'évènement.</li>
 * </ul>
 * Sans ça, constaté sur le robot : {@code poll()} continue de rendre la main sans
 * erreur sur un périphérique arraché — le plugin Linux se contente de renvoyer les <b>dernières
 * valeurs lues</b> — et la manette passe pour vivante, joystick figé sur sa dernière position
 * comprise. Exactement ce que le watchdog doit attraper.
 *
 * <h2>Pourquoi la découverte n'est pas répétée à intervalle fixe</h2>
 * Un cycle de découverte est <b>tout sauf gratuit</b> : {@code LinuxEventDevicePlugin} ouvre
 * <i>tous</i> les {@code /dev/input/event*} pour les interroger, et il <b>fuit un descripteur de
 * fichier par périphérique écarté</b> — bouton d'alimentation, HDMI, micro, clavier sont abandonnés
 * par un {@code continue} avant d'entrer dans la table que {@code close()} referme. À quoi s'ajoute
 * la création puis la fermeture d'une {@code Arena} partagée (mémoire native) à chaque tour.
 * <p>
 * Une tentative par seconde tant que la manette est absente — ce que faisait la première version —
 * revient donc à fuir une poignée de descripteurs par seconde <b>dans le processus qui pilote les
 * moteurs</b>. Le robot est parti en {@code SIGSEGV} dans du code natif, une minute
 * après un débranchement, au premier mouvement demandé.
 * <p>
 * D'où la règle : on ne relance une découverte que lorsqu'un <b>nouveau nœud</b> est apparu sous
 * {@code /dev/input} (plus quelques tentatives de rattrapage, le temps qu'udev applique les droits).
 * Brancher une manette crée forcément un nœud : rien n'est perdu, et entre deux branchements la
 * boucle ne fait plus qu'un {@code Files.list} par seconde.
 */
public class Input4jGamepadManager extends AbstractGamepadManager<LogitechListener> {

    private static final Logger logger = LoggerFactory.getLogger(Input4jGamepadManager.class);

    private static final long POLL_INTERVAL_MS = 16;

    /**
     * Cadence de la boucle quand aucune manette n'est ouverte : on cesse de scruter, il ne reste
     * qu'à guetter un branchement. Ça met fin au flot d'erreurs d'input4j, et une seconde de
     * latence à la reconnexion n'a jamais gêné personne.
     */
    private static final long INTERVALLE_RECONNEXION_MS = 1000;

    /** Répertoire des nœuds evdev, guetté pour repérer un branchement sans rien ouvrir. */
    private static final Path REPERTOIRE_ENTREES = Path.of("/dev/input");

    /**
     * Tentatives de découverte accordées à chaque apparition d'un nœud. Plus d'une, parce que le
     * nœud existe avant qu'udev lui ait donné ses droits : la première tentative peut échouer sur
     * une manette pourtant bien branchée. Peu, parce que chaque tentative coûte des descripteurs.
     */
    private static final int TENTATIVES_PAR_BRANCHEMENT = 3;

    /**
     * Période de vérification de la présence du nœud evdev. La scrutation tourne à 60 Hz, mais le
     * watchdog raisonne à l'échelle de la seconde : inutile d'interroger le système de
     * fichiers à chaque tour.
     */
    private static final long PERIODE_VERIFICATION_PRESENCE_MS = 250;

    /**
     * Passé ce délai, une manette absente n'est plus une anomalie mais un fait acquis : elle n'est
     * plus « en panne », elle n'est plus là.
     * <p>
     * Sans cette bascule, une manette débranchée resterait éternellement muette et le watchdog
     * couperait les moteurs à chaque mouvement demandé depuis la tablette — rendant le
     * pilotage à la tablette seule impossible, ce qui est pourtant un usage légitime. La fenêtre
     * doit rester nettement plus large que le délai de silence toléré, pour que la perte survenue
     * <b>pendant</b> un mouvement, elle, déclenche bien.
     */
    private static final long DELAI_ABSENCE_ACQUISE_MS = 15_000;

    private Thread pollThread;
    private volatile boolean running = false;

    /** Plugin input4j courant. Refermé et recréé à chaque cycle de reconnexion. */
    private InputDevicePlugin plugin;

    /** Manette ouverte, ou {@code null} si aucune n'est branchée pour l'instant. */
    private volatile InputDevice manette;

    /**
     * Nœud evdev de la manette ouverte, ou {@code null} si l'identifiant n'est pas un chemin
     * exploitable (ailleurs que sous Linux) : on ne teste alors plus la présence.
     */
    private Path noeudPeripherique;

    /**
     * Horodatage du dernier tour de scrutation où la manette était réellement là. C'est le
     * battement lu par le watchdog via {@code RobotLogitechController}.
     */
    private volatile long dernierPollReussi = 0L;

    /** Instant de la dernière perte, ou {@code 0} si la manette n'a jamais été perdue. */
    private volatile long instantPerte = 0L;

    private long derniereVerificationPresence = 0L;

    /** L'inventaire des périphériques a déjà été journalisé pour l'épisode d'absence en cours. */
    private boolean inventaireJournalise = false;

    /** Nœuds evdev présents lors du dernier examen, pour ne réagir qu'aux apparitions. */
    private Set<String> noeudsConnus = Set.of();

    /**
     * Découvertes encore autorisées. Non nul au démarrage : la manette déjà branchée doit être
     * trouvée sans attendre qu'on la débranche et la rebranche.
     */
    private int tentativesOuverture = TENTATIVES_PAR_BRANCHEMENT;

    @Override
    public void start() {
        running = true;
        // Thread plateforme, et non virtuel : cette boucle fait des appels natifs en continu (qui
        // épinglent de toute façon leur porteur) et ferme des Arena partagées — autant lui donner
        // un thread bien à elle. Toute la vie de la manette (ouverture, scrutation, fermeture,
        // réouverture) s'y déroule : le plugin input4j alloue de la mémoire native dans une Arena,
        // la fermer pendant qu'un autre thread scrute serait une faute.
        pollThread = Thread.ofPlatform().daemon().name("GamepadPolling").start(this::boucle);
    }

    private void boucle() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                if (manette == null) {
                    guetterBranchement();
                    continue;
                }
                // Présence testée AVANT la scrutation : interroger un périphérique disparu ne
                // rapporte rien et fait journaliser une erreur à input4j — à 60 Hz, le flot noie
                // les journaux et remplit le disque du Jetson.
                if (!manettePresente()) {
                    signalerPerte();
                    fermerManette();
                    continue;
                }
                manette.poll();
                dernierPollReussi = System.currentTimeMillis();
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("Erreur de scrutation manette : {}", e.getMessage());
            }
        }
        fermerManette();
    }

    /**
     * Un tour de guet, manette absente : on ne tente une découverte que si un nœud vient
     * d'apparaître sous {@code /dev/input}, sinon on se contente d'attendre. Voir l'en-tête de
     * classe pour ce que coûte une découverte inutile.
     */
    private void guetterBranchement() throws InterruptedException {
        if (unNoeudEstApparu()) {
            tentativesOuverture = TENTATIVES_PAR_BRANCHEMENT;
        }
        if (tentativesOuverture > 0) {
            tentativesOuverture--;
            ouvrirManette();
        }
        if (manette == null) {
            Thread.sleep(INTERVALLE_RECONNEXION_MS);
        }
    }

    /**
     * Un nœud evdev est-il apparu depuis le dernier examen ? Seules les apparitions comptent : une
     * disparition ne peut pas amener une manette, et le débranchement est déjà traité ailleurs.
     * <p>
     * Le répertoire est relu à chaque tour, mais {@code Files.list} n'ouvre aucun périphérique :
     * c'est justement tout l'intérêt par rapport à une découverte input4j.
     */
    private boolean unNoeudEstApparu() {
        Set<String> noeuds;
        try (Stream<Path> contenu = Files.list(REPERTOIRE_ENTREES)) {
            noeuds = contenu.map(chemin -> chemin.getFileName().toString())
                    .filter(nom -> nom.startsWith("event"))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException | RuntimeException e) {
            // Pas de /dev/input lisible (hors Linux, ou droits) : rien à guetter. Les tentatives
            // initiales ont déjà eu lieu, il n'y a plus qu'à rester tranquille.
            noeuds = Set.of();
        }
        boolean apparition = !noeudsConnus.containsAll(noeuds);
        noeudsConnus = noeuds;
        return apparition;
    }

    /**
     * Cycle de découverte complet : initialise input4j, cherche une manette compatible et lui
     * branche les callbacks. Sans effet visible si rien n'est branché — c'est le cas courant tant
     * que l'utilisateur n'a pas allumé sa manette, et il ne doit produire aucun bruit dans les
     * journaux.
     */
    private void ouvrirManette() {
        try {
            plugin = InputDevices.init();
        } catch (Exception e) {
            logger.error("Impossible d'initialiser input4j (manette désactivée)", e);
            plugin = null;
            return;
        }
        if (plugin == null) {
            return;
        }

        // Tout ce qui suit doit refermer le plugin en cas d'échec : l'abandonner sans le fermer
        // laisserait derrière lui l'Arena native et un descripteur par périphérique du système.
        try {
            Optional<InputDevice> trouvee = trouverManette();
            if (trouvee.isEmpty()) {
                fermerPlugin();
                return;
            }

            boolean reconnexion = instantPerte != 0L;
            InputDevice ouverte = trouvee.get();
            LogitechControllerInput4j controller = new LogitechControllerInput4j();
            listeners.forEach(controller::addListener);
            controller.register(ouverte);

            noeudPeripherique = noeudEvdev(ouverte);
            derniereVerificationPresence = 0L;
            dernierPollReussi = System.currentTimeMillis();
            manette = ouverte;
            instantPerte = 0L;

            logger.info("Manette {} : {} (type {})", reconnexion ? "reconnectée" : "détectée",
                    ouverte.getDisplayName(), ouverte.getControllerType());
            ouverte.getBatteryInfo()
                    .filter(b -> b.isAvailable() && b.getPercentage() >= 0)
                    .ifPresent(b -> logger.info("Batterie manette : {}%", b.getPercentage()));
        } catch (RuntimeException e) {
            logger.error("Échec de l'ouverture de la manette", e);
            manette = null;
            noeudPeripherique = null;
            fermerPlugin();
        }
    }

    private void signalerPerte() {
        instantPerte = System.currentTimeMillis();
        logger.error("Manette débranchée ({}) — scrutation suspendue, réouverture automatique dès "
                + "qu'une manette sera de nouveau détectée.", noeudPeripherique);
    }

    /**
     * Referme la manette et le plugin. Indispensable avant toute réouverture : le descripteur de
     * fichier ouvert au démarrage reste définitivement invalide après un débranchement, et c'est
     * la fermeture du plugin qui libère l'Arena native.
     */
    private void fermerManette() {
        manette = null;
        noeudPeripherique = null;
        fermerPlugin();
    }

    private void fermerPlugin() {
        if (plugin == null) {
            return;
        }
        try {
            plugin.close();
        } catch (Exception e) {
            logger.warn("Erreur à la fermeture d'input4j : {}", e.getMessage());
        }
        plugin = null;
    }

    /**
     * Chemin du nœud evdev de la manette, ou {@code null} s'il n'est pas exploitable.
     * <p>
     * Le plugin Linux d'input4j construit l'{@link InputDevice} avec le chemin absolu du fichier de
     * périphérique comme identifiant ({@code new InputDevice(eventDeviceFile.getAbsolutePath(), …)}),
     * ce qui nous donne gratuitement de quoi vérifier sa présence. On ne le retient que s'il existe
     * à l'ouverture : ailleurs que sous Linux, {@code getID()} est tout autre chose, et il vaut
     * mieux ne rien vérifier que de déclarer la manette morte à tort.
     */
    private static Path noeudEvdev(InputDevice manette) {
        try {
            Path chemin = Path.of(manette.getID());
            if (Files.exists(chemin)) {
                return chemin;
            }
            logger.warn("Identifiant de manette non exploitable comme chemin ({}) : "
                    + "la disparition du périphérique ne sera pas détectée", manette.getID());
        } catch (RuntimeException e) {
            logger.warn("Identifiant de manette illisible comme chemin : {}", e.getMessage());
        }
        return null;
    }

    /**
     * La manette est-elle toujours branchée ? Réponse mise en cache entre deux vérifications
     * (voir {@link #PERIODE_VERIFICATION_PRESENCE_MS}). Toujours vrai si le nœud n'est pas suivi :
     * on retombe alors sur l'ancien comportement, où seul l'arrêt du thread se voit.
     */
    private boolean manettePresente() {
        if (noeudPeripherique == null) {
            return true;
        }
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereVerificationPresence < PERIODE_VERIFICATION_PRESENCE_MS) {
            return true;
        }
        derniereVerificationPresence = maintenant;
        return Files.exists(noeudPeripherique);
    }

    /** Horodatage de la dernière scrutation réussie, ou {@code 0} si aucune. */
    public long dernierPollReussi() {
        return dernierPollReussi;
    }

    /**
     * La manette est-elle censée répondre ? Vrai si elle est ouverte, ou si elle vient tout juste
     * d'être perdue.
     * <p>
     * Cette fenêtre après la perte est ce qui permet au watchdog de couper les moteurs quand
     * la manette disparaît <b>pendant</b> un mouvement. Passée cette fenêtre, l'absence devient un
     * fait acquis : l'organe est éteint et non en panne, et cesse de peser sur quoi que ce soit.
     */
    public boolean devraitRepondre() {
        if (manette != null) {
            return true;
        }
        return instantPerte != 0L && System.currentTimeMillis() - instantPerte < DELAI_ABSENCE_ACQUISE_MS;
    }

    public void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
        }
    }

    private Optional<InputDevice> trouverManette() {
        var devices = plugin.getAll();
        Optional<InputDevice> compatible = devices.stream().filter(this::estManetteCompatible).findFirst();
        // Inventaire journalisé une seule fois : c'est le cas où l'on a besoin de savoir comment le
        // noyau nomme la F710 en mode XInput. La boucle de reconnexion repassant ici chaque seconde,
        // le répéter noierait les journaux — le défaut même qu'on vient de corriger.
        if (compatible.isEmpty() && !inventaireJournalise) {
            inventaireJournalise = true;
            logger.warn("Aucune manette compatible détectée (input4j) — nouvelles tentatives en continu");
            devices.forEach(d -> logger.info("Périphérique d'entrée détecté : {} / {} (type {})",
                    d.getName(), d.getProductName(), d.getControllerType()));
        } else if (compatible.isPresent()) {
            inventaireJournalise = false;
        }
        return compatible;
    }

    private boolean estManetteCompatible(InputDevice device) {
        if (device.getControllerType() == ControllerType.XBOX) {
            return true;
        }
        String nom = (StringUtils.defaultString(device.getName()) + " " + StringUtils.defaultString(device.getProductName()))
                .toLowerCase(Locale.ROOT);
        return nom.contains("f710") || nom.contains("logitech") || nom.contains("x-box") || nom.contains("xbox") || nom.contains("360");
    }
}
