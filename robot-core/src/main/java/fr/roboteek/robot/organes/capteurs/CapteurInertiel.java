package fr.roboteek.robot.organes.capteurs;

import com.phidget22.AttachEvent;
import com.phidget22.DetachEvent;
import com.phidget22.PhidgetException;
import com.phidget22.Spatial;
import com.phidget22.SpatialAlgorithm;
import com.phidget22.SpatialAlgorithmDataEvent;
import com.phidget22.SpatialSpatialDataEvent;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.TelemetrieOrganeEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;
import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Centrale inertielle du robot : le Phidget Spatial MOT1102 (accéléromètre, gyroscope et
 * magnétomètre trois axes), branché sur le hub VINT.
 * <p>
 * L'organe ne lit pas les trois capteurs bruts : il laisse la carte fusionner leurs mesures
 * (algorithme IMU ou AHRS embarqué) et n'en retient que le quaternion, converti en
 * {@link Attitude}. C'est ce qui répond à la question qui intéresse le robot — « suis-je penché,
 * et de combien ? » — sans réécrire un filtre de fusion côté Java.
 * <p>
 * Deux exceptions, tirées des mesures brutes : la {@link Secousse}, qui dit ce que l'attitude ne
 * dit pas (un choc ne penche pas le robot), et l'intensité du champ magnétique, qui ne sert pas au
 * robot mais au réglage, pour décider si le magnétomètre peut corriger le cap (voir
 * {@code phidgets.spatial.algorithm}).
 * <p>
 * Deux rythmes, comme pour le cou : la carte pousse ses mesures sur le thread Phidget, à la
 * cadence {@code phidgets.spatial.data.interval} ; la télémétrie part sur le bus à un rythme
 * plus lâche, et seulement quand l'attitude a bougé — l'interface n'a que faire de vingt angles
 * identiques par seconde.
 */
@Component
public class CapteurInertiel extends AbstractOrgane implements SmartLifecycle, OrganeSurveille {

    private static final Logger logger = LoggerFactory.getLogger(CapteurInertiel.class);

    /** Délai laissé au hub pour présenter le canal avant de conclure qu'il est absent. */
    private static final int DELAI_OUVERTURE_MS = 5000;

    /** Période de diffusion de la télémétrie, indépendante de l'échantillonnage. */
    private static final long PERIODE_TELEMETRIE_MS = 100;

    /** Variation en degrés en deçà de laquelle l'attitude est jugée inchangée (bruit de mesure). */
    private static final double SEUIL_VARIATION_DEGRES = 0.5;

    /**
     * Même chose pour le champ magnétique, en gauss : 1 % du champ terrestre (environ 0,5 G). La
     * résolution du MOT1102 est de 1,5 mG, mais la diffuser à ce grain ferait de la télémétrie un
     * flux continu de bruit.
     */
    private static final double SEUIL_VARIATION_GAUSS = 0.005;

    /** Même chose pour la secousse, en g : au-dessus du bruit de l'accéléromètre au repos. */
    private static final double SEUIL_VARIATION_SECOUSSE_G = 0.02;

    /**
     * Au-delà, la valeur n'est pas une mesure : le capteur plafonne à 8 G, et Phidget22 signale
     * une donnée indisponible par une valeur démesurée plutôt que par une exception.
     */
    private static final double CHAMP_MAX_PLAUSIBLE_GAUSS = 100;

    private final PhidgetsConfig phidgetsConfig = phidgetsConfig();

    private Spatial spatial;

    /** Dernière attitude calculée, {@code null} tant que la carte n'a rien envoyé. */
    private volatile Attitude attitude;

    /** Intensité du dernier champ magnétique mesuré en gauss, {@code null} tant qu'il n'y en a pas. */
    private volatile Double champMagnetique;

    private final Secousse secousse = new Secousse();

    /** Secousse du dernier intervalle de télémétrie en g, {@code null} tant qu'il n'y en a pas. */
    private volatile Double derniereSecousse;

    private Attitude derniereAttitudeDiffusee;

    private Double dernierChampDiffuse;

