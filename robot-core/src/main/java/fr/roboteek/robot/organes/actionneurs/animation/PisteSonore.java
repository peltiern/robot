package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.actionneurs.SoundPlayer;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.SonDeclenche;
import fr.roboteek.robot.organes.actionneurs.son.BibliothequeDesSons;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * La bande-son d'une animation : tous ses sons assemblés en <b>un seul fichier</b>, silences
 * compris, et joués d'un seul {@code play}.
 * <p>
 * Un {@code play} par son ne tenait pas : {@code play} joue directement sur la carte, qui n'accepte
 * qu'un programme à la fois, si bien que deux sons proches se la disputaient et que le second
 * échouait (voir {@code SoundPlayer.arreterLecture}). Un seul fichier règle la dispute, place chaque
 * son à l'échantillon près, et mélange deux sons qui se chevauchent au lieu que l'un coupe l'autre.
 * <p>
 * Il n'y a plus qu'<b>une</b> latence à compenser, celle du lancement de {@code play} (environ
 * 300 ms, {@code RobotConfig.animationSonAvanceMs}) : c'est l'animation qui attend le son, pas
 * l'inverse. Un son posé à l'instant zéro tombe ainsi juste lui aussi, ce qu'aucune avance ne
 * permettait — on ne joue pas avant le lancement.
 * <p>
 * Assemblée par le robot, au moment de jouer, et non par l'éditeur : retoucher un son dans le
 * Studio doit profiter à toutes les animations qui s'en servent, sans les reconstruire.
 */
@Component
public class PisteSonore {

    private static final Logger logger = LoggerFactory.getLogger(PisteSonore.class);

    /** Le seul format que le Studio produit, et donc le seul qu'on sait additionner tel quel. */
    static final int FREQUENCE = 44100;

    /** Un son à sa place dans la bande-son. */
    record Pose(long instantMs, short[] echantillons) {
    }

    private final BibliothequeDesSons bibliotheque;

    private final SoundPlayer lecteurSon;

    private final LongSupplier avanceMs;

    /** Le nom sous lequel notre bande-son joue, tant qu'elle nous appartient ; {@code null} sinon. */
    private String enCours;

    /** Le fichier de la bande-son précédente, effacé dès que la suivante a pris sa place. */
    private Path fichierPrecedent;

    /**
     * {@code @Autowired} obligatoire : deux constructeurs, et sans lui Spring n'en choisit aucun
     * et tout le contexte échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public PisteSonore(BibliothequeDesSons bibliotheque, SoundPlayer lecteurSon) {
        this(bibliotheque, lecteurSon, () -> Configurations.robotConfig().animationSonAvanceMs());
    }

    /** Pour les tests : une avance qu'on maîtrise, sans configuration. */
    PisteSonore(BibliothequeDesSons bibliotheque, SoundPlayer lecteurSon, LongSupplier avanceMs) {
        this.bibliotheque = bibliotheque;
        this.lecteurSon = lecteurSon;
        this.avanceMs = avanceMs;
    }

    /** Ce que l'animation doit attendre, une fois la bande-son lancée, pour que les deux coïncident. */
    public long avanceMs() {
        return avanceMs.getAsLong();
    }

    /**
     * Assemble et lance la bande-son d'une animation, à partir de {@code depuisMs}.
     *
     * @return vrai si une bande-son est partie — l'animation doit alors attendre {@link #avanceMs()}
     *         avant de bouger ; faux si elle n'a rien à faire entendre à partir de là
     */
    public synchronized boolean demarrer(Animation animation, long depuisMs) {
        arreter();
        List<Pose> poses = new ArrayList<>();
        for (SonDeclenche son : animation.sons()) {
            if (son.instant() >= animation.dureeTotale()) {
                continue;
            }
            lireSon(animation.nom(), son).ifPresent(echantillons -> poses.add(new Pose(son.instant(), echantillons)));
        }
        // La bande-son s'arrête avec l'animation : un son plus long qu'elle allait au bout, bien après
        // l'arrêt des moteurs, sans que la timeline de l'Atelier le montre.
        long finMs = Math.min(animation.dureeTotale(),
                poses.stream().mapToLong(p -> p.instantMs() + p.echantillons().length * 1000L / FREQUENCE).max().orElse(0));
        if (finMs <= depuisMs) {
            return false;
        }
        try {
            Path fichier = Files.createTempFile("bande-son-", ".wav");
            fichier.toFile().deleteOnExit();
            Files.write(fichier, enWav(assembler(poses, animation.dureeTotale())));
            String nom = "bande-son de « " + animation.nom() + " »";
            if (!lecteurSon.jouer(nom, fichier, depuisMs / 1000.0)) {
                logger.warn("Animation « {} » : sa bande-son n'a pas pu être jouée", animation.nom());
                Files.deleteIfExists(fichier);
                return false;
            }
            enCours = nom;
            effacerPrecedent(fichier);
            return true;
        } catch (IOException e) {
            logger.error("Animation « {} » : bande-son non assemblée", animation.nom(), e);
            return false;
        }
    }

