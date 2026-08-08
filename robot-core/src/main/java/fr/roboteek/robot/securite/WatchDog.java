package fr.roboteek.robot.securite;

import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.systemenerveux.event.SanteOrganesEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Watchdog : coupe les moteurs quand un organe du mouvement cesse de donner signe de vie.
 * <p>
 * <b>Le scénario qui justifie tout.</b> Le robot roule et ce qui devait l'arrêter meurt. Les ordres
 * de mouvement continus ({@code AVANCER}, {@code TOURNER_GAUCHE}) n'expirent jamais d'eux-mêmes :
 * les chenilles tournent jusqu'à recevoir un {@code STOPPER}. Si le thread de scrutation de la
 * manette meurt pendant une marche avant, plus personne n'enverra jamais ce {@code STOPPER} — le
 * robot part tout droit, et le bouton d'arrêt d'urgence de la manette est mort avec le reste.
 * <p>
 * <b>Trois garde-fous, parce qu'un watchdog qui se déclenche à tort finit débranché :</b>
 * <ul>
 *   <li><b>périmètre étroit</b> : seuls les organes qui provoquent ou exécutent un mouvement
 *   ({@link OrganeSurveille#provoqueUnMouvement()}) peuvent déclencher ; un capteur muet est un
 *   incident à journaliser, pas une urgence ;</li>
 *   <li><b>délai large</b> : plusieurs dizaines de fois la période de battement de l'organe le plus
 *   lent, pour qu'une pause du ramasse-miettes sur le Nano ne passe jamais pour une panne ;</li>
 *   <li><b>rien ne bouge, rien ne se passe</b> : un thread mort sur un robot à l'arrêt donne une
 *   ligne de journal. Couper les moteurs d'un robot immobile n'apporte aucune sécurité et coûte un
 *   réarmement à l'utilisateur, qui apprendrait vite à ignorer l'alerte.</li>
 * </ul>
 * <p>
 * <b>Son propre thread, et pas l'ordonnanceur Spring.</b> Un surveillant ne partage pas son fil
 * d'exécution avec ce qu'il surveille : les battements du cou et des yeux sont émis depuis des
 * tâches {@code @Scheduled}, et si cet ordonnanceur venait à se bloquer, un watchdog qui y
 * tournerait aussi se tairait en même temps que ses témoins — panne totale, silence total. Thread
 * de plateforme et non virtuel, pour la même raison : ne dépendre de rien d'autre.
 * <p>
 * Le déclenchement passe par {@link ArretUrgence#declencher(String)} : même verrou, même
 * propagation, même réarmement que le bouton B de la manette ou celui du HUD.
 */
@Component
public class WatchDog implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(WatchDog.class);

    /**
     * Période de vérification. Fine devant le délai de silence : ce qui décide du temps de réaction,
     * c'est le délai, pas la cadence à laquelle on le teste.
     */
    private static final long PERIODE_MS = 500;

    /**
     * Nombre de tours entre deux diffusions de l'état vital vers l'interface. Le relevé est déjà
     * calculé à chaque tour ; il n'y a aucune raison d'en envoyer deux par seconde à une tablette.
     */
    private static final int TOURS_PAR_DIFFUSION = 2;

    private final RegistreSante registreSante;
    private final ArretUrgence arretUrgence;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RobotConfig robotConfig = robotConfig();

    /** Organes déjà signalés muets, pour ne pas répéter la même ligne toutes les 500 ms. */
    private final Set<String> muetsDejaSignales = new HashSet<>();

    /** Compteur de tours, pour espacer les diffusions vers l'interface. */
    private int tours = 0;

    private volatile boolean running = false;
    private Thread thread;

    public WatchDog(RegistreSante registreSante, ArretUrgence arretUrgence,
                        ApplicationEventPublisher applicationEventPublisher) {
        this.registreSante = registreSante;
        this.arretUrgence = arretUrgence;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Un tour de surveillance : relève l'état de tous les organes, le diffuse vers l'interface, et
     * décide s'il faut couper. Public pour être testable sans thread ni contexte Spring.
     */
    public void verifier() {
        long delaiMillis = (long) (robotConfig.watchDogSilenceSecondes() * 1000);
        List<SanteOrgane> releve = registreSante.releve(delaiMillis);

        // La diffusion a lieu même watchdog désactivé : couper les moteurs est une chose,
        // renseigner l'utilisateur en est une autre, et il n'y a aucune raison d'aveugler les
        // pastilles parce qu'on a désarmé le déclenchement.
        if (++tours % TOURS_PAR_DIFFUSION == 0) {
            applicationEventPublisher.publishEvent(new SanteOrganesEvent(releve));
        }

        if (!robotConfig.watchDogActive()) {
            // Désarmé, on oublie les silences déjà signalés. Sans ça, un organe muet au moment du
            // désarmement resterait marqué « déjà signalé » pendant toute la coupure : au
            // réarmement, le même organe toujours muet ne donnerait aucune ligne, et le watchdog
            // paraîtrait ne rien faire alors qu'il surveille.
            muetsDejaSignales.clear();
            return;
        }

        List<SanteOrgane> muets = releve.stream()
                .filter(SanteOrgane::surveille)
                .filter(sante -> sante.etat() == EtatSante.MUET)
                .toList();

        if (muets.isEmpty()) {
            // Tout est rentré dans l'ordre : le prochain silence sera de nouveau signalé.
            muetsDejaSignales.clear();
            return;
        }

        String description = muets.stream().map(SanteOrgane::libelle).collect(Collectors.joining(", "));

        if (!registreSante.quelqueChoseBouge()) {
            signalerUneFois(muets, description, delaiMillis);
            return;
        }

        // declencher() est sans effet si l'arrêt d'urgence est déjà actif : pas de risque de
        // le redéclencher en boucle tant que les organes restent muets.
        if (arretUrgence.declencher("watchdog — " + description)) {
            logger.error("WATCHDOG : {} sans signe de vie depuis plus de {} ms alors que le robot bouge — arrêt d'urgence",
                    description, delaiMillis);
        }
    }

    /**
     * Journalise un silence sans conséquence (robot immobile), une seule fois par épisode.
     */
    private void signalerUneFois(List<SanteOrgane> muets, String description, long delaiMillis) {
        if (enregistrerNouveauxMuets(muets)) {
            logger.warn("WATCHDOG : {} sans signe de vie depuis plus de {} ms — rien ne bouge, moteurs laissés en l'état",
                    description, delaiMillis);
        }
    }

    /**
     * Mémorise les organes muets et indique si l'un d'eux n'avait pas encore été signalé.
     * <p>
     * Parcours <b>sans court-circuit</b>, volontairement : un {@code anyMatch(muetsDejaSignales::add)}
     * s'arrête au premier ajout réussi et laisse les organes suivants hors du registre. Au tour
     * suivant, c'est l'un d'eux qui paraît nouveau, et la ligne censée n'apparaître qu'une fois par
     * épisode est réémise autant de fois qu'il y a d'organes muets. Constaté sur le robot le
     * 2026-08-08 : deux lignes identiques à 500 ms d'intervalle pour « Cou, Yeux ».
     */
    boolean enregistrerNouveauxMuets(List<SanteOrgane> muets) {
        boolean nouveau = false;
        for (SanteOrgane muet : muets) {
            nouveau |= muetsDejaSignales.add(muet.id());
        }
        return nouveau;
    }

    @Override
    public void start() {
        running = true;
        thread = new Thread(this::boucle, "WatchDog");
        thread.setDaemon(true);
        thread.start();
        logger.info("Watchdog démarré (silence toléré : {} s)", robotConfig.watchDogSilenceSecondes());
    }

    private void boucle() {
        while (running) {
            try {
                verifier();
            } catch (RuntimeException e) {
                // Un watchdog ne meurt pas : on journalise et on continue au tour suivant.
                logger.error("Watchdog : tour de surveillance en échec", e);
            }
            try {
                Thread.sleep(PERIODE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
        logger.info("Watchdog arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Phase infrastructure : démarré avant tous les organes, arrêté après eux. Il surveille donc
     * pendant tout l'arrêt, où les organes passent hors service les uns après les autres — ce qui
     * ne déclenche rien, un organe hors service n'étant jamais jugé muet.
     */
    @Override
    public int getPhase() {
        return RobotLifecyclePhases.INFRASTRUCTURE;
    }
}
