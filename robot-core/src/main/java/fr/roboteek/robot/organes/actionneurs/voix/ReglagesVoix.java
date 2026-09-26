package fr.roboteek.robot.organes.actionneurs.voix;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * La coloration de la voix du robot : ce que sox fait à la phrase que Piper vient de dire.
 * <p>
 * Elle remplace le script {@code synthesis_piper_walle.sh}, figé et réglable seulement en éditant
 * un fichier sur le Jetson puis en redémarrant le robot. {@link #ORIGINE} en est la traduction
 * exacte : tant qu'on ne touche à rien, la voix ne change pas.
 *
 * @param hauteur    décalage en demi-tons (négatif : plus grave)
 * @param debit      multiplie la vitesse d'élocution, sans changer la hauteur
 * @param passeHaut  coupe sous cette fréquence (Hz), 0 pour ne rien couper
 * @param passeBas   coupe au-dessus (Hz) : plus c'est bas, plus la voix sonne « petit haut-parleur »
 * @param grain      saturation, en dB de gain dans l'{@code overdrive}, 0 pour une voix propre
 * @param machine    chorus et flanger dosés ensemble, de 0 (humaine) à 1 (le Wall-E d'origine)
 * @param metal      écho très court qui fait résonner la voix comme dans une boîte, de 0 à 1
 * @param modulation fréquence (Hz) de la modulation : vers 30 Hz elle hache la voix, vers 150 Hz elle
 *                   la rend métallique ; 0 pour ne rien moduler
 * @param profondeur part de la voix que la modulation emporte, de 0 à 1 ; {@code null} vaut 1, la
 *                   modulation entière — c'est ce que faisait une voix adoptée avant ce réglage
 */
public record ReglagesVoix(double hauteur, double debit, double passeHaut, double passeBas,
                           double grain, double machine, double metal, double modulation, Double profondeur) {

    /** Modulation entière : les réglages d'avant la profondeur. */
    public ReglagesVoix(double hauteur, double debit, double passeHaut, double passeBas,
                        double grain, double machine, double metal, double modulation) {
        this(hauteur, debit, passeHaut, passeBas, grain, machine, metal, modulation, 1.0);
    }

    /** La voix validée à l'oreille le 2026-08-16, celle du script. */
    public static final ReglagesVoix ORIGINE = new ReglagesVoix(-2, 1, 450, 3200, 12, 1, 0, 0);

    /**
     * Les mêmes réglages, ramenés dans ce que sox accepte : ils arrivent d'une requête HTTP, et un
     * {@code tempo 0} ou un {@code lowpass} négatif ferait échouer {@code play} — le robot resterait
     * muet sans rien dire.
     */
    public ReglagesVoix bornes() {
        return new ReglagesVoix(
                borner(hauteur, -12, 12),
                borner(debit, 0.5, 2),
                borner(passeHaut, 0, 1500),
                borner(passeBas, 800, 20000),
                borner(grain, 0, 40),
                borner(machine, 0, 1),
                borner(metal, 0, 1),
                borner(modulation, 0, 200),
                profondeur == null ? 1.0 : borner(profondeur, 0, 1));
    }

    /**
     * Les effets à passer à {@code play}, après le nom du fichier. Un effet réglé à zéro est omis
     * plutôt que passé à vide : un {@code pitch 0} coûte du calcul sur le Jetson pour rien.
     */
    public List<String> effetsSox() {
        ReglagesVoix r = bornes();
        List<String> effets = new ArrayList<>();
        if (Math.abs(r.hauteur) >= 0.01) {
            effets.addAll(List.of("pitch", nombre(r.hauteur * 100)));
        }
        if (Math.abs(r.debit - 1) >= 0.01) {
            // -s : réglé pour la parole, sinon le tempo hache les syllabes.
            effets.addAll(List.of("tempo", "-s", nombre(r.debit)));
        }
        if (r.passeHaut >= 1) {
            effets.addAll(List.of("highpass", nombre(r.passeHaut)));
        }
        if (r.passeBas < 20000) {
            effets.addAll(List.of("lowpass", nombre(r.passeBas)));
        }
        if (r.grain >= 0.1) {
            effets.addAll(List.of("overdrive", nombre(r.grain), "8"));
        }
        if (r.machine >= 0.01) {
            effets.addAll(List.of("chorus", "0.7", "0.9", "45", nombre(0.5 * r.machine), "0.4", "2", "-t"));
            effets.addAll(List.of("flanger", "0", nombre(2 * r.machine), "0", "71", "0.5", "sin", "25"));
        }
        if (r.metal >= 0.01) {
            // 5 ms : un peigne dans les aigus, la résonance d'une boîte de conserve. Gain de sortie à
            // 0,7 : à 0,9, sox avertit que l'écho sature.
            effets.addAll(List.of("echo", "0.8", "0.7", "5", nombre(0.7 * r.metal)));
        }
        if (r.modulation >= 0.5 && r.profondeur >= 0.01) {
            effets.addAll(List.of("synth", "sine", "amod", nombre(r.modulation)));
            if (r.profondeur < 0.999) {
                // Le décalage de l'onde de synth dose la modulation : mesuré sur un son pur, un
                // décalage de x % laisse une profondeur de 100 − x %. Nicolas trouvait le métal à
                // 100 % trop difficile à comprendre ; à moitié, la voix reste dessous.
                effets.add(nombre(100 * (1 - r.profondeur)));
            }
        }
        // Renormalise : saturation, écho et modulation font varier le niveau d'un réglage à l'autre.
        effets.addAll(List.of("gain", "-n", "-2"));
        return effets;
    }

    private static double borner(double valeur, double min, double max) {
        return Double.isNaN(valeur) ? min : Math.min(max, Math.max(min, valeur));
    }

    /**
     * Toujours avec un point : {@code String.format} suit la langue du système, et sur un poste
     * français il écrirait {@code 0,5}, que sox ne comprend pas.
     */
    private static String nombre(double valeur) {
        return BigDecimal.valueOf(valeur).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
