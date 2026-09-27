package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.SpeechProviderConfig;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.voix.DecoupageEnPhrases;
import fr.roboteek.robot.organes.actionneurs.voix.Prononciation;
import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;
import fr.roboteek.robot.organes.actionneurs.voix.Voix;
import fr.roboteek.robot.organes.actionneurs.voix.VoixDuRobot;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.GoogleSpeechSynthesizerService;
import fr.roboteek.robot.services.providers.piper.speech.synthesizer.PiperSpeechSynthesizerService;
import fr.roboteek.robot.services.synthesizer.SpeechSynthesizerService;
import fr.roboteek.robot.systemenerveux.event.EssaiDeVoixEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleTermineeEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static fr.roboteek.robot.configuration.Configurations.googleSpeechSynthesisConfig;

/**
 * Organe permettant de synthétiser un texte (fournisseur cloud ou local, cf.
 * {@link SpeechProviderConfig}) et en appliquant des effets avec SOX.
 * <p>
 * Cycle de vie géré par {@link SmartLifecycle}, évènements reçus via {@link EventListener}.
 */
@Component
public class OrganeParole extends AbstractOrgane implements SmartLifecycle {

    private final GoogleSpeechSynthesisConfig googleConfig;
    private final VoixDuRobot voix;
    /** Pour ne pas parler par-dessus un son : la carte son n'en accepte qu'un à la fois. */
    private final SoundPlayer lecteurSon;
    private SpeechSynthesizerService speechSynthesizerService;
    /** Le même que {@link #speechSynthesizerService} quand c'est Piper : lui seul change de modèle. */
    private PiperSpeechSynthesizerService piper;
    private String fichierSyntheseVocale;

    /**
     * Vrai quand la voix se colore avec les réglages de l'appli (voir {@link VoixDuRobot}) : c'est le
     * cas de Piper. La voix Google garde son script, écrit pour elle.
     */
    private volatile boolean voixReglable;

    /**
     * Une seule phrase à la fois. Les phrases arrivent par le bus sur plusieurs fils ; sans ce
     * verrou, deux phrases rapprochées lançaient deux {@code play}, la carte son n'en acceptait qu'un
     * et la seconde se perdait sans un mot. Il couvre aussi la pause et la reprise de l'écoute : la
     * fin d'une phrase relançait sinon l'écoute pendant que la suivante parlait encore.
     * <p>
     * Une phrase attend son tour ; un essai de voix, lui, est ignoré si le robot parle déjà (voir
     * {@link #handleEssaiDeVoixEvent}).
     */
    private final ReentrantLock bouche = new ReentrantLock();

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(OrganeParole.class);

    /** Flag de démarrage de l'organe (cycle de vie Spring). */
    private volatile boolean running = false;

    public OrganeParole(VoixDuRobot voix, SoundPlayer lecteurSon) {
        super();
        this.voix = voix;
        this.lecteurSon = lecteurSon;
        googleConfig = googleSpeechSynthesisConfig();
    }

    /** La voix se règle-t-elle depuis l'appli, avec le fournisseur qui a démarré. */
    public boolean voixReglable() {
        return voixReglable;
    }

    /**
     * Lit un texte.
     *
     * @param texte le texte à dire
     */
    public void lire(String texte) {
        lire(texte, voix.voix(), true);
    }

    /**
     * @param annoncerLaFin faux pour une phrase d'essai : la présentation compte les fins de phrase
     *                      pour savoir où elle en est, une phrase d'essai dite pendant qu'elle se
     *                      déroule lui ferait sauter une étape
     */
    private void lire(String texte, Voix voixDeLaPhrase, boolean annoncerLaFin) {
        if (texte == null || texte.isEmpty()) {
            return;
        }
        if (annoncerLaFin) {
            bouche.lock();
        } else if (!bouche.tryLock()) {
            // Un essai qui attendrait son tour garderait un des quatre fils du bus pendant toute la
            // phrase en cours : dix clics sur « écouter » bloqueraient les sons et la conversation.
            // L'essai suivant, une fois le robot silencieux, dira la même chose.
            logger.info("Essai de voix ignoré : le robot parle déjà");
            return;
        }
        try {
            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(CONTROLE.METTRE_EN_PAUSE);
            applicationEventPublisher.publishEvent(eventPause);
            try {
                // Sous le verrou : la voix de base ne change qu'entre deux phrases. Une phrase normale
                // dite après un essai repasse ainsi d'elle-même au modèle adopté.
                if (piper != null) {
                    piper.utiliser(voixDeLaPhrase.modele(), voixDeLaPhrase.locuteur());
                }
                lireEtAttendre(texte, voixDeLaPhrase.reglages());
            } finally {
                // Les deux reprises sont dans un finally, et ce n'est pas de la précaution de
                // principe : la reconnaissance vocale est mise en pause AVANT la synthèse. Si
                // celle-ci échoue — Piper mort, fichier illisible, exception quelconque —, sans
                // ces lignes le robot resterait sourd définitivement, et qui attend la fin de la
                // phrase pour écouter la réponse attendrait pour rien.
                final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
                eventRedemarrage.setControle(CONTROLE.DEMARRER);
                applicationEventPublisher.publishEvent(eventRedemarrage);
                if (annoncerLaFin) {
                    applicationEventPublisher.publishEvent(new ParoleTermineeEvent(texte));
                }
            }
        } finally {
            bouche.unlock();
        }
    }

