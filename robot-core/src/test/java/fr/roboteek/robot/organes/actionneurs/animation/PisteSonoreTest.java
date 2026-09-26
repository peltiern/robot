package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.SoundPlayer;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.SonDeclenche;
import fr.roboteek.robot.organes.actionneurs.son.BibliothequeDesSons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La bande-son d'une animation : assemblée en un seul fichier, jouée d'un seul `play`. */
class PisteSonoreTest {

    @TempDir
    Path dossier;

    private BibliothequeDesSons bibliotheque;

    private LecteurEnTrompeLOeil lecteur;

    private PisteSonore piste;

    @BeforeEach
    void setUp() {
        bibliotheque = new BibliothequeDesSons(dossier);
        lecteur = new LecteurEnTrompeLOeil();
        piste = new PisteSonore(bibliotheque, lecteur, () -> 300);
    }

    /** Un son du Studio : `n` échantillons tous à la même valeur, faciles à retrouver dans la bande. */
    private void ranger(String nom, int n, short valeur) {
        short[] echantillons = new short[n];
        Arrays.fill(echantillons, valeur);
        bibliotheque.enregistrer(nom, JsonMapper.builder().build().readTree("{\"version\":1}"), PisteSonore.enWav(echantillons));
    }

    private static Animation avecSons(long dureeMs, SonDeclenche... sons) {
        return new Animation("Essai", dureeMs, List.of(), List.of(sons));
    }

    @Nested
    class Assemblage {

        @Test
        void chaqueSonEstPoseASonInstantDansLeSilence() {
            short[] bande = PisteSonore.assembler(List.of(new PisteSonore.Pose(1000, new short[]{7, 7})), 2000);

            assertEquals(88200, bande.length, "la bande dure ce qu'on lui demande");
            assertEquals(0, bande[44099]);
            assertEquals(7, bande[44100], "à 1 s, on doit trouver le son");
            assertEquals(7, bande[44101]);
            assertEquals(0, bande[44102]);
        }

        /** Deux sons qui se chevauchent s'entendent ensemble, au lieu que l'un coupe l'autre. */
        @Test
        void deuxSonsQuiSeChevauchentSontMelanges() {
            short[] bande = PisteSonore.assembler(List.of(
                    new PisteSonore.Pose(0, new short[]{100, 100}),
                    new PisteSonore.Pose(0, new short[]{50})), 100);

            assertEquals(150, bande[0]);
            assertEquals(100, bande[1]);
        }

        /** Écrêtée au plafond plutôt que repliée : repliée, la somme craquerait. */
        @Test
        void unMelangeTropFortEstEcreteEtNonReplie() {
            short[] bande = PisteSonore.assembler(List.of(
                    new PisteSonore.Pose(0, new short[]{30000, -30000}),
                    new PisteSonore.Pose(0, new short[]{30000, -30000})), 100);

            assertEquals(Short.MAX_VALUE, bande[0]);
            assertEquals(Short.MIN_VALUE, bande[1]);
        }

        @Test
        void leWavEcritSeRelitTelQuel() {
            short[] echantillons = {1, -2, 300, -32768, 32767};

            assertArrayEquals(echantillons, PisteSonore.lireWav(PisteSonore.enWav(echantillons)).orElseThrow());
        }

        /** Additionné tel quel, un autre format serait déformé : il est refusé, pas converti. */
        @Test
        void unWavDUnAutreFormatEstRefuse() {
            byte[] wav = PisteSonore.enWav(new short[]{1, 2, 3});
            ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN).putInt(24, 22050);

