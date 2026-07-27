package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.event.TelemetrieOrganeEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.nativefree.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Capteur « matériel » : relevé des métriques de la machine hôte (CPU, mémoire, température,
 * disque) via <a href="https://github.com/oshi/oshi">OSHI</a> (variante FFM, sans natifs à
 * embarquer — cf. {@code input4j} pour la manette, même logique de migration). Point d'entrée
 * {@code oshi.nativefree.SystemInfo} (et non {@code oshi.SystemInfo}, classe de la variante JNA
 * classique) : c'est le point d'entrée propre à {@code oshi-core-ffm}.
 * <p>
 * Migré au même moule que les autres organes : bean Spring {@link SmartLifecycle} en phase
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
public class CapteurMateriel extends AbstractOrganeWithThread implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(CapteurMateriel.class);

    /** Période de scrutation : ces métriques évoluent lentement, pas besoin de fréquence fine. */
    private static final long PERIODE_MS = 2000;

    private final SystemInfo systemInfo = new SystemInfo();
    private final CentralProcessor processeur;
    private final GlobalMemory memoire;
    private final Sensors capteurs;
    private final FileSystem systemeFichiers;

    private long[] derniersTicksCpu;

    private volatile double chargeCpuPourcent;
    private volatile double memoireUtiliseePourcent;
    private volatile Double temperatureCpu;
    private volatile double disqueUtilisePourcent;

    private volatile boolean running = false;

    public CapteurMateriel() {
        super("CapteurMateriel");
        HardwareAbstractionLayer materiel = systemInfo.getHardware();
        this.processeur = materiel.getProcessor();
        this.memoire = materiel.getMemory();
        this.capteurs = materiel.getSensors();
        this.systemeFichiers = systemInfo.getOperatingSystem().getFileSystem();
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
            applicationEventPublisher.publishEvent(new TelemetrieOrganeEvent("materiel", mesuresCourantes()));
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
        return mesures;
    }

    /**
     * Recalcule toutes les métriques. Chaque métrique est isolée dans son propre {@code try} :
     * un capteur illisible dans un environnement donné (ex. température CPU indisponible dans le
     * conteneur Docker du robot) ne doit pas faire perdre les autres métriques de ce relevé, ni
     * interrompre les relevés suivants.
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
            disqueUtilisePourcent = calculerUtilisationDisqueRacine();
        } catch (RuntimeException e) {
            logger.warn("Lecture de l'utilisation disque échouée : {}", e.getMessage());
        }
    }

    /** Utilisation (%) du point de montage racine, ou du premier système de fichiers à défaut. */
    private double calculerUtilisationDisqueRacine() {
        OSFileStore fs = systemeFichiers.getFileStores(true).stream()
                .filter(store -> "/".equals(store.getMount()))
                .findFirst()
                .or(() -> systemeFichiers.getFileStores(true).stream().findFirst())
                .orElse(null);
        if (fs == null || fs.getTotalSpace() == 0) {
            return 0;
        }
        return (double) (fs.getTotalSpace() - fs.getUsableSpace()) / fs.getTotalSpace() * 100;
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
}
