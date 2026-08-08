package fr.roboteek.robot.organes.capteurs;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.writer.WaveHeader;
import com.google.common.primitives.Bytes;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.spring.server.websocket.RegistreAbonnesWebsocket;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Capteur vocal avec appel d'un web service externe pour effectuer la reconnaisance vocale.
 *
 * @author Nicolas
 */
public abstract class AbstractCapteurVocal extends AbstractOrganeWithThread {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCapteurVocal.class);

    /**
     * Fréquence d'échantillonage : celle du micro, qui n'en expose pas d'autre
     * (voir {@link Constantes#FREQUENCE_ECHANTILLONNAGE_CAPTURE_HZ}).
     */
    private static final float sampleRate = Constantes.FREQUENCE_ECHANTILLONNAGE_CAPTURE_HZ;

    /**
     * Taille du buffer, en frames : durée d'un bloc audio, donc granularité de la détection de
     * parole et unité des blocs de pré-amorce.
     * <p>
     * 1536 frames à 16 kHz = 96 ms, soit la même durée de bloc qu'avant le passage de 44,1 kHz à
     * 16 kHz (4096 frames = 93 ms). Volontairement conservée à l'identique : la baisser (par ex. à
     * 1024, soit 64 ms) modifierait à la fois la statistique de la détection de silence et la
     * durée de pré-amorce (5 blocs, ici 480 ms contre 464 ms avant), ce qui mêlerait un changement
     * de comportement au simple retrait du ré-échantillonnage. YIN travaille dans le domaine
     * temporel, la taille n'a pas besoin d'être une puissance de deux.
     */
    private static final int bufferSize = 1536;

    /**
     * Nombre de blocs audio que le buffer de la ligne peut contenir. La ligne est vidée en continu
     * par le dispatcher : cette marge ne sert qu'à absorber un aléa d'ordonnancement sans perdre
     * d'échantillons, elle n'ajoute pas de latence.
     */
    private static final int NB_BLOCS_BUFFER_LIGNE = 8;

    /**
     * ?.
     */
    private static final int overlap = 0;

    /**
     * Durée de silence (en secondes) marquant la fin d'une phrase, lue à chaque bloc audio et non
     * mise en cache : c'est ce qui rend le rechargement à chaud effectif (voir
     * {@link RobotConfig#dureeSilenceFinPhraseSecondes()}), et permet donc d'ajuster la réactivité
     * du robot sans le redéployer.
     *
     * @return la durée de silence marquant la fin d'une phrase, en secondes
     */
    private static double dureeSilenceFinPhrase() {
        return robotConfig().dureeSilenceFinPhraseSecondes();
    }

    /**
     * Flag indiquant que la reconnaissance est mise en pause
     * (modifié par les threads des listeners, lu par le thread audio).
     */
    private volatile boolean misEnPause = false;

    /**
     * Destination STOMP du flux audio (voir {@code WebSocketBrokerConfig}).
     */
    private static final String DESTINATION_AUDIO = "/audio";

    /**
     * Registre des abonnements WebSocket : sert à n'encoder le WAV que si un client l'écoute.
     */
    @Autowired(required = false)
    private RegistreAbonnesWebsocket registreAbonnesWebsocket;

    /**
     * Indique si un client est abonné au flux audio.
     */
    private boolean diffusionAudioEcoutee() {
        return registreAbonnesWebsocket != null
                && registreAbonnesWebsocket.aAuMoinsUnAbonne(DESTINATION_AUDIO);
    }

    /**
     * Dispatcher audio (permet d'arrêter proprement l'acquisition).
     */
    private AudioDispatcher dispatcher;

    /**
     * Format audio.
     */
    private AudioFormat format;

    /**
     * Timestamp précédent (permet de connaître le temps depuis le dernier bloc "parlé").
     */
    private double timestampDernierBlocParle = 0;

    /**
     * Chemin du fichier WAV.
     */
    private String cheminFichierWav;

    /**
     * Buffer permettant de stocker le signal audio précédent.
     */
    private byte[] bufferNMoins1;

    /**
     * Buffer permettant de stocker le signal audio précédent.
     */
    private byte[] bufferNMoins2;

    /**
     * Buffer permettant de stocker le signal audio précédent.
     */
    private byte[] bufferNMoins3;

    /**
     * Buffer permettant de stocker le signal audio précédent.
     */
    private byte[] bufferNMoins4;

    /**
     * Buffer permettant de stocker le signal audio précédent.
     */
    private byte[] bufferNMoins5;

    /**
     * Buffer permettant de stocker une phase de reconnaissance.
     */
    private byte[] contenuParle;

    /**
     * Configuration.
     */
    private RobotConfig robotConfig;


    public AbstractCapteurVocal(String threadName) {
        super(threadName);
        robotConfig = robotConfig();
    }

    @Override
    public void initialiser() {

        try {

            final Path dossierReconnaissanceVocaleGoogle = Paths.get(Constantes.DOSSIER_RECONNAISSANCE_VOCALE);
            logger.debug("Dossier de reconnaissance vocale : {}", dossierReconnaissanceVocaleGoogle);
            if (!Files.exists(dossierReconnaissanceVocaleGoogle)) {
                // Création du dossier
                Files.createDirectories(dossierReconnaissanceVocaleGoogle);
                logger.debug("Dossier de reconnaissance vocale créé : {}", dossierReconnaissanceVocaleGoogle);
            }
            cheminFichierWav = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "reconnaissance.wav";

            // Définition du format audio d'acquisition
            format = new AudioFormat(sampleRate, 16, 1, true, false);

            // Recherche de la ligne correspondant au micro recherché
            final TargetDataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = null;
            Mixer.Info[] infoMixers = AudioSystem.getMixerInfo();
            logger.debug("Recherche du micro « {} » parmi {} mixers", robotConfig.microphoneName(), infoMixers.length);
            for (Mixer.Info infoMixer : infoMixers) {
                logger.debug("Mixer disponible : {}", infoMixer.getName());
                if (infoMixer.getName() != null && infoMixer.getName().contains(robotConfig.microphoneName())) {
                    Mixer mixer = AudioSystem.getMixer(infoMixer);
                    if (mixer.isLineSupported(dataLineInfo)) {
                        line = (TargetDataLine) mixer.getLine(dataLineInfo);
                        logger.debug("Ligne micro trouvée pour « {} »", robotConfig.microphoneName());
                        break;
                    }
                }
            }
            // Si le micro n'est pas trouvé, on prend une ligne par défaut
            if (line == null) {
                logger.warn("Micro « {} » introuvable : utilisation de la ligne audio par défaut", robotConfig.microphoneName());
                line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            }

            // Récupération du flux du micro au format souhaité
            final AudioInputStream stream = new AudioInputStream(line);
            final JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
            dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);

            // Ouverture du flux et démarrage de l'acquisition.
            // Attention à l'unité : open() attend une taille en OCTETS, alors que bufferSize est
            // en frames (c'est ainsi que AudioDispatcher l'interprète). Le code précédent passait
            // bufferSize brut, donnant un buffer de ligne deux fois plus court qu'un bloc audio.
            line.open(format, bufferSize * format.getFrameSize() * NB_BLOCS_BUFFER_LIGNE);
            line.start();

            // Initialisation des tableaux d'octets contenant les différents blocs audio
            contenuParle = new byte[0];
            bufferNMoins1 = new byte[0];
            bufferNMoins2 = new byte[0];
            bufferNMoins3 = new byte[0];
            bufferNMoins4 = new byte[0];
            bufferNMoins5 = new byte[0];

            // Création d'un processeur détectant les silences dans le flux
            final SilenceDetector silenceDetector = new SilenceDetector(SilenceDetector.DEFAULT_SILENCE_THRESHOLD, false);

            // Création d'un processeur permettant de déterminer la hauteur d'un bloc audio (permet de récupérer la fréquence)
            AudioProcessor p = new PitchProcessor(PitchEstimationAlgorithm.YIN, sampleRate, bufferSize, new PitchDetectionHandler() {

                public synchronized void handlePitch(PitchDetectionResult result, AudioEvent e) {

                    // Signe de vie : un bloc audio nous parvient. C'est la seule preuve que la
                    // capture micro vit encore — le thread du dispatcher peut mourir sans que le
                    // drapeau de cycle de vie de l'organe ne bouge d'un iota.
                    battement();

                    // Flag permettant de savoir si le flux en cours de traitement est un flux "parlé"
                    boolean isBlocParle;

                    // Récupération du timestamp du bloc audio
                    double timestampBlocEnCours = e.getTimeStamp();

                    // On teste si le bloc en cours contient de la voix (bruit à une certaine fréquence)

                    if (!misEnPause) {
                        // Si le bloc ne correspond pas à un silence (volume au delà d'un certain seuil), on traite ce bloc
                        if (silenceDetector.currentSPL() > SilenceDetector.DEFAULT_SILENCE_THRESHOLD) {

                            // Récupération de la fréquence du bloc
                            final float pitchInHz = result.getPitch();

                            // Si le bloc est compris dans une certaine plage de fréquences : bloc contenant de la voix (parlé)
                            isBlocParle = pitchInHz > 0;
                        } else {
                            isBlocParle = false;
                        }

                        if (isBlocParle || timestampBlocEnCours - timestampDernierBlocParle < dureeSilenceFinPhrase()) {
                            // Si ça parle, ou petit silence
                            if (contenuParle.length == 0) {
                                surDebutPhrase(Bytes.concat(bufferNMoins5, bufferNMoins4, bufferNMoins3, bufferNMoins2, bufferNMoins1));
                            }
                            surBlocAudio(e.getByteBuffer());
                            // On concatène le bloc audio en cours au contenu général
                            contenuParle = Bytes.concat(contenuParle, e.getByteBuffer());
                        } else {
                            // Ca ne parle pas et grand silence

                            // On ne lance le traitement que s'il y a du contenu
                            if (contenuParle.length > 0) {
                                surFinPhrase();
                                genererFichierEtTraiterDetectionVocale();

                                // Réinitialisation des blocs
                                contenuParle = new byte[0];
                                bufferNMoins1 = new byte[0];
                                bufferNMoins2 = new byte[0];
                                bufferNMoins3 = new byte[0];
                                bufferNMoins4 = new byte[0];
                                bufferNMoins5 = new byte[0];
                            } else {
                                // Silence et pas de contenu : on ne fait rien
                            }
                        }

                        // Mise à jour du timestamp précédent par celui du bloc audio en cours si c'est un bloc "parlé"
                        if (isBlocParle) {
                            timestampDernierBlocParle = timestampBlocEnCours;
                        }

                        // Si aucun bloc "parlé" depuis la dernière reconnaissance : échange des blocs précédant un éventuel bloc "parlé"
                        if (contenuParle.length == 0) {
                            bufferNMoins5 = Bytes.concat(bufferNMoins4);
                            bufferNMoins4 = Bytes.concat(bufferNMoins3);
                            bufferNMoins3 = Bytes.concat(bufferNMoins2);
                            bufferNMoins2 = Bytes.concat(bufferNMoins1);
                            bufferNMoins1 = Bytes.concat(e.getByteBuffer());
                        }
                    } else {
                        // Capteur en pause : réinitialisation des blocs (pas de log, appelé à chaque bloc audio)
                        contenuParle = new byte[0];
                        bufferNMoins1 = new byte[0];
                        bufferNMoins2 = new byte[0];
                        bufferNMoins3 = new byte[0];
                        bufferNMoins4 = new byte[0];
                        bufferNMoins5 = new byte[0];
                    }

                    // Envoi de l'évènement audio : fichier WAV (entête + contenu).
                    // Uniquement si un client l'écoute, et hors pause : pendant une pause le
                    // micro ne capte que le robot en train de parler, et l'écho ainsi renvoyé
                    // à la webapp ne sert à rien tout en consommant du débit sur la session.
                    if (!misEnPause && diffusionAudioEcoutee()) {
                        fr.roboteek.robot.systemenerveux.event.AudioEvent audioEvent = new fr.roboteek.robot.systemenerveux.event.AudioEvent();
                        audioEvent.setAudioContentBase64(Base64.getEncoder().encodeToString(creerFichierWav(e.getByteBuffer())));
                        applicationEventPublisher.publishEvent(audioEvent);
                    }
                }

            });

            // Assemblage des différents processeurs dans le dispatcher
            dispatcher.addAudioProcessor(silenceDetector);
            dispatcher.addAudioProcessor(p);

            // Override thread with dispatcher
            thread = new Thread(dispatcher, "Audio Dispatcher");

        } catch (LineUnavailableException | IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
    }

    @Override
    public void loop() {
        // Do nothing because the thread is overriden in the init method
    }

    @Override
    public void arreter() {
        if (dispatcher != null) {
            dispatcher.stop();
        }
    }

    public abstract void traiterDetectionVocale(String cheminFichierWav);

    /**
     * Appelé au premier bloc "parlé" d'une nouvelle phrase, avec l'audio de pré-amorce
     * (mêmes blocs que {@code bufferNMoins1..5}, dans le même ordre que dans
     * {@link #genererFichierEtTraiterDetectionVocale()}). No-op par défaut : ne change rien pour
     * un capteur qui ne surcharge pas ce hook (ex. reconnaissance vocale Google, batch).
     *
     * @param audioPreOnset audio précédant le premier bloc "parlé", au format de capture d'origine
     */
    protected void surDebutPhrase(byte[] audioPreOnset) {
    }

    /**
     * Appelé pour chaque bloc audio faisant partie de la phrase en cours (y compris le tout
     * premier, juste après {@link #surDebutPhrase(byte[])}). No-op par défaut.
     *
     * @param blocAudio le bloc audio, au format de capture d'origine
     */
    protected void surBlocAudio(byte[] blocAudio) {
    }

    /**
     * Appelé juste avant {@link #genererFichierEtTraiterDetectionVocale()}, à la fin d'une
     * phrase (silence prolongé détecté). No-op par défaut.
     */
    protected void surFinPhrase() {
    }

    /**
     * Appelé quand une phrase en cours est abandonnée (démarrage ou mise en pause de la
     * reconnaissance vocale). No-op par défaut.
     */
    protected void surInterruptionPhrase() {
    }

    private synchronized void genererFichierEtTraiterDetectionVocale() {

        try {
            // On ne traite la detection vocale que s'il y a du contenu
            if (contenuParle.length > 0) {
                logger.debug("Génération du fichier contenant le flux de parole détecté");

                // Concaténation du contenu parlé avec le contenu précédent
                contenuParle = Bytes.concat(bufferNMoins5, bufferNMoins4, bufferNMoins3, bufferNMoins2, bufferNMoins1, contenuParle);

                // Création du fichier Wav
                final String cheminFichierWavTemp = cheminFichierWav.replace(".wav", "") + System.currentTimeMillis() + ".wav";
                final RandomAccessFile fichierWavRandom = new RandomAccessFile(cheminFichierWavTemp, "rw");
                fichierWavRandom.seek(0);
                // Création du fichier WAV (entête + contenu)
                fichierWavRandom.write(creerFichierWav(contenuParle));
                fichierWavRandom.close();

                // Traitement de la détection vocale
                traiterDetectionVocale(cheminFichierWavTemp);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Intercepte les évènements de contrôle de la reconnaissance vocale.
     *
     * @param reconnaissanceVocaleControleEvent évènement de contrôle de la reconnaissance vocale
     */
    @EventListener
    public void handleReconnaissanceVocaleControleEvent(ReconnaissanceVocaleControleEvent reconnaissanceVocaleControleEvent) {
        if (reconnaissanceVocaleControleEvent.getControle() == ReconnaissanceVocaleControleEvent.CONTROLE.DEMARRER) {
            logger.debug("Démarrage de la reconnaissance vocale");
            misEnPause = false;
            surInterruptionPhrase();
            // Réinitialisation des blocs
            contenuParle = new byte[0];
            bufferNMoins1 = new byte[0];
            bufferNMoins2 = new byte[0];
            bufferNMoins3 = new byte[0];
            bufferNMoins4 = new byte[0];
            bufferNMoins5 = new byte[0];
        } else if (reconnaissanceVocaleControleEvent.getControle() == ReconnaissanceVocaleControleEvent.CONTROLE.METTRE_EN_PAUSE) {
            logger.debug("Mise en pause de la reconnaissance vocale");
            misEnPause = true;
            surInterruptionPhrase();
            // Réinitialisation des blocs
            contenuParle = new byte[0];
            bufferNMoins1 = new byte[0];
            bufferNMoins2 = new byte[0];
            bufferNMoins3 = new byte[0];
            bufferNMoins4 = new byte[0];
            bufferNMoins5 = new byte[0];
        }
    }

    /**
     * Crée l'entête WAV au contenu audio
     *
     * @param contenuAudio le contenu audio
     * @return le fichier WAV (entête + contenu) sous forme de tableau d'octets
     */
    private byte[] creerFichierWav(byte[] contenuAudio) {
        // Création du header WAV
        WaveHeader waveHeader = new WaveHeader(WaveHeader.FORMAT_PCM,
                (short) format.getChannels(),
                (int) format.getSampleRate(), (short) 16, contenuAudio.length);
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        try {
            waveHeader.write(header);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return Bytes.concat(header.toByteArray(), contenuAudio);
    }
}