    private Double derniereSecousseDiffusee;

    private volatile boolean running = false;

    @Override
    public void initialiser() {
        try {
            spatial = new Spatial();
            spatial.setDeviceSerialNumber(phidgetsConfig.hubSerialNumber());
            spatial.setHubPort(phidgetsConfig.spatialPort());
            spatial.addAttachListener(this::surAttachement);
            spatial.addDetachListener(this::surDetachement);
            spatial.addAlgorithmDataListener(this::surDonneesAlgorithme);
            spatial.addSpatialDataListener(this::surDonneesBrutes);
            spatial.open(DELAI_OUVERTURE_MS);
        } catch (PhidgetException e) {
            logger.warn("Centrale inertielle introuvable sur le port {} du hub {} : {}",
                    phidgetsConfig.spatialPort(), phidgetsConfig.hubSerialNumber(), e.getMessage());
            fermer();
        }
    }

    /**
     * Réglages appliqués à chaque attachement, et non une seule fois à l'ouverture : un câble VINT
     * débranché puis rebranché rend une carte revenue à ses valeurs d'usine.
     * <p>
     * Le gyroscope est remis à zéro ici, ce qui suppose le robot immobile pendant environ deux
     * secondes. C'est le cas au démarrage — les capteurs démarrent avant les moteurs — mais pas
     * forcément après un rebranchement à chaud : un léger biais de cap est alors à prévoir.
     */
    private void surAttachement(AttachEvent evenement) {
        try {
            spatial.setDataInterval(phidgetsConfig.spatialDataInterval());
            spatial.setAlgorithm(algorithmeConfigure());
            spatial.zeroGyro();
            logger.info("Centrale inertielle attachée (port {}, {} ms, {})", spatial.getHubPort(),
                    spatial.getDataInterval(), spatial.getAlgorithm());
        } catch (PhidgetException e) {
            logger.error("Réglage de la centrale inertielle échoué : {}", e.getMessage());
        }
    }

    private void surDetachement(DetachEvent evenement) {
        logger.warn("Centrale inertielle détachée");
    }

    /** Le seul travail réel de l'organe, donc le seul endroit où il donne signe de vie. */
    private void surDonneesAlgorithme(SpatialAlgorithmDataEvent evenement) {
        attitude = Attitude.depuisQuaternion(evenement.getQuaternion());
        battement();
    }

    /**
     * Les données brutes arrivent quel que soit l'algorithme : en IMU aussi, ce qui permet de
     * juger le magnétomètre avant de lui confier le cap.
     */
    private void surDonneesBrutes(SpatialSpatialDataEvent evenement) {
        secousse.noter(evenement.getAcceleration());
        double[] champ = evenement.getMagneticField();
        double intensite = Math.sqrt(champ[0] * champ[0] + champ[1] * champ[1] + champ[2] * champ[2]);
        if (Double.isFinite(intensite) && intensite < CHAMP_MAX_PLAUSIBLE_GAUSS) {
            champMagnetique = intensite;
        }
    }

    /**
     * {@code NONE} est écarté : sans fusion, la carte n'envoie aucun quaternion, et l'organe se
     * tairait tout en restant attaché — le pire des deux mondes pour la surveillance.
     */
    private SpatialAlgorithm algorithmeConfigure() {
        String configure = phidgetsConfig.spatialAlgorithm();
        try {
            SpatialAlgorithm algorithme = SpatialAlgorithm.valueOf(configure.trim().toUpperCase());
            if (algorithme != SpatialAlgorithm.NONE) {
                return algorithme;
            }
        } catch (IllegalArgumentException e) {
            // Repli ci-dessous.
        }
        logger.warn("Algorithme de centrale inertielle « {} » refusé, repli sur IMU", configure);
        return SpatialAlgorithm.IMU;
    }

