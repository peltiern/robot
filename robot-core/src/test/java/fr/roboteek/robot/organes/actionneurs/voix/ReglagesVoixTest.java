package fr.roboteek.robot.organes.actionneurs.voix;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les réglages de la voix, traduits en effets sox. */
class ReglagesVoixTest {

    /**
     * La garantie du chantier : sans retouche, la voix est celle du script validé le 2026-08-16,
     * au caractère près.
     */
    @Test
    void lOrigineRedonneExactementLeScriptWallE() {
        String script = "pitch -200 highpass 450 lowpass 3200 overdrive 12 8 chorus 0.7 0.9 45 0.5 0.4 2 -t "
                + "flanger 0 2 0 71 0.5 sin 25 gain -n -2";

        assertEquals(List.of(script.split(" ")), ReglagesVoix.ORIGINE.effetsSox());
    }

    /** Sur un poste en français, un nombre formaté à la légère s'écrit « 0,5 » : sox le refuse. */
    @Test
    void lesNombresGardentLeurPointQuelleQueSoitLaLangue() {
        Locale avant = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            List<String> effets = new ReglagesVoix(0, 0.85, 0, 20000, 0, 0.3, 0, 0).effetsSox();

            assertTrue(effets.contains("0.85"), effets.toString());
            assertTrue(effets.contains("0.15"), effets.toString());
            assertFalse(String.join(" ", effets).contains(","));
        } finally {
            Locale.setDefault(avant);
        }
    }

    @Test
    void uneVoixSansRetoucheNeGardeQueLaRenormalisation() {
        assertEquals(List.of("gain", "-n", "-2"), new ReglagesVoix(0, 1, 0, 20000, 0, 0, 0, 0).effetsSox());
    }

    @Test
    void leMetalEtLaModulationSAjoutentALaFin() {
        List<String> effets = new ReglagesVoix(0, 1, 0, 20000, 0, 0, 1, 60).effetsSox();

        assertEquals(List.of("echo", "0.8", "0.7", "5", "0.7", "synth", "sine", "amod", "60", "gain", "-n", "-2"), effets);
    }

    /** Le métal à moitié : la modulation laisse la moitié de la voix intacte, dessous. */
    @Test
    void laProfondeurDoseLaModulation() {
        List<String> effets = new ReglagesVoix(0, 1, 0, 20000, 0, 0, 0, 150, 0.5).effetsSox();

        assertEquals(List.of("synth", "sine", "amod", "150", "50", "gain", "-n", "-2"), effets);
    }

    /** Une voix adoptée avant la profondeur n'a pas ce champ : elle garde sa modulation entière. */
    @Test
    void uneProfondeurAbsenteVautLaModulationEntiere() {
        ReglagesVoix sansProfondeur = new ReglagesVoix(0, 1, 0, 20000, 0, 0, 0, 30, null);

        assertEquals(1.0, sansProfondeur.bornes().profondeur());
        assertEquals(List.of("synth", "sine", "amod", "30", "gain", "-n", "-2"), sansProfondeur.effetsSox());
    }

    @Test
    void uneProfondeurNulleNeModulePlus() {
        assertEquals(List.of("gain", "-n", "-2"), new ReglagesVoix(0, 1, 0, 20000, 0, 0, 0, 150, 0.0).effetsSox());
    }

    /** Venus d'une requête HTTP : un tempo nul ou un passe-bas négatif rendrait le robot muet. */
    @Test
    void desReglagesHorsLimitesSontRamenesDansCeQueSoxAccepte() {
        ReglagesVoix bornes = new ReglagesVoix(-50, 0, -10, -5, 999, 7, -1, Double.NaN).bornes();

        assertEquals(new ReglagesVoix(-12, 0.5, 0, 800, 40, 1, 0, 0, 1.0), bornes);
        assertEquals(0.0, new ReglagesVoix(0, 1, 0, 20000, 0, 0, 0, 150, -3.0).bornes().profondeur());
    }
}
