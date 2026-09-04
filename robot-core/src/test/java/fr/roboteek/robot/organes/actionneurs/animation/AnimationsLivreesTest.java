package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les animations livrées avec le robot ({@code installation/Robot/Programme/animations}) doivent
 * rester lisibles et jouables.
 * <p>
 * Ce test est le garde-fou du <b>contrat JSON</b> : le modèle est fait de records, et renommer un
 * composant ou en ajouter un relirait ces fichiers sans erreur, le champ manquant à {@code null}.
 * C'est exactement l'accident qui a fait abandonner MapDB. Ici, la vérification est double —
 * le fichier se relit, et l'animation obtenue tient dans les butées du vrai robot.
 */
class AnimationsLivreesTest {

    private static final Path DOSSIER = Path.of("installation/Robot/Programme/animations");

    /**
     * Butées et vitesses de travail du robot, telles que {@code robot.properties} les règle.
     * <p>
     * Recopiées à la main plutôt que lues : la configuration se trouve à trois endroits — le
     * dépôt, le poste, le Jetson — et un test qui suivrait celle du poste passerait ou tomberait
     * selon la machine. Ces valeurs doivent donc être tenues à jour avec le fichier du dépôt.
     * <p>
     * Celles des yeux sont en <b>degrés d'œil</b> depuis le 2026-09-03 (cf. {@code TransmissionOeil}).
     */
    private static Map<Axe, LimitesMoteur> limitesDuRobot() {
        Map<Axe, LimitesMoteur> limites = new EnumMap<>(Axe.class);
        limites.put(Axe.COU_GAUCHE_DROITE, new LimitesMoteur(40, 200, -60, 60));
        limites.put(Axe.COU_HAUT_BAS, new LimitesMoteur(10, 200, -8, 7));
        limites.put(Axe.COU_MONTER_DESCENDRE, new LimitesMoteur(100, 200, -10, 60));
        limites.put(Axe.OEIL_GAUCHE, new LimitesMoteur(50, 75, -6.14, 31.06));
        limites.put(Axe.OEIL_DROIT, new LimitesMoteur(50, 75, -6.14, 31.06));
        return limites;
    }

    /**
     * Une animation sans version est refusée : avant la version 1, les pistes des yeux portaient
     * des unités moteur, et la tringlerie n'étant pas linéaire, les rejouer telles quelles
     * décalerait le geste d'un tiers sans qu'aucune trace ne l'explique.
     */
    @Test
    void lesAnimationsLivreesDisentDansQuelleUniteEllesSontEcrites() {
        BibliothequeDesAnimations bibliotheque = new BibliothequeDesAnimations(DOSSIER);

        for (String nom : bibliotheque.noms()) {
            assertTrue(bibliotheque.charger(nom).orElseThrow().versionLisible(),
                    nom + " : sans version, l'animation serait refusée au chargement");
        }
    }

    @Test
    void lesAnimationsLivreesSeRelisentEtNAvertissentDeRien() {
        assertTrue(Files.isDirectory(DOSSIER), "Dossier des animations livrées introuvable : " + DOSSIER);
        BibliothequeDesAnimations bibliotheque = new BibliothequeDesAnimations(DOSSIER);
        VerificateurAnimation verificateur = new VerificateurAnimation(limitesDuRobot());

        List<String> noms = bibliotheque.noms();
        assertFalse(noms.isEmpty(), "Aucune animation livrée");

        for (String nom : noms) {
            Animation animation = bibliotheque.charger(nom)
                    .orElseThrow(() -> new AssertionError("Animation illisible : " + nom));
            assertFalse(animation.pistes().isEmpty(), nom + " : aucune piste");
            assertEquals(List.of(), verificateur.controler(animation),
                    nom + " : une animation livrée doit être jouable telle quelle sur le robot");
        }
    }

    /**
     * Le monter/descendre est le seul axe qui peut amener la tête contre le corps : les valeurs
     * négatives la font descendre. Les animations livrées, jouées sans surveillance pour essayer
     * le lecteur, restent du bon côté.
     */
    @Test
    void aucuneAnimationLivreeNeFaitDescendreLaTeteVersLeCorps() {
        BibliothequeDesAnimations bibliotheque = new BibliothequeDesAnimations(DOSSIER);

        for (String nom : bibliotheque.noms()) {
            bibliotheque.charger(nom).orElseThrow().piste(Axe.COU_MONTER_DESCENDRE)
                    .ifPresent(piste -> piste.imagesCles().forEach(imageCle ->
                            assertTrue(imageCle.valeur() >= 0,
                                    nom + " : monter/descendre à " + imageCle.valeur()
                                            + "°, la tête irait vers le corps")));
        }
    }
}