    private void lireEtAttendre(String texte, ReglagesVoix reglages) {
        logger.info("Lecture :\t{}", texte);

        // Phrase par phrase pour Piper seulement, et si le réglage le demande : chaque morceau de
        // Google serait un appel réseau de plus. En un seul morceau, tout se déroule comme avant.
        List<String> decoupe = voixReglable && Configurations.robotConfig().paroleParPhrase()
                ? DecoupageEnPhrases.decouper(texte)
                : List.of();
        List<String> morceaux = decoupe.isEmpty() ? List.of(texte) : decoupe;

        // Le morceau suivant se synthétise pendant que le précédent se dit : Piper va plus vite que
        // la parole, il est donc prêt à temps, et l'attente se réduit à la synthèse du premier.
        byte[] audio = synthetiser(morceaux.getFirst(), 1, morceaux.size());
        for (int i = 0; i < morceaux.size() && audio != null; i++) {
            String fichier = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "output-" + System.currentTimeMillis() + "-" + i + ".wav";
            try {
                Files.write(Path.of(fichier), audio);
                if (i == 0) {
                    attendreLeSilence();
                }
                Process lecture = new ProcessBuilder(commandeDeLecture(fichier, reglages)).start();
                audio = i + 1 < morceaux.size() ? synthetiser(morceaux.get(i + 1), i + 2, morceaux.size()) : null;
                lecture.waitFor();
            } catch (IOException e) {
                logger.error("Erreur lors de la lecture du fichier de synthèse vocale {}", fichier, e);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Lecture de la synthèse vocale interrompue");
                return;
            } finally {
                // Dite, la phrase ne sert plus : rien ne les effaçait, et chacune laissait son WAV
                // dans synthese-vocale/ depuis le passage à Piper.
                try {
                    Files.deleteIfExists(Path.of(fichier));
                } catch (IOException e) {
                    logger.warn("Fichier de phrase non effacé : {}", fichier, e);
                }
            }
        }
        logger.debug("Fin lecture :\t{}", texte);
    }

    /**
     * Synthétise un morceau, et journalise ce qu'il a coûté : le temps de synthèse décide du silence
     * avant la phrase — relevé à chaque fois pour le mesurer, pas le deviner.
     */
    private byte[] synthetiser(String morceau, int rang, int nombre) {
        long debut = System.currentTimeMillis();
        // Piper seulement : c'est lui qui prononçait mal le nom ; le texte journalisé reste l'original.
        byte[] audio = speechSynthesizerService.synthesize(voixReglable ? Prononciation.pourPiper(morceau) : morceau);
        logger.info("Phrase synthétisée en {} ms pour {} ms de parole ({} caractères{})",
                System.currentTimeMillis() - debut, dureeMs(audio), morceau.length(),
                nombre > 1 ? ", morceau " + rang + "/" + nombre : "");
        return audio;
    }

    /** La durée d'un WAV d'après son en-tête ; -1 si ce n'en est pas un. */
    private static long dureeMs(byte[] wav) {
        if (wav == null || wav.length < 44) {
            return -1;
        }
        ByteBuffer entete = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        int octetsParSeconde = entete.getInt(28);
        return octetsParSeconde <= 0 ? -1 : (wav.length - 44) * 1000L / octetsParSeconde;
    }

