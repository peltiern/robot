package fr.roboteek.robot.organes.actionneurs.transmission;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La conversion angle d'organe ↔ position moteur, et le contrat que toute loi doit tenir. */
class TransmissionTest {

    private static final double PRES = 1e-9;

    /**
     * Les valeurs exactes qui étaient écrites à la main dans {@code Yeux} et {@code Cou}, avec les
     * nombres de {@code robot.properties}. Ce test est le filet du déménagement : si l'une de ces
     * égalités tombe, le robot ne bouge plus comme avant, et c'est tout ce qu'on ne veut pas.
     */
    @Nested
    class LesFormulesDuRobot {

        @Test
        void oeilGaucheDescendQuandLeMoteurMonte() {
            // Yeux.toPositionAbsolueOeilGauche : absolue = zéro - relative, zéro = 98
            Transmission gauche = Transmission.affine(98, -1);

            assertEquals(78, gauche.versMoteur(20), PRES);
            assertEquals(103, gauche.versMoteur(-5), PRES);
            assertEquals(20, gauche.depuisMoteur(78), PRES);
        }

        @Test
        void oeilDroitEstMonteEnMiroir() {
            // Yeux.toPositionAbsolueOeilDroit : absolue = zéro + relative, zéro = 95.
            // Le signe opposé à l'œil gauche n'est pas une coquille : les deux servos se font face.
            Transmission droit = Transmission.affine(95, +1);

            assertEquals(115, droit.versMoteur(20), PRES);
            assertEquals(90, droit.versMoteur(-5), PRES);
            assertEquals(20, droit.depuisMoteur(115), PRES);
        }

        @Test
        void lesTroisAxesDuCouSontDecroissants() {
            // Cou : positionMoteur = init - position, init = 95 / 67 / 140
            assertEquals(35, Transmission.affine(95, -1).versMoteur(60), PRES);
            assertEquals(155, Transmission.affine(95, -1).versMoteur(-60), PRES);
            assertEquals(60, Transmission.affine(67, -1).versMoteur(7), PRES);
            assertEquals(90, Transmission.affine(140, -1).versMoteur(50), PRES);
        }

        /**
         * La butée moteur haute est la butée d'organe basse. Rien dans le code ne doit supposer
         * que min va sur min : c'est vrai pour quatre des cinq axes du robot.
         */
        @Test
        void laButeeMoteurHauteEstLaButeeOrganeBasse() {
            Transmission panoramique = Transmission.affine(95, -1);

            assertTrue(panoramique.depuisMoteur(155) < panoramique.depuisMoteur(35));
        }
    }

    @Nested
    class LeContrat {

        @Test
        void lesDeuxSensSontReciproques() {
            for (Transmission transmission : new Transmission[]{
                    Transmission.affine(98, -1), Transmission.affine(95, +1),
                    Transmission.affine(95, -1.583), Transmission.affine(67, -4.75)}) {
                for (double angle = -60; angle <= 60; angle += 0.5) {
                    assertEquals(angle, transmission.depuisMoteur(transmission.versMoteur(angle)), 1e-6,
                            () -> "aller-retour rompu sur " + transmission);
                }
            }
        }

        /**
         * Le gain sert à convertir une vitesse, et une vitesse n'a pas de signe — même sur une
         * transmission décroissante, ce que sont quatre axes sur cinq. Un gain négatif passerait
         * une limite de vitesse négative au contrôleur, qui la refuse.
         */
        @Test
        void leGainEstToujoursPositif() {
            assertEquals(1, Transmission.affine(98, -1).gain(0), PRES);
            assertEquals(1 / 1.583, Transmission.affine(95, -1.583).gain(0), PRES);
        }

        /** Le gain par défaut de l'interface doit tomber sur le même nombre que la version exacte. */
        @Test
        void leGainNumeriqueVautLeGainExact() {
            Transmission affine = Transmission.affine(95, -1.583);
            Transmission memeLoiSansGainDedie = new Transmission() {
                @Override
                public double versMoteur(double angleOrgane) {
                    return affine.versMoteur(angleOrgane);
                }

                @Override
                public double depuisMoteur(double positionMoteur) {
                    return affine.depuisMoteur(positionMoteur);
                }
            };

            assertEquals(affine.gain(0), memeLoiSansGainDedie.gain(0), 1e-9);
        }

        @Test
        void unRapportNulEstRefuse() {
            assertThrows(IllegalArgumentException.class, () -> Transmission.affine(95, 0));
        }
    }
}
