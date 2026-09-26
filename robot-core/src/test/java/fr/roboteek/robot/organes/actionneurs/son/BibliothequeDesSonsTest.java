package fr.roboteek.robot.organes.actionneurs.son;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibliothequeDesSonsTest {

    @TempDir
    Path dossier;

    private BibliothequeDesSons bibliotheque;

    @BeforeEach
    void setUp() {
        bibliotheque = new BibliothequeDesSons(dossier);
    }

    private static JsonNode recette(String nom) {
        return JsonMapper.builder().build().readTree("""
                {"nom":"%s","version":1,"reglages":{"volume":0.6},
                 "morceaux":[{"timbre":"voix","debut":0,"duree":0.25,"courbe":[1,1.3]}]}
                """.formatted(nom));
    }

    /** Un WAV minimal : en-tête RIFF/WAVE de 44 octets, sans échantillon. */
    private static byte[] wav() {
        byte[] octets = new byte[46];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, octets, 0, 4);
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, octets, 8, 4);
        return octets;
    }

    @Nested
    class AllerRetour {

        @Test
        void unSonEnregistreSeRelitAvecSaRecetteEtSonAudio() throws IOException {
            bibliotheque.enregistrer("Coucou", recette("Coucou"), wav());

            assertEquals(Optional.of(recette("Coucou")), bibliotheque.recette("Coucou"));
            assertTrue(bibliotheque.audio("Coucou").isPresent());
            assertEquals(46, Files.size(bibliotheque.audio("Coucou").orElseThrow()));
        }

        @Test
        void lesNomsSontTriesEtNeComptentQuUneFoisParSon() {
            bibliotheque.enregistrer("Surprise", recette("Surprise"), wav());
            bibliotheque.enregistrer("babil", recette("babil"), wav());

            assertEquals(List.of("babil", "Surprise"), bibliotheque.noms());
        }

        /** Le Studio fabrique lui-même « humeur colère » ou « arpège » : le robot doit les ranger. */
        @Test
        void unNomAccentueEstAccepte() {
            bibliotheque.enregistrer("humeur colère", recette("humeur colère"), wav());

            assertEquals(List.of("humeur colère"), bibliotheque.noms());
            assertTrue(bibliotheque.audio("humeur colère").isPresent());
        }

        /**
         * Un « é » s'écrit en un caractère ou en « e » plus l'accent (un nom de fichier venu d'un Mac).
         * Les deux désignent le même son : sans ça, deux fichiers identiques à l'œil cohabiteraient.
         */
        @Test
        void unAccentDecomposeDesigneLeMemeSon() {
            bibliotheque.enregistrer("ohé", recette("ohé"), wav());

            assertTrue(bibliotheque.existe("ohe\u0301"));
            bibliotheque.enregistrer("ohe\u0301", recette("ohé"), wav());
            assertEquals(List.of("ohé"), bibliotheque.noms());
        }

        @Test
        void unSonInconnuNExistePas() {
            assertFalse(bibliotheque.existe("Fantome"));
            assertEquals(Optional.empty(), bibliotheque.recette("Fantome"));
            assertEquals(Optional.empty(), bibliotheque.audio("Fantome"));
            assertFalse(bibliotheque.supprimer("Fantome"));
        }

        @Test
        void supprimerEmporteLaRecetteEtLAudio() {
            bibliotheque.enregistrer("Coucou", recette("Coucou"), wav());

            assertTrue(bibliotheque.supprimer("Coucou"));
            assertEquals(List.of(), bibliotheque.noms());
            assertEquals(Optional.empty(), bibliotheque.audio("Coucou"));
        }
    }

    @Nested
    class CeQuiEstRefuse {

        /** Un nom venu d'une URL sert à fabriquer un chemin : sans filtre, il écrirait ailleurs. */
        @Test
        void unNomQuiRemonteLArborescenceEstRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> bibliotheque.enregistrer("../../passwd", recette("x"), wav()));
            assertFalse(bibliotheque.existe("../../passwd"));
        }

        @Test
        void uneRecetteSansVersionEstRefusee() {
            JsonNode sansVersion = JsonMapper.builder().build().readTree("{\"nom\":\"Coucou\"}");

            assertThrows(IllegalArgumentException.class,
                    () -> bibliotheque.enregistrer("Coucou", sansVersion, wav()));
        }

        /** Le robot ne relit pas la recette : si l'audio n'en est pas, il n'a plus rien à jouer. */
        @Test
        void unAudioQuiNEstPasUnWavEstRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> bibliotheque.enregistrer("Coucou", recette("Coucou"), "pas du son".getBytes(StandardCharsets.UTF_8)));
            assertFalse(bibliotheque.existe("Coucou"));
        }
    }

    @Nested
    class FichiersAbimes {

        @Test
        void uneRecetteIllisibleNeCasseRienDAutre() throws IOException {
            bibliotheque.enregistrer("Bon", recette("Bon"), wav());
            Files.writeString(dossier.resolve("Casse.json"), "{ ceci n'est pas du JSON");

            assertEquals(Optional.empty(), bibliotheque.recette("Casse"));
            assertTrue(bibliotheque.recette("Bon").isPresent());
        }

        /**
         * L'audio est écrit avant la recette : un son listé a donc toujours de quoi être joué.
         * L'inverse annoncerait des sons muets après une coupure en plein enregistrement.
         */
        @Test
        void unWavSansRecetteNEstPasAnnonce() throws IOException {
            Files.write(dossier.resolve("Orphelin.wav"), wav());

            assertEquals(List.of(), bibliotheque.noms());
        }
    }
}
