package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.TelemetrieOrganeEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.nativefree.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Capteur « matériel » : relevé des métriques de la machine hôte (CPU, mémoire, température,
 * disque) via <a href="https://github.com/oshi/oshi">OSHI</a> (variante FFM, sans natifs à
 * embarquer — cf. {@code input4j} pour la manette, même logique de migration). Point d'entrée
 * {@code oshi.nativefree.SystemInfo} (et non {@code oshi.SystemInfo}, classe de la variante JNA
 * classique) : c'est le point d'entrée propre à {@code oshi-core-ffm}.
 * <p>
 * Bean {@link SmartLifecycle} en phase
 * {@link RobotLifecyclePhases#CAPTEURS}, thread de scrutation ({@link AbstractOrganeWithThread}),
 * publication d'un {@link TelemetrieOrganeEvent} sur le bus applicatif à chaque relevé — c'est
 * {@code WebsocketBroadcaster} (générique, déjà en place) qui le diffuse vers les clients sur
 * {@code /events/telemetrie-organe}, sans code de diffusion dédié.
 * <p>
 * Identifiants de mesure identiques à ceux exposés par {@code OrganeController} (ressource REST
 * {@code /api/organes}) : les getters publics servent le relevé initial (GET), le thread sert la
 * télémétrie continue (évènement).
 */
@Component
public class CapteurMateriel extends AbstractOrganeWithThread implements SmartLifecycle, OrganeSurveille {

    private static final Logger logger = LoggerFactory.getLogger(CapteurMateriel.class);

    /** Période de scrutation : ces métriques évoluent lentement, pas besoin de fréquence fine. */
    private static final long PERIODE_MS = 2000;

    private final SystemInfo systemInfo = new SystemInfo();
    private final CentralProcessor processeur;
    private final GlobalMemory memoire;
    private final Sensors capteurs;
    private final FileSystem systemeFichiers;
    private final OperatingSystem systemeExploitation;

    /** Octets dans un gigaoctet, au sens des constructeurs comme des explorateurs de fichiers. */
    private static final double OCTETS_PAR_GO = 1024d * 1024 * 1024;

    private long[] derniersTicksCpu;

    private volatile double chargeCpuPourcent;
    private volatile double memoireUtiliseePourcent;
    private volatile Double temperatureCpu;
    private volatile double disqueUtilisePourcent;
    private volatile double disqueLibreGo;
    private volatile double disqueTotalGo;
    private volatile double uptimeSecondes;

    private volatile boolean running = false;

    public CapteurMateriel() {
        super("CapteurMateriel");
        HardwareAbstractionLayer materiel = systemInfo.getHardware();
        this.processeur = materiel.getProcessor();
        this.memoire = materiel.getMemory();
        this.capteurs = materiel.getSensors();
        this.systemeExploitation = systemInfo.getOperatingSystem();
        this.systemeFichiers = systemeExploitation.getFileSystem();
        this.derniersTicksCpu = processeur.getSystemCpuLoadTicks();
    }

    @Override
    public void initialiser() {
        // Premier relevé synchrone : valeurs exploitables immédiatement pour GET /api/organes
        // sans attendre le premier tour de la boucle de scrutation.
        rafraichir();
        logger.info("CapteurMateriel : fin initialisation");
    }

    @Override
    public void loop() {
        while (running) {
            rafraichir();
            battement();
            applicationEventPublisher.publishEvent(new TelemetrieOrganeEvent(idOrgane(), mesuresCourantes()));
            try {
                Thread.sleep(PERIODE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private Map<String, Double> mesuresCourantes() {
        Map<String, Double> mesures = new LinkedHashMap<>();
        mesures.put("cpuCharge", chargeCpuPourcent);
        mesures.put("memoire", memoireUtiliseePourcent);
        if (temperatureCpu != null) {
            mesures.put("temperatureCpu", temperatureCpu);
        }
        mesures.put("disque", disqueUtilisePourcent);
        mesures.put("disqueLibre", disqueLibreGo);
        // L'uptime ne figure pas dans /api/organes : une valeur qui ne fait que croître n'a pas
        // d'échelle, donc pas de jauge. Il voyage tout de même avec la télémétrie, l'interface
        // l'affiche en toutes lettres.
        mesures.put("uptimeSecondes", uptimeSecondes);
        return mesures;
    }

    /**
     * Recalcule toutes les métriques. Chaque métrique est isolée dans son propre {@code try} :
     * un capteur illisible sur une machine donnée ne doit pas faire perdre les autres métriques de
     * ce relevé, ni interrompre les relevés suivants. La température était l'exemple attendu — elle
     * remonte en fait très bien depuis le conteneur du Jetson (42 °C mesurés), mais
     * rien ne le garantit sur une autre machine, d'où le repli sur {@code null} plutôt qu'une
     * valeur inventée.
     */
    private void rafraichir() {
        try {
            long[] ticksActuels = processeur.getSystemCpuLoadTicks();
            chargeCpuPourcent = processeur.getSystemCpuLoadBetweenTicks(derniersTicksCpu) * 100;
            derniersTicksCpu = ticksActuels;
        } catch (RuntimeException e) {
            logger.warn("Lecture de la charge CPU échouée : {}", e.getMessage());
        }

        try {
            long total = memoire.getTotal();
            memoireUtiliseePourcent = total == 0 ? 0 : (double) (total - memoire.getAvailable()) / total * 100;
        } catch (RuntimeException e) {
            logger.warn("Lecture de la mémoire échouée : {}", e.getMessage());
        }

        try {
            double temperature = capteurs.getCpuTemperature();
            temperatureCpu = temperature > 0 ? temperature : null;
        } catch (RuntimeException e) {
            temperatureCpu = null;
            logger.warn("Lecture de la température CPU échouée : {}", e.getMessage());
        }

        try {
            releverLeDisque();
        } catch (RuntimeException e) {
            logger.warn("Lecture de l'utilisation disque échouée : {}", e.getMessage());
        }

        try {
            uptimeSecondes = systemeExploitation.getSystemUptime();
        } catch (RuntimeException e) {
            logger.warn("Lecture de l'uptime échouée : {}", e.getMessage());
        }
    }

    /**
     * Relève le disque qui compte : celui où vit {@code ROBOT_HOME}, à défaut la racine, à défaut
     * le premier système de fichiers venu.
     * <p>
     * Le robot tourne en conteneur, et {@code /} y désigne l'overlay de l'image, pas le support où
     * s'entassent réellement les modèles Vosk et Piper, la base SQLite et les vignettes —
     * {@code ROBOT_HOME} est un volume monté depuis l'hôte. Regarder la racine reviendrait à
     * surveiller le mauvais disque, et à annoncer de la place quand il n'y en a plus là où on
     * écrit.
     * <p>
     * Espace <b>utilisable</b> et non « libre » : les deux diffèrent de la réserve du superutilisateur
     * (5 % sur ext4 par défaut, mesuré 7,5 Go d'écart sur un disque de 146 Go). C'est l'utilisable
     * qu'on peut réellement remplir.
     */
    private void releverLeDisque() {
        List<OSFileStore> stores = systemeFichiers.getFileStores(true);
        OSFileStore fs = magasinDe(stores, System.getenv(Constantes.ENV_VAR_ROBOT_HOME))
                .or(() -> stores.stream().filter(store -> "/".equals(store.getMount())).findFirst())
                .or(() -> stores.stream().findFirst())
                .orElse(null);
        if (fs == null || fs.getTotalSpace() == 0) {
            return;
        }
        disqueTotalGo = fs.getTotalSpace() / OCTETS_PAR_GO;
        disqueLibreGo = fs.getUsableSpace() / OCTETS_PAR_GO;
        disqueUtilisePourcent = (double) (fs.getTotalSpace() - fs.getUsableSpace()) / fs.getTotalSpace() * 100;
    }

    /**
     * Le magasin qui porte ce chemin : le point de montage le plus long dont il descend. Le plus
     * long, parce que tout chemin descend de {@code /} — sans ce critère, un volume dédié serait
     * systématiquement confondu avec la racine.
     */
    private static Optional<OSFileStore> magasinDe(List<OSFileStore> stores, String chemin) {
        if (chemin == null || chemin.isBlank()) {
            return Optional.empty();
        }
        String cible = chemin.endsWith(File.separator) ? chemin : chemin + File.separator;
        return stores.stream()
                .filter(store -> {
                    String mount = store.getMount().endsWith(File.separator)
                            ? store.getMount() : store.getMount() + File.separator;
                    return cible.startsWith(mount);
                })
                .max(Comparator.comparingInt(store -> store.getMount().length()));
    }

    public double getChargeCpuPourcent() {
        return chargeCpuPourcent;
    }

    public double getMemoireUtiliseePourcent() {
        return memoireUtiliseePourcent;
    }

    /** Température CPU en °C, ou {@code null} si le capteur n'est pas exposé par l'hôte. */
    public Double getTemperatureCpu() {
        return temperatureCpu;
    }

    public double getDisqueUtilisePourcent() {
        return disqueUtilisePourcent;
    }

    /** Espace réellement remplissable, en Go, là où le robot écrit. */
    public double getDisqueLibreGo() {
        return disqueLibreGo;
    }

    /** Taille totale du même disque, en Go : c'est l'échelle de l'espace libre. */
    public double getDisqueTotalGo() {
        return disqueTotalGo;
    }

    /** Depuis combien de temps la machine tourne, en secondes. */
    public double getUptimeSecondes() {
        return uptimeSecondes;
    }

    @Override
    public void start() {
        initialiser();
        running = true;
        super.start();
        logger.info("CapteurMateriel démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("CapteurMateriel arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.CAPTEURS;
    }

    // --- Surveillance : affichage seulement ---
    // Ce capteur ne touche à aucun moteur : provoqueUnMouvement() reste à false, son silence ne
    // peut donc pas couper les moteurs. Il ne fait qu'alimenter les pastilles d'état de l'interface.

    @Override
    public String idOrgane() {
        return "materiel";
    }

    @Override
    public String libelleOrgane() {
        return "Matériel";
    }

    @Override
    public NatureOrgane nature() {
        return NatureOrgane.CAPTEUR;
    }

    @Override
    public boolean enService() {
        return running;
    }
}
