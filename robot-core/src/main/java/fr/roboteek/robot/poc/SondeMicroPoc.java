package fr.roboteek.robot.poc;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Sonde de capacités du micro : énumère les lignes disponibles et leurs formats, puis capture
 * réellement quelques secondes à 16 kHz et à 44,1 kHz en écrivant deux WAV comparables (dérive
 * d'horloge, RMS/crête/saturation, répartition spectrale par PSD de Welch).
 * <p>
 * Écrite pour trancher le choix de la fréquence de capture, à l'époque où le robot capturait en
 * 44,1 kHz et ré-échantillonnait vers le 16 kHz exigé par Vosk. <b>Verdict</b>
 * : le ReSpeaker (ArrayUAC10) n'expose <i>que</i> du 16 kHz, tout le reste étant fabriqué par la
 * couche {@code plug} d'ALSA — le robot capture donc désormais nativement en 16 kHz
 * ({@link fr.roboteek.robot.Constantes#FREQUENCE_ECHANTILLONNAGE_CAPTURE_HZ}) et plus aucun
 * ré-échantillonnage ne subsiste. Conservée comme outil de diagnostic micro réutilisable.
 * <p>
 * <b>Aucune dépendance</b> hors JDK. Attention toutefois : l'image Docker du robot n'embarque
 * qu'un <i>JRE</i>, le lancement direct de la source y est donc impossible. Compiler d'abord côté
 * poste de développement, puis exécuter les classes dans le conteneur :
 * <pre>
 *   javac --release 25 -d /tmp/sonde .../poc/SondeMicroPoc.java
 *   scp -r /tmp/sonde/* jetson@wall-e.local:~/Robot/Programme/
 *   docker-compose ... run --rm --no-deps robot-core \
 *       java -cp /Robot/Programme fr.roboteek.robot.poc.SondeMicroPoc [duree_s] [dossier_sortie]
 * </pre>
 * <b>Important</b> : le conteneur de production garde le micro ouvert en continu. Il faut donc
 * l'arrêter avant de lancer la sonde ({@code docker-compose ... stop}), sinon l'ouverture de ligne
 * échouera (la sonde le signale explicitement au lieu de planter).
 */
public class SondeMicroPoc {

    /** Format retenu : celui qu'attend Vosk, et le seul que le micro expose réellement. */
    private static final AudioFormat FORMAT_16K = new AudioFormat(16000f, 16, 1, true, false);

    /** Ancien format du robot, capturé en témoin : ALSA le fabrique par sur-échantillonnage. */
    private static final AudioFormat FORMAT_44K = new AudioFormat(44100f, 16, 1, true, false);

    /**
     * Taille de buffer de ligne, en frames. À 16 kHz, le {@code bufferSize} de 4096 frames utilisé
     * aujourd'hui par le capteur donnerait des blocs de 256 ms (contre 93 ms à 44,1 kHz), trop
     * grossier pour la détection de silence : 1024 frames rétablit ~64 ms.
     */
    private static final int FRAMES_BUFFER_16K = 1024;

    private static final int FRAMES_BUFFER_44K = 4096;

    /**
     * Nom (partiel) du micro à sélectionner, par défaut celui de {@code device.microphone.name}.
     * La sonde reproduit la sélection par nom faite par le capteur, avec le même repli sur la
     * ligne par défaut : tester un autre périphérique que celui réellement utilisé en production
     * donnerait une réponse sans valeur.
     */
    private static String nomMicro = "ArrayUAC10";

    public static void main(String[] args) {
        double dureeSecondes = args.length > 0 ? Double.parseDouble(args[0]) : 4.0;
        String dossierSortie = args.length > 1 ? args[1] : ".";
        if (args.length > 2) {
            nomMicro = args[2];
        }

        System.out.println("=== Sonde des capacités du micro ===");
        System.out.printf("Durée de capture par format : %.1f s | micro recherché : « %s »%n",
                dureeSecondes, nomMicro);
        System.out.printf("Dossier de sortie : %s%n%n", new File(dossierSortie).getAbsolutePath());

        enumererLignes();
        testerSupport();

        System.out.println("\n=== 3) Captures réelles ===");
        System.out.println("Parle normalement pendant chaque capture (une phrase complète, "
                + "assez longue pour couvrir toute la durée).\n");

        ResultatCapture r16 = capturer(FORMAT_16K, FRAMES_BUFFER_16K, dureeSecondes,
                new File(dossierSortie, "sonde-micro-16k.wav"));
        ResultatCapture r44 = capturer(FORMAT_44K, FRAMES_BUFFER_44K, dureeSecondes,
                new File(dossierSortie, "sonde-micro-44k.wav"));

        conclure(r16, r44);
    }

    // ------------------------------------------------------------------
    // 1) Inventaire
    // ------------------------------------------------------------------

    private static void enumererLignes() {
        System.out.println("=== 1) Lignes de capture disponibles ===");
        boolean auMoinsUne = false;
        for (Mixer.Info infoMixer : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(infoMixer);
            List<String> formats = new ArrayList<>();
            for (Line.Info infoLigne : mixer.getTargetLineInfo()) {
                if (infoLigne instanceof DataLine.Info infoDataLine) {
                    auMoinsUne = true;
                    for (AudioFormat format : infoDataLine.getFormats()) {
                        formats.add(decrire(format));
                    }
                }
            }
            if (!formats.isEmpty()) {
                System.out.printf("  [%s] %s%n", infoMixer.getName(), infoMixer.getDescription());
                // Les pilotes annoncent souvent des dizaines de variantes : on ne garde que les
                // formats mono 16 bits, les seuls pertinents ici.
                formats.stream().filter(f -> f.contains("mono") && f.contains("16 bits"))
                        .distinct().sorted().forEach(f -> System.out.println("      " + f));
            }
        }
        if (!auMoinsUne) {
            System.out.println("  AUCUNE ligne de capture détectée : soit aucun micro n'est visible,");
            System.out.println("  soit Java Sound n'a pas accès au périphérique audio (droits, ALSA).");
        }
    }

    private static String decrire(AudioFormat format) {
        float frequence = format.getSampleRate();
        return String.format("%s Hz, %d bits, %s, %s",
                frequence == AudioSystem.NOT_SPECIFIED ? "variable" : String.format("%.0f", frequence),
                format.getSampleSizeInBits(),
                format.getChannels() == 1 ? "mono" : format.getChannels() + " canaux",
                format.isBigEndian() ? "big-endian" : "little-endian");
    }

    // ------------------------------------------------------------------
    // 2) Support déclaré
    // ------------------------------------------------------------------

    private static void testerSupport() {
        System.out.println("\n=== 2) Support déclaré des formats visés ===");
        for (AudioFormat format : new AudioFormat[]{FORMAT_16K, FORMAT_44K}) {
            boolean supporte = AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, format));
            System.out.printf("  %-40s : %s%n", decrire(format),
                    supporte ? "supporté" : "NON supporté (déclaré)");
        }
        System.out.println("  Note : un « non supporté » ici n'est pas rédhibitoire — ALSA sait");
        System.out.println("  rééchantillonner via sa couche « plug », de façon continue (donc");
        System.out.println("  correcte, contrairement au découpage par chunk actuel). Seule la");
        System.out.println("  capture réelle ci-dessous tranche.");
    }

    // ------------------------------------------------------------------
    // 3) Capture réelle
    // ------------------------------------------------------------------

    /**
     * @param framesChronometrees frames capturées pendant la fenêtre chronométrée (hors démarrage
     *                            de ligne), seule base valable pour mesurer la dérive d'horloge
     */
    private record ResultatCapture(AudioFormat format, boolean reussie, String erreur,
                                   int framesCaptures, int framesAttendues, int framesChronometrees,
                                   double dureeReelleS, double rms, int crete, int nbSaturations,
                                   double[] energiesBandes, File fichier) {

        static ResultatCapture echec(AudioFormat format, String erreur) {
            return new ResultatCapture(format, false, erreur, 0, 0, 0, 0, 0, 0, 0, new double[5], null);
        }

        /** Écart entre le temps réellement écoulé et le temps que représente l'audio capturé. */
        double derivePourcent() {
            double dureeAudio = framesChronometrees / (double) format.getSampleRate();
            if (dureeAudio < 1e-9) return 0;
            return 100 * (dureeReelleS - dureeAudio) / dureeAudio;
        }
    }

    private static ResultatCapture capturer(AudioFormat format, int framesBuffer, double dureeSecondes,
                                            File fichier) {
        System.out.printf("--- Capture à %.0f Hz (buffer de %d frames = %.0f ms) ---%n",
                format.getSampleRate(), framesBuffer, 1000.0 * framesBuffer / format.getSampleRate());

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        TargetDataLine ligne = null;
        try {
            ligne = trouverLigne(info);
            if (ligne == null) {
                System.out.println("  Aucune ligne ne supporte ce format, capture non tentée.\n");
                return ResultatCapture.echec(format, "format non supporté");
            }
            ligne.open(format, framesBuffer * format.getFrameSize());
            ligne.start();

            int octetsAttendus = (int) (format.getSampleRate() * format.getFrameSize() * dureeSecondes);
            byte[] capture = new byte[octetsAttendus];
            byte[] tampon = new byte[framesBuffer * format.getFrameSize()];
            int total = 0;
            // Chrono démarré après la première lecture : l'ouverture puis le démarrage de la ligne
            // coûtent quelques dizaines de ms, qui fausseraient la mesure de dérive d'horloge (le
            // seul indice d'un rééchantillonnage douteux en amont) sur une capture courte.
            long debut = 0;
            int octetsAuDebutDuChrono = 0;
            System.out.println("  Parle maintenant...");
            while (total < octetsAttendus) {
                int lus = ligne.read(tampon, 0, Math.min(tampon.length, octetsAttendus - total));
                if (lus <= 0) break;
                System.arraycopy(tampon, 0, capture, total, lus);
                total += lus;
                if (debut == 0) {
                    debut = System.nanoTime();
                    octetsAuDebutDuChrono = total;
                }
            }
            double dureeReelle = debut == 0 ? 0 : (System.nanoTime() - debut) / 1e9;
            int framesChronometrees = (total - octetsAuDebutDuChrono) / format.getFrameSize();
            ligne.stop();

            byte[] utile = total == capture.length ? capture : java.util.Arrays.copyOf(capture, total);
            ResultatCapture resultat = analyser(format, utile, octetsAttendus, framesChronometrees,
                    dureeReelle, fichier);
            ecrireWav(format, utile, fichier);
            afficher(resultat);
            return resultat;

        } catch (Exception e) {
            String message = e.getClass().getSimpleName() + " : " + e.getMessage();
            System.out.println("  ÉCHEC de la capture : " + message);
            if (e instanceof javax.sound.sampled.LineUnavailableException) {
                System.out.println("  => Ligne indisponible : le conteneur de production tient très");
                System.out.println("     probablement le micro ouvert. Arrête-le puis relance :");
                System.out.println("     docker-compose -f ~/Robot/docker/docker-compose.yml stop");
            }
            System.out.println();
            return ResultatCapture.echec(format, message);
        } finally {
            if (ligne != null && ligne.isOpen()) {
                ligne.close();
            }
        }
    }

    /**
     * Sélectionne la ligne de capture comme le fait {@code AbstractCapteurVocal} : recherche du
     * mixer dont le nom contient {@link #nomMicro}, puis repli sur la ligne par défaut. Le mixer
     * effectivement retenu est affiché, sans quoi les mesures seraient ininterprétables.
     */
    private static TargetDataLine trouverLigne(DataLine.Info info) throws Exception {
        for (Mixer.Info infoMixer : AudioSystem.getMixerInfo()) {
            if (infoMixer.getName() != null && infoMixer.getName().contains(nomMicro)) {
                Mixer mixer = AudioSystem.getMixer(infoMixer);
                if (mixer.isLineSupported(info)) {
                    System.out.printf("  ligne utilisée     : mixer « %s » (micro recherché)%n", infoMixer.getName());
                    return (TargetDataLine) mixer.getLine(info);
                }
                System.out.printf("  mixer « %s » trouvé mais ne supporte pas ce format%n", infoMixer.getName());
            }
        }
        if (!AudioSystem.isLineSupported(info)) {
            return null;
        }
        System.out.printf("  ligne utilisée     : LIGNE PAR DÉFAUT (micro « %s » introuvable) —%n"
                + "                       comme le capteur en production dans ce cas%n", nomMicro);
        return (TargetDataLine) AudioSystem.getLine(info);
    }

    private static ResultatCapture analyser(AudioFormat format, byte[] pcm, int octetsAttendus,
                                            int framesChronometrees, double dureeReelle, File fichier) {
        int nbFrames = pcm.length / 2;
        double sommeCarres = 0;
        int crete = 0;
        int saturations = 0;
        for (int i = 0; i < nbFrames; i++) {
            int v = echantillon(pcm, i);
            sommeCarres += (double) v * v;
            crete = Math.max(crete, Math.abs(v));
            if (Math.abs(v) >= 32700) saturations++;
        }
        double rms = nbFrames == 0 ? 0 : Math.sqrt(sommeCarres / nbFrames);

        double[] energies = repartitionParBande(pcm, format.getSampleRate());
        return new ResultatCapture(format, true, null, nbFrames, octetsAttendus / 2,
                framesChronometrees, dureeReelle, rms, crete, saturations, energies, fichier);
    }

    private static void afficher(ResultatCapture r) {
        System.out.printf("  frames capturées   : %d / %d attendues%n", r.framesCaptures(), r.framesAttendues());
        System.out.printf("  dérive d'horloge   : %+.2f %% (%.3f s écoulées pour %.3f s d'audio,%n"
                        + "                       hors démarrage de ligne)%n",
                r.derivePourcent(), r.dureeReelleS(),
                r.framesChronometrees() / (double) r.format().getSampleRate());
        System.out.printf("  niveau             : RMS %.0f (%.1f %% pleine échelle), crête %d%s%n",
                r.rms(), 100 * r.rms() / 32768, r.crete(),
                r.nbSaturations() > 0 ? "  <- " + r.nbSaturations() + " échantillons saturés !" : "");
        System.out.print("  énergie par bande  : ");
        for (int b = 0; b < r.energiesBandes().length; b++) {
            if (BANDES[b] < r.format().getSampleRate() / 2) {
                System.out.printf("%.0f-%.0fHz=%.1f%%  ", BANDES[b], BANDES[b + 1], r.energiesBandes()[b]);
            }
        }
        System.out.println();
        // Une capture demandée au-dessus de la fréquence native du micro est nécessairement
        // suréchantillonnée en amont : l'absence d'énergie au-delà de la moitié de la fréquence
        // native le prouve, et signale que ce format n'apporte aucune information.
        if (r.format().getSampleRate() > 16000 && r.energiesBandes().length >= 5) {
            double auDela = r.energiesBandes()[4];
            System.out.printf("  4-8 kHz : %.3f %% — %s%n", auDela, auDela < 0.5
                    ? "quasi vide, cohérent avec une source de fréquence plus basse suréchantillonnée"
                    : "contenu réel, la source est bien à cette fréquence");
        }
        if (r.rms() < 50) {
            System.out.println("  ATTENTION : niveau quasi nul — micro muet, mauvais périphérique,");
            System.out.println("  ou rien n'a été dit pendant la capture.");
        }
        System.out.println("  WAV écrit          : " + r.fichier().getAbsolutePath());
        System.out.println();
    }

    private static void conclure(ResultatCapture r16, ResultatCapture r44) {
        System.out.println("=== Conclusion ===");
        if (r16.reussie()) {
            boolean sain = r16.rms() > 50 && r16.nbSaturations() == 0 && Math.abs(r16.derivePourcent()) < 2;
            System.out.println(sain
                    ? "  Capture 16 kHz FONCTIONNELLE et saine : on peut supprimer le\n"
                      + "  ré-échantillonnage et capturer directement au format de Vosk."
                    : "  Capture 16 kHz obtenue mais SUSPECTE (voir avertissements ci-dessus) :\n"
                      + "  à écouter avant de décider.");
        } else {
            System.out.println("  Capture 16 kHz IMPOSSIBLE (" + r16.erreur() + ") :");
            System.out.println("  il faudra conserver un ré-échantillonnage, mais continu (à état),");
            System.out.println("  et non par chunk comme aujourd'hui.");
        }
        if (r16.reussie() && r44.reussie()) {
            System.out.println("\n  Écoute et compare les deux WAV : ils doivent sonner équivalents.");
            System.out.println("  Une différence nette de timbre ou des artefacts sur le 16 kHz");
            System.out.println("  signaleraient un rééchantillonnage de mauvaise qualité en amont.");
        }
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private static void ecrireWav(AudioFormat format, byte[] pcm, File fichier) {
        try (AudioInputStream flux = new AudioInputStream(new ByteArrayInputStream(pcm), format, pcm.length / 2)) {
            AudioSystem.write(flux, AudioFileFormat.Type.WAVE, fichier);
        } catch (Exception e) {
            System.out.println("  (impossible d'écrire " + fichier + " : " + e.getMessage() + ")");
        }
    }

    /** Bornes des bandes analysées, en Hz. Voir {@link #BANDES}. */
    static final double[] BANDES = {0, 500, 1000, 2000, 4000, 8000};

    /**
     * Répartition de l'énergie entre les bandes de {@link #BANDES}, en pourcentage du total.
     * <p>
     * Mesurée par densité spectrale moyennée (méthode de Welch : fenêtres de Hann de 1024 points
     * à recouvrement de 50 %). Une corrélation à une fréquence unique sur toute la capture ne
     * marcherait pas : la parole n'est pas stationnaire, donc une telle corrélation tend vers zéro
     * quel que soit le contenu réel, et donnait des valeurs trompeuses proches de 0.
     */
    private static double[] repartitionParBande(byte[] pcm, double frequenceEchantillonnage) {
        int taille = 1024;
        int nbFrames = pcm.length / 2;
        double[] parts = new double[BANDES.length - 1];
        if (nbFrames < taille) {
            return parts;
        }
        double[] fenetre = new double[taille];
        for (int i = 0; i < taille; i++) {
            fenetre[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (taille - 1));
        }
        double[] psd = new double[taille / 2];
        int nbBlocs = 0;
        for (int debut = 0; debut + taille <= nbFrames; debut += taille / 2) {
            double[] re = new double[taille];
            double[] im = new double[taille];
            for (int i = 0; i < taille; i++) {
                re[i] = echantillon(pcm, debut + i) * fenetre[i];
            }
            fft(re, im);
            for (int i = 0; i < taille / 2; i++) {
                psd[i] += re[i] * re[i] + im[i] * im[i];
            }
            nbBlocs++;
        }
        double resolution = frequenceEchantillonnage / taille;
        double total = 0;
        for (double p : psd) {
            total += p;
        }
        if (total <= 0) {
            return parts;
        }
        for (int b = 0; b < parts.length; b++) {
            double energie = 0;
            // Borne haute exclusive : sinon le bin de frontière est compté dans les deux bandes
            // voisines et le total dépasse 100 %.
            int premier = (int) Math.ceil(BANDES[b] / resolution);
            int dernier = (int) Math.ceil(BANDES[b + 1] / resolution);
            for (int i = premier; i < dernier && i < psd.length; i++) {
                energie += psd[i];
            }
            parts[b] = 100 * energie / total;
        }
        return parts;
    }

    /** FFT radix-2 sur place, sur des tableaux de taille puissance de deux. */
    private static void fft(double[] re, double[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) {
                j ^= bit;
            }
            j ^= bit;
            if (i < j) {
                double t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        for (int longueur = 2; longueur <= n; longueur <<= 1) {
            double angle = -2 * Math.PI / longueur;
            double wr = Math.cos(angle);
            double wi = Math.sin(angle);
            for (int i = 0; i < n; i += longueur) {
                double cr = 1;
                double ci = 0;
                for (int j = 0; j < longueur / 2; j++) {
                    int a = i + j;
                    int b = a + longueur / 2;
                    double tr = re[b] * cr - im[b] * ci;
                    double ti = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - tr;
                    im[b] = im[a] - ti;
                    re[a] += tr;
                    im[a] += ti;
                    double ncr = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = ncr;
                }
            }
        }
    }

    private static int echantillon(byte[] pcm, int frame) {
        return (short) ((pcm[frame * 2] & 0xff) | (pcm[frame * 2 + 1] << 8));
    }
}