    /**
     * Coupe la bande-son, si c'est encore elle qui sonne : l'animation a été interrompue. Un son
     * lancé entre-temps depuis le Studio n'est pas le nôtre, et reste.
     */
    public synchronized void arreter() {
        if (enCours != null && enCours.equals(lecteurSon.sonEnCours().orElse(null))) {
            lecteurSon.arreterLecture();
        }
        enCours = null;
    }

    /** L'animation est allée au bout : sa bande-son s'éteint d'elle-même, elle n'est plus à nous. */
    public synchronized void laisserFinir() {
        enCours = null;
    }

    /**
     * Les sons additionnés à leur place, sur une bande de {@code dureeMs}, bornés au plafond. Deux
     * sons qui se chevauchent s'entendent ensemble ; leur somme est écrêtée plutôt que repliée, ce
     * qui sonnerait comme un craquement.
     */
    static short[] assembler(List<Pose> poses, long dureeMs) {
        int[] somme = new int[(int) (dureeMs * FREQUENCE / 1000)];
        for (Pose pose : poses) {
            int debut = (int) (pose.instantMs() * FREQUENCE / 1000);
            for (int i = 0; i < pose.echantillons().length && debut + i < somme.length; i++) {
                somme[debut + i] += pose.echantillons()[i];
            }
        }
        short[] bande = new short[somme.length];
        for (int i = 0; i < somme.length; i++) {
            bande[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, somme[i]));
        }
        return bande;
    }

    /**
     * Les échantillons d'un WAV 44,1 kHz mono 16 bits, le format du Studio. Tout autre format est
     * refusé plutôt que converti : l'additionner tel quel le déformerait.
     */
    static Optional<short[]> lireWav(byte[] octets) {
        ByteBuffer tampon = ByteBuffer.wrap(octets).order(ByteOrder.LITTLE_ENDIAN);
        if (octets.length < 12 || tampon.getInt(0) != 0x46464952 /* RIFF */ || tampon.getInt(8) != 0x45564157 /* WAVE */) {
            return Optional.empty();
        }
        boolean formatDuStudio = false;
        int position = 12;
        while (position + 8 <= octets.length) {
            int identifiant = tampon.getInt(position);
            int taille = tampon.getInt(position + 4);
            int contenu = position + 8;
            if (identifiant == 0x20746d66 /* "fmt " */) {
                formatDuStudio = tampon.getShort(contenu) == 1 && tampon.getShort(contenu + 2) == 1
                        && tampon.getInt(contenu + 4) == FREQUENCE && tampon.getShort(contenu + 14) == 16;
            } else if (identifiant == 0x61746164 /* "data" */) {
                if (!formatDuStudio) {
                    return Optional.empty();
                }
                int nb = Math.min(taille, octets.length - contenu) / 2;
                short[] echantillons = new short[nb];
                for (int i = 0; i < nb; i++) {
                    echantillons[i] = tampon.getShort(contenu + 2 * i);
                }
                return Optional.of(echantillons);
            }
            // Les blocs de taille impaire sont suivis d'un octet de bourrage.
            position = contenu + taille + (taille & 1);
        }
        return Optional.empty();
    }

    static byte[] enWav(short[] echantillons) {
        ByteBuffer wav = ByteBuffer.allocate(44 + echantillons.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        wav.putInt(0x46464952).putInt(36 + echantillons.length * 2).putInt(0x45564157);
        wav.putInt(0x20746d66).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(FREQUENCE).putInt(FREQUENCE * 2).putShort((short) 2).putShort((short) 16);
        wav.putInt(0x61746164).putInt(echantillons.length * 2);
        for (short echantillon : echantillons) {
            wav.putShort(echantillon);
        }
        return wav.array();
    }

    private Optional<short[]> lireSon(String animation, SonDeclenche son) {
        if (son.son() == null || son.son().isBlank()) {
            return Optional.empty();
        }
        Optional<Path> fichier = bibliotheque.audio(son.son());
        if (fichier.isEmpty()) {
            // Sauté, pas bloquant : le son a pu être supprimé du Studio depuis que l'animation a été
            // écrite. Le geste se joue sans lui, et le journal dit lequel manque.
            logger.warn("Animation « {} » : le son « {} » n'est plus dans la bibliothèque, il est sauté", animation, son.son());
            return Optional.empty();
        }
        try {
            Optional<short[]> echantillons = lireWav(Files.readAllBytes(fichier.get()));
            if (echantillons.isEmpty()) {
                logger.warn("Animation « {} » : le son « {} » n'est pas en 44,1 kHz mono 16 bits, il est sauté", animation, son.son());
            }
            return echantillons;
        } catch (IOException e) {
            logger.warn("Animation « {} » : le son « {} » est illisible, il est sauté", animation, son.son(), e);
            return Optional.empty();
        }
    }

    private void effacerPrecedent(Path nouveau) {
        if (fichierPrecedent != null) {
            try {
                Files.deleteIfExists(fichierPrecedent);
            } catch (IOException e) {
                logger.debug("Bande-son précédente non effacée : {}", fichierPrecedent, e);
            }
        }
        fichierPrecedent = nouveau;
    }

    @PreDestroy
    synchronized void fermer() {
        effacerPrecedent(null);
    }
}
