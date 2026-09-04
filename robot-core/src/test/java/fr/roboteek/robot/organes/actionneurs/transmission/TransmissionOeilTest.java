package fr.roboteek.robot.organes.actionneurs.transmission;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La loi du quadrilatère, confrontée au relevé du STL.
 * <p>
 * Les valeurs attendues ne sont pas recopiées de cette implémentation : elles viennent de
 * {@code robot-core/3d/mesures/MESURES.md}, produit par un script Python indépendant. C'est donc
 * un vrai test de non-divergence entre deux calculs de la même géométrie, comme l'est déjà
 * {@code InterpolateurTest} face au Catmull-Rom de l'éditeur.
 */
class TransmissionOeilTest {

    @Nested
    class LaTableDuReleve {

        /** Un centième de degré : la précision à laquelle la fiche de cotes donne ses valeurs. */
        private static final double PRES = 0.011;

        @Test
        void chaqueAngleDeServoDonneLAngleDOeilReleve() {
            assertEquals(6.86, TransmissionOeil.oeilDepuisServo(-5.60), PRES);
            assertEquals(6.14, TransmissionOeil.oeilDepuisServo(-5), PRES);
            assertEquals(0, TransmissionOeil.oeilDepuisServo(0), PRES);
            assertEquals(-6.42, TransmissionOeil.oeilDepuisServo(5), PRES);
            assertEquals(-13.30, TransmissionOeil.oeilDepuisServo(10), PRES);
            assertEquals(-21.03, TransmissionOeil.oeilDepuisServo(15), PRES);
            assertEquals(-31.07, TransmissionOeil.oeilDepuisServo(20), PRES);
            assertEquals(-35.01, TransmissionOeil.oeilDepuisServo(21.30), PRES);
        }

        /**
         * Le rapport n'est pas constant, et c'est toute la raison d'être de cette classe : trois
         * fois plus d'œil par degré de servo en bas de course qu'au neutre.
         */
        @Test
        void leRapportTripleLeLongDeLaCourse() {
            assertEquals(1.252, rapport(0), 0.005);
            assertEquals(1.441, rapport(10), 0.005);
            assertEquals(2.595, rapport(20), 0.01);
            // Juste en deçà de la butée, et non dessus : oeilDepuisServo écrête au-delà, donc une
            // dérivée centrée sur la butée elle-même enjambe le plat et rend la moitié du rapport.
            // Sans conséquence — à la butée rien ne bouge — mais ça ferait un test qui ment.
            assertEquals(3.675, rapport(21.29), 0.02);
        }

        private static double rapport(double servo) {
            double pas = 0.001;
            return Math.abs((TransmissionOeil.oeilDepuisServo(servo + pas)
                    - TransmissionOeil.oeilDepuisServo(servo - pas)) / (2 * pas));
        }
    }

    @Nested
    class CeQuiSortDeLaCourse {

        /**
         * Au-delà du point mort, la fermeture n'a plus de solution. Rendre {@code NaN} ferait
         * voyager le défaut jusqu'à une consigne moteur absurde, sans une trace.
         */
        @Test
        void auDelaDuPointMortOnRendLePointMort() {
            double auPointMort = TransmissionOeil.oeilDepuisServo(TransmissionOeil.SERVO_POINT_MORT);

            assertEquals(auPointMort, TransmissionOeil.oeilDepuisServo(90));
            assertTrue(Double.isFinite(TransmissionOeil.oeilDepuisServo(1000)));
        }

        /**
         * Le contact des coques n'est <b>pas</b> écrêté ici : c'est une limite de politique, qui
         * vit dans la configuration. La position de repos des yeux est justement au-delà — elle
         * cherche l'appui mécanique — et un écrêtage la rendrait inatteignable.
         */
        @Test
        void leContactDesCoquesNEstPasUneLimiteDeLaLoi() {
            double auRepos = TransmissionOeil.oeilDepuisServo(-7);

            assertTrue(auRepos > TransmissionOeil.oeilDepuisServo(TransmissionOeil.SERVO_CONTACT_DES_COQUES_BAS),
                    "le repos doit pouvoir dépasser le contact des coques");
            assertEquals(-7, TransmissionOeil.servoDepuisOeil(auRepos), 1e-6);
        }
    }

