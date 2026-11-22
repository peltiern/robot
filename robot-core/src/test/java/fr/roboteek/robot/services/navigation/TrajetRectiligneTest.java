package fr.roboteek.robot.services.navigation;

import fr.roboteek.robot.services.navigation.TrajetRectiligne.Consigne;
import fr.roboteek.robot.services.navigation.TrajetRectiligne.Fin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrajetRectiligneTest {

    @Test
    void avanceAVitesseDeCroisiereLoinDeLaCible() {
        TrajetRectiligne trajet = new TrajetRectiligne(900, 0.6);
        Consigne consigne = trajet.suivante(0, 0, 0);
        assertFalse(consigne.estFinie());
        assertEquals(0.6, consigne.gauche(), 1e-9);
        assertEquals(0.6, consigne.droite(), 1e-9);
    }

    @Test
    void ralentitPresDeLaCibleEtSArreteDessus() {
        TrajetRectiligne trajet = new TrajetRectiligne(900, 0.6);
        trajet.suivante(0, 0, 0);
        Consigne approche = trajet.suivante(800, 800, 100);
        assertEquals(TrajetRectiligne.VITESSE_APPROCHE, approche.gauche(), 1e-9);

        Consigne fin = trajet.suivante(899, 901, 200);
        assertEquals(Fin.ATTEINT, fin.fin());
        assertEquals(0, fin.gauche());
        assertEquals(0, fin.droite());
    }

    /** La droite tourne plus vite : elle prend de l'avance, on la ralentit et on presse la gauche. */
    @Test
    void ralentitLaChenilleQuiPrendDeLAvance() {
        TrajetRectiligne trajet = new TrajetRectiligne(900, 0.6);
        trajet.suivante(0, 0, 0);
        Consigne consigne = trajet.suivante(200, 225, 100);
        assertEquals(0.65, consigne.gauche(), 1e-9);
        assertEquals(0.55, consigne.droite(), 1e-9);
    }

    @Test
    void laCorrectionEstBornee() {
        TrajetRectiligne trajet = new TrajetRectiligne(900, 0.6);
        trajet.suivante(0, 0, 0);
        Consigne consigne = trajet.suivante(0, 400, 100);
        assertEquals(0.6 + TrajetRectiligne.CORRECTION_MAX, consigne.gauche(), 1e-9);
        assertEquals(0.6 - TrajetRectiligne.CORRECTION_MAX, consigne.droite(), 1e-9);
    }

    @Test
    void reculeAvecDesConsignesNegativesEtLaMemeCorrection() {
        TrajetRectiligne trajet = new TrajetRectiligne(-500, 0.6);
        trajet.suivante(0, 0, 0);
        // En arrière, la droite a reculé de 25 mm de plus : c'est encore elle qu'on ralentit
        Consigne consigne = trajet.suivante(-100, -125, 100);
        assertEquals(-0.65, consigne.gauche(), 1e-9);
        assertEquals(-0.55, consigne.droite(), 1e-9);

        assertEquals(Fin.ATTEINT, trajet.suivante(-500, -501, 200).fin());
        assertEquals(-500, trajet.distanceMm());
    }

    /** Chenille bloquée ou encodeur débranché : on n'attend pas la durée maximale pour couper. */
    @Test
    void sArreteSiLesEncodeursNeBougentPlus() {
        TrajetRectiligne trajet = new TrajetRectiligne(900, 0.6);
        trajet.suivante(0, 0, 0);
        trajet.suivante(100, 100, 500);
        assertFalse(trajet.suivante(100, 100, 1400).estFinie());
        assertEquals(Fin.IMMOBILE, trajet.suivante(100.5, 100.5, 1600).fin());
    }

    @Test
    void sArreteQuandLeTrajetDureTrop() {
        TrajetRectiligne trajet = new TrajetRectiligne(100, 0.6);
        long dureeMax = TrajetRectiligne.DUREE_MARGE_MS + 100 * (long) TrajetRectiligne.DUREE_MS_PAR_MM;
        long t = 0;
        double chemin = 0;
        trajet.suivante(0, 0, t);
        // Il avance, mais si lentement qu'il n'arrive jamais : 2 mm toutes les 500 ms
        Consigne consigne;
        do {
            t += 500;
            chemin += 2;
            consigne = trajet.suivante(chemin, chemin, t);
        } while (!consigne.estFinie());
        assertEquals(Fin.DUREE_DEPASSEE, consigne.fin());
        assertTrue(t > dureeMax && t <= dureeMax + 500);
    }
}