    /**
     * Attend qu'un son en cours se taise avant de parler : la phrase est déjà synthétisée, il ne
     * reste qu'à la dire. C'est ce qui permet à une réaction de jouer son animation, bande-son
     * comprise, <b>pendant</b> que Piper synthétise, puis à la phrase de suivre sans trou : le silence
     * vaut la plus longue des deux, et non leur somme.
     * <p>
     * Sans cette attente, le {@code play} de la voix trouvait la carte son prise, échouait, et la
     * phrase se perdait sans un mot. Au-delà de l'attente maximale, le son est coupé : le robot ne
     * doit jamais rester muet à cause d'un son bloqué.
     */
    private void attendreLeSilence() {
        if (lecteurSon == null || lecteurSon.sonEnCours().isEmpty()) {
            return;
        }
        String son = lecteurSon.sonEnCours().orElse("");
        long debut = System.currentTimeMillis();
        long limite = debut + Configurations.robotConfig().paroleAttenteSonMaxMs();
        try {
            while (lecteurSon.sonEnCours().isPresent()) {
                if (System.currentTimeMillis() > limite) {
                    logger.warn("Le son « {} » dure trop : coupé pour laisser parler", lecteurSon.sonEnCours().orElse(""));
                    lecteurSon.arreterLecture();
                    return;
                }
                Thread.sleep(50);
            }
            // Ce qui mesure le silence entre la réaction et la phrase : la synthèse est déjà faite,
            // tout ce qui s'attend ici est le son qui finit.
            logger.info("Phrase prête, dite après {} ms d'attente de « {} »", System.currentTimeMillis() - debut, son);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Piper : {@code play} directement, avec les effets des réglages — plus de script entre deux, que
     * seule une édition sur le Jetson pouvait changer. Google : son script, comme avant.
     */
    private List<String> commandeDeLecture(String fichier, ReglagesVoix reglages) {
        if (!voixReglable) {
            return List.of(fichierSyntheseVocale, fichier);
        }
        List<String> commande = new ArrayList<>(List.of("play", fichier));
        commande.addAll(reglages.effetsSox());
        return commande;
    }

    /** Dit une phrase avec une voix qu'on essaie, sans l'adopter. */
    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleEssaiDeVoixEvent(EssaiDeVoixEvent essai) {
        if (running && StringUtils.isNotBlank(essai.getTexte()) && essai.getVoix() != null) {
            lire(essai.getTexte().trim(), essai.getVoix(), false);
        }
    }

    /**
     * Intercepte les évènements pour lire du texte.
     *
     * @param paroleEvent évènement pour lire du texte
     */
    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleParoleEvent(ParoleEvent paroleEvent) {
        if (running && StringUtils.isNotBlank(paroleEvent.getTexte())) {
            lire(paroleEvent.getTexte().trim());
        }
    }

    @Override
    public void initialiser() {
        SpeechProviderConfig providerConfig = Configurations.speechProviderConfig();
        switch (providerConfig.synthesizerProvider()) {
            case PIPER -> {
                try {
                    piper = demarrerPiper();
                    speechSynthesizerService = piper;
                    // Pas de script de filtre : la voix se colore avec les réglages de l'appli.
                    voixReglable = true;
                } catch (Exception e) {
                    logger.error("Impossible d'initialiser Piper, repli sur Google", e);
                    speechSynthesizerService = GoogleSpeechSynthesizerService.getInstance();
                    fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + googleConfig.voiceFilter();
                }
            }
            case GOOGLE -> {
                speechSynthesizerService = GoogleSpeechSynthesizerService.getInstance();
                fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + googleConfig.voiceFilter();
            }
        }
    }

    /**
     * Avec le modèle adopté ; s'il ne démarre pas (effacé du Jetson, fichier abîmé), avec celui de
     * {@code robot.properties} plutôt que de passer à Google — qui demande le réseau et une clé.
     */
    private PiperSpeechSynthesizerService demarrerPiper() {
        Voix adoptee = voix.voix();
        if (adoptee.modele() == null) {
            return PiperSpeechSynthesizerService.getInstance(null, null);
        }
        try {
            return PiperSpeechSynthesizerService.getInstance(adoptee.modele(), adoptee.locuteur());
        } catch (IllegalStateException e) {
            logger.error("Modèle de voix adopté {} impossible à charger, modèle par défaut", adoptee.modele(), e);
            return PiperSpeechSynthesizerService.getInstance(null, null);
        }
    }

    @Override
    public void arreter() {

    }

    @Override
    public void start() {
        initialiser();
        running = true;
        logger.info("OrganeParole démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("OrganeParole arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.ACTIONNEURS;
    }
}