            assertEquals(Optional.empty(), PisteSonore.lireWav(wav));
            assertEquals(Optional.empty(), PisteSonore.lireWav("pas un wav du tout".getBytes()));
        }
    }

    @Nested
    class Lecture {

        @Test
        void laBandeSonPartDUnSeulPlayEtLesSonsYSontALeurPlace() throws IOException {
            ranger("a", 441, (short) 11);
            ranger("b", 441, (short) 22);

            boolean partie = piste.demarrer(avecSons(1000, new SonDeclenche(0, "a"), new SonDeclenche(500, "b")), 0);

            assertTrue(partie);
            assertEquals(1, lecteur.lancements, "un seul play pour toute l'animation");
            short[] bande = PisteSonore.lireWav(Files.readAllBytes(lecteur.fichier)).orElseThrow();
            assertEquals(11, bande[0]);
            assertEquals(22, bande[22050], "le second son à 500 ms");
            assertEquals(0.0, lecteur.depuis);
        }

        /** Une lecture reprise au milieu fait entendre la bande-son à partir de là. */
        @Test
        void unDepartAuMilieuEstTransmisAPlay() {
            ranger("a", 44100, (short) 5);

            piste.demarrer(avecSons(2000, new SonDeclenche(0, "a")), 800);

            assertEquals(0.8, lecteur.depuis, 1e-9);
        }

        /** Rien à entendre à partir de là : pas de play, et l'animation n'a pas à attendre. */
        @Test
        void unDepartApresLeDernierSonNeJoueRien() {
            ranger("a", 4410, (short) 5);   // 100 ms

            assertFalse(piste.demarrer(avecSons(2000, new SonDeclenche(0, "a")), 500));
            assertEquals(0, lecteur.lancements);
        }

        @Test
        void uneAnimationSansSonNeJoueRien() {
            assertFalse(piste.demarrer(avecSons(2000), 0));
            assertEquals(0, lecteur.lancements);
        }

        /** Un son supprimé du Studio depuis l'écriture de l'animation est sauté, sans rien casser. */
        @Test
        void unSonAbsentDeLaBibliothequeEstSaute() throws IOException {
            ranger("a", 441, (short) 11);

            assertTrue(piste.demarrer(avecSons(1000, new SonDeclenche(0, "disparu"), new SonDeclenche(0, "a")), 0));
            assertEquals(11, PisteSonore.lireWav(Files.readAllBytes(lecteur.fichier)).orElseThrow()[0]);
        }

        /** Interrompre l'animation coupe sa bande-son… */
        @Test
        void arreterCoupeLaBandeSon() {
            ranger("a", 441, (short) 11);
            piste.demarrer(avecSons(1000, new SonDeclenche(0, "a")), 0);

            piste.arreter();

            assertTrue(lecteur.coupe);
        }

        /** … mais pas un son lancé entre-temps depuis le Studio, qui n'est pas à elle. */
        @Test
        void arreterNeCoupePasUnSonQuiNEstPasLeSien() {
            ranger("a", 441, (short) 11);
            piste.demarrer(avecSons(1000, new SonDeclenche(0, "a")), 0);
            lecteur.enCours = "coucou";

            piste.arreter();

            assertFalse(lecteur.coupe);
        }

        /** Une animation allée au bout laisse sa bande-son s'éteindre, et n'en coupe plus rien. */
        @Test
        void uneAnimationFinieNeCoupePlusRien() {
            ranger("a", 441, (short) 11);
            piste.demarrer(avecSons(1000, new SonDeclenche(0, "a")), 0);

            piste.laisserFinir();
            piste.arreter();

            assertFalse(lecteur.coupe);
        }
    }

    private static class LecteurEnTrompeLOeil extends SoundPlayer {

        private int lancements;

        private Path fichier;

        private double depuis = -1;

        private String enCours;

        private boolean coupe;

        @Override
        public synchronized boolean jouer(String nom, Path fichier, double depuisSecondes) {
            lancements++;
            this.fichier = fichier;
            this.depuis = depuisSecondes;
            this.enCours = nom;
            return true;
        }

        @Override
        public Optional<String> sonEnCours() {
            return Optional.ofNullable(enCours);
        }

        @Override
        public synchronized boolean arreterLecture() {
            coupe = true;
            enCours = null;
            return true;
        }
    }
}