    @Nested
    class LeContrat {

        /** La dichotomie doit vraiment inverser la forme close, sur toute la course. */
        @Test
        void lesDeuxSensSontReciproques() {
            for (double servo = TransmissionOeil.SERVO_CONTACT_DES_COQUES_BAS; servo <= TransmissionOeil.SERVO_CONTACT_DES_COQUES_HAUT; servo += 0.1) {
                double angleServo = servo;
                double oeil = TransmissionOeil.oeilDepuisServo(angleServo);

                assertEquals(angleServo, TransmissionOeil.servoDepuisOeil(oeil), 1e-6,
                        () -> "aller-retour rompu à " + angleServo + "° de servo");
            }
        }

        @Test
        void laLoiEstStrictementDecroissante() {
            double precedent = Double.MAX_VALUE;
            for (double servo = TransmissionOeil.SERVO_CONTACT_DES_COQUES_BAS; servo <= TransmissionOeil.SERVO_CONTACT_DES_COQUES_HAUT; servo += 0.05) {
                double angleServo = servo;
                double oeil = TransmissionOeil.oeilDepuisServo(angleServo);
                assertTrue(oeil < precedent, () -> "non monotone à " + angleServo + "° de servo");
                precedent = oeil;
            }
        }

        /**
         * Montée jusqu'au moteur : l'œil gauche tel qu'il est câblé aujourd'hui (zéro 98, une
         * unité moteur pour un degré de servo, sens décroissant). Le signe reste à confronter au
         * robot ; la réciprocité, elle, ne dépend pas de lui.
         */
        @Test
        void laMonteeJusquAuMoteurResteReciproque() {
            for (Transmission oeil : new Transmission[]{new TransmissionOeil(98, -1), new TransmissionOeil(95, +1)}) {
                for (double angle = -8; angle <= 31; angle += 0.25) {
                    assertEquals(angle, oeil.depuisMoteur(oeil.versMoteur(angle)), 1e-6);
                }
            }
        }

        /**
         * Le miroir : les deux servos sont montés en sens inverse, et c'est le <b>seul</b> écart
         * entre les deux yeux. Une même commande relative doit donc donner le même angle d'organe
         * des deux côtés — sinon les yeux ne bougeraient pas ensemble.
         */
        @Test
        void lesDeuxYeuxVontDuMemeCoteAvecLaMemeCommande() {
            Transmission gauche = new TransmissionOeil(98, -1);
            Transmission droit = new TransmissionOeil(95, +1);

            for (double relatif : new double[]{-5, 0, 10, 20}) {
                assertEquals(gauche.depuisMoteur(98 - relatif), droit.depuisMoteur(95 + relatif), 1e-9,
                        "les deux yeux divergent à " + relatif);
            }
        }

        /**
         * Le sens de l'angle d'organe : il croît avec la commande relative, à l'inverse du repère
         * du relevé. C'est ce qui garde aux curseurs verticaux du HUD le sens qu'ils ont
         * aujourd'hui.
         */
        @Test
        void langleCroitDansLeSensDeLaCommande() {
            Transmission gauche = new TransmissionOeil(98, -1);

            assertTrue(gauche.depuisMoteur(98 - 20) > gauche.depuisMoteur(98 - 0));
            assertEquals(31.06, gauche.depuisMoteur(98 - 20), 0.01);
            assertEquals(-6.14, gauche.depuisMoteur(98 + 5), 0.01);
        }

        /**
         * Le gain sert à convertir une vitesse : il vaut ici l'inverse du rapport, et il varie.
         * C'est ce qui rendait les vitesses des animations bancales sans qu'on sache pourquoi.
         */
        @Test
        void leGainSuitLaCourse() {
            Transmission oeilGauche = new TransmissionOeil(98, -1);

            assertEquals(1 / 1.252, oeilGauche.gain(0), 0.005);
            assertTrue(oeilGauche.gain(31) < oeilGauche.gain(0) / 2,
                    "en bout de course, un degré d'œil coûte trois fois moins de servo");
        }
    }
}