    /**
     * Toutes les mesures partent ensemble dès que l'une a bougé : le client reçoit toujours un
     * relevé complet, et non une attitude d'un instant mêlée au champ d'un autre.
     */
    @Scheduled(fixedRate = PERIODE_TELEMETRIE_MS)
    public void diffuserTelemetrie() {
        if (!running) {
            return;
        }
        // Relevée à chaque tour, publiée ou non : c'est ce relevé qui borne l'intervalle sur lequel
        // court le maximum.
        Double releve = secousse.relever();
        if (releve != null) {
            derniereSecousse = releve;
        }
        Attitude attitudeCourante = attitude;
        Double champCourant = champMagnetique;
        Double secousseCourante = derniereSecousse;
        boolean attitudeAVarie = attitudeCourante != null && aVarie(attitudeCourante, derniereAttitudeDiffusee);
        boolean champAVarie = aVarie(champCourant, dernierChampDiffuse, SEUIL_VARIATION_GAUSS);
        boolean secousseAVarie = aVarie(secousseCourante, derniereSecousseDiffusee, SEUIL_VARIATION_SECOUSSE_G);
        if (!attitudeAVarie && !champAVarie && !secousseAVarie) {
            return;
        }
        derniereAttitudeDiffusee = attitudeCourante;
        dernierChampDiffuse = champCourant;
        derniereSecousseDiffusee = secousseCourante;
        applicationEventPublisher.publishEvent(new TelemetrieOrganeEvent(idOrgane(),
                mesures(attitudeCourante, champCourant, secousseCourante)));
    }

    private static boolean aVarie(Double courante, Double precedente, double seuil) {
        return courante != null && (precedente == null || Math.abs(courante - precedente) >= seuil);
    }

    private static Map<String, Double> mesures(Attitude attitude, Double champ, Double secousse) {
        Map<String, Double> mesures = new LinkedHashMap<>();
        if (attitude != null) {
            mesures.put("roulis", attitude.roulis());
            mesures.put("tangage", attitude.tangage());
            mesures.put("cap", attitude.lacet());
        }
        if (champ != null) {
            mesures.put("champMagnetique", champ);
        }
        if (secousse != null) {
            mesures.put("secousse", secousse);
        }
        return mesures;
    }

    private static boolean aVarie(Attitude courante, Attitude precedente) {
        return precedente == null
                || Math.abs(courante.roulis() - precedente.roulis()) >= SEUIL_VARIATION_DEGRES
                || Math.abs(courante.tangage() - precedente.tangage()) >= SEUIL_VARIATION_DEGRES
                || Math.abs(courante.lacet() - precedente.lacet()) >= SEUIL_VARIATION_DEGRES;
    }

    /** Dernière attitude connue, {@code null} si la centrale n'a encore rien mesuré. */
    public Attitude getAttitude() {
        return attitude;
    }

    /** Intensité du champ magnétique en gauss, {@code null} si la centrale n'a encore rien mesuré. */
    public Double getChampMagnetique() {
        return champMagnetique;
    }

    /** Plus forte secousse du dernier intervalle de télémétrie en g, {@code null} si inconnue. */
    public Double getSecousse() {
        return derniereSecousse;
    }

    @Override
    public void arreter() {
        fermer();
    }

    private void fermer() {
        if (spatial == null) {
            return;
        }
        try {
            spatial.close();
        } catch (PhidgetException e) {
            logger.warn("Fermeture de la centrale inertielle échouée : {}", e.getMessage());
        }
        spatial = null;
    }

    @Override
    public void start() {
        if (!robotConfig().inertielEnabled()) {
            logger.info("Centrale inertielle désactivée (robot.capteurs.inertiel.enabled=false)");
            return;
        }
        initialiser();
        if (spatial == null) {
            // Absente du hub : l'organe reste inerte, comme la vision privée de webcam.
            logger.warn("CapteurInertiel non démarré (centrale absente)");
            return;
        }
        running = true;
        logger.info("CapteurInertiel démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("CapteurInertiel arrêté");
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
    // La centrale ne commande rien : son silence ne coupe pas les moteurs, il allume une pastille.

    @Override
    public String idOrgane() {
        return "inertiel";
    }

    @Override
    public String libelleOrgane() {
        return "Centrale inertielle";
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
