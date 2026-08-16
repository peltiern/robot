package fr.roboteek.robot.memoire.courtterme;

import fr.roboteek.robot.memoire.RecognizedFace;
import fr.roboteek.robot.memoire.personne.Personne;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie ce que le robot retient des visages d'une image à l'autre, indépendamment d'OpenCV et
 * du matériel : aucune webcam, aucun modèle, aucune image.
 */
class SuiviDesVisagesTest {

    /** Rayon effectif pour un visage de 50 px de large : 0,7 × 50 = 35 px. */
    private static final int TAILLE_VISAGE = 50;

    private static final Personne NICOLAS = new Personne("id-nicolas", "Nicolas", null);
    private static final Personne EINSTEIN = new Personne("id-einstein", "Einstein", null);

    private static final long MAINTENANT = 100_000;

    @Test
    void aucunVisagePrecedentRenvoieNull() {
        RecognizedFace detecte = new RecognizedFace(100, 100, 50, 50);

        assertNull(SuiviDesVisages.trouverVisagePrecedentProche(null, detecte));
        assertNull(SuiviDesVisages.trouverVisagePrecedentProche(List.of(), detecte));
    }

    @Test
    void unVisagePrecedentProcheEstTrouve() {
        VisageSuivi precedent = visageSuivi(100, 100, 50, "id-amy", "Amy");
        // Centroïde décalé de quelques pixels seulement (petit mouvement entre 2 frames throttlées).
        RecognizedFace detecte = new RecognizedFace(105, 102, 50, 50);

        VisageSuivi trouve = SuiviDesVisages.trouverVisagePrecedentProche(List.of(precedent), detecte);

        assertSame(precedent, trouve);
    }

    @Test
    void unVisagePrecedentTropLoinNestPasTrouve() {
        VisageSuivi precedent = visageSuivi(100, 100, 50, null, null);
        RecognizedFace detecte = new RecognizedFace(300, 300, 50, 50);

        assertNull(SuiviDesVisages.trouverVisagePrecedentProche(List.of(precedent), detecte));
    }

    @Test
    void exactementAuRayonNestPasRetenu() {
        // Rayon d'un visage de 50 px : 0,7 × 50 = 35 px. Centroïdes distants d'exactement 35 px,
        // et la comparaison est stricte : ne doit pas matcher.
        VisageSuivi precedent = visageSuivi(100, 75, TAILLE_VISAGE, null, null);
        RecognizedFace detecte = new RecognizedFace(65, 75, TAILLE_VISAGE, TAILLE_VISAGE);

        assertNull(SuiviDesVisages.trouverVisagePrecedentProche(List.of(precedent), detecte));
    }

    @Test
    void leVisagePrecedentLePlusProcheEstRetenuParmiPlusieurs() {
        VisageSuivi loin = visageSuivi(0, 0, 50, "id-loin", "Loin");
        VisageSuivi proche = visageSuivi(102, 100, 50, "id-proche", "Proche");
        RecognizedFace detecte = new RecognizedFace(100, 100, 50, 50);

        VisageSuivi trouve = SuiviDesVisages.trouverVisagePrecedentProche(List.of(loin, proche), detecte);

        assertSame(proche, trouve);
    }

    /**
     * Le cas de la photo qu'on retire : un vrai visage prend sa place, au même endroit, mais il
     * est bien plus grand. Il ne doit pas hériter de son nom.
     */
    @Test
    void unVisageDeTailleTropDifferenteNestPasLeMeme() {
        VisageSuivi photo = visageSuivi(100, 100, 40, "id-einstein", "Einstein");
        RecognizedFace vraiVisage = new RecognizedFace(95, 98, 120, 120);

        assertNull(SuiviDesVisages.trouverVisagePrecedentProche(List.of(photo), vraiVisage));
    }

    @Test
    void uneIdentiteFraicheEstReutiliseeSansRelancerLaReconnaissance() {
        VisageSuivi precedent = identifie(100, 100, EINSTEIN, MAINTENANT - 500);
        RecognizedFace detecte = new RecognizedFace(102, 100, 50, 50);
        CompteurDeReconnaissances sface = new CompteurDeReconnaissances(NICOLAS);

        VisageSuivi resultat = SuiviDesVisages.identifier(detecte, precedent, MAINTENANT, sface);

        assertEquals("id-einstein", resultat.idPersonne());
        assertEquals(0, sface.appels, "SFace ne doit pas être relancé sur une identité fraîche");
    }

    /**
     * Le défaut du 2026-08-15 : sans péremption, l'identité héritée se recopiait indéfiniment.
     * Ici elle doit périmer, laisser SFace trancher, et le nom doit changer.
     */
    @Test
    void uneIdentiteHeriteeFinitParEtreReverifiee() {
        VisageSuivi precedent = identifie(100, 100, EINSTEIN, MAINTENANT - 1200);
        RecognizedFace detecte = new RecognizedFace(102, 100, 50, 50);
        CompteurDeReconnaissances sface = new CompteurDeReconnaissances(NICOLAS);

        VisageSuivi resultat = SuiviDesVisages.identifier(detecte, precedent, MAINTENANT, sface);

        assertEquals(1, sface.appels);
        assertEquals("id-nicolas", resultat.idPersonne());
        assertEquals("Nicolas", resultat.prenom());
        assertEquals(MAINTENANT, resultat.instantIdentification());
    }

    /**
     * Une identité recopiée ne doit jamais rajeunir : sinon elle ne périme plus jamais, et le
     * verrou est de retour.
     */
    @Test
    void uneIdentiteHeriteeNeRajeunitPas() {
        VisageSuivi precedent = identifie(100, 100, EINSTEIN, MAINTENANT - 500);
        RecognizedFace detecte = new RecognizedFace(102, 100, 50, 50);

        VisageSuivi resultat = SuiviDesVisages.identifier(detecte, precedent, MAINTENANT, () -> null);

        assertEquals(MAINTENANT - 500, resultat.instantIdentification());
    }

    /**
     * SFace échoue par intermittence près de son seuil : un échec isolé ne doit pas transformer
     * quelqu'un de connu en inconnu, sans quoi le robot lui redemande son prénom.
     */
    @Test
    void unEchecDeReconnaissanceNeRetrogradePasUnVisageConnu() {
        VisageSuivi precedent = identifie(100, 100, EINSTEIN, MAINTENANT - 1200);
        RecognizedFace detecte = new RecognizedFace(102, 100, 50, 50);

        VisageSuivi resultat = SuiviDesVisages.identifier(detecte, precedent, MAINTENANT, () -> null);

        assertTrue(resultat.estIdentifie());
        assertEquals("Einstein", resultat.prenom());
    }

    @Test
    void uneIdentiteJamaisReconfirmeeFinitParEtreAbandonnee() {
        VisageSuivi precedent = identifie(100, 100, EINSTEIN, MAINTENANT - 3500);
        RecognizedFace detecte = new RecognizedFace(102, 100, 50, 50);

        VisageSuivi resultat = SuiviDesVisages.identifier(detecte, precedent, MAINTENANT, () -> null);

        assertFalse(resultat.estIdentifie());
        assertNull(resultat.prenom());
    }

    @Test
    void unVisageSansPrecedentPasseParLaReconnaissance() {
        RecognizedFace detecte = new RecognizedFace(102, 100, 50, 50);
        CompteurDeReconnaissances sface = new CompteurDeReconnaissances(NICOLAS);

        VisageSuivi resultat = SuiviDesVisages.identifier(detecte, null, MAINTENANT, sface);

        assertEquals(1, sface.appels);
        assertEquals("id-nicolas", resultat.idPersonne());
    }

    @Test
    void unVisagePrecedentInconnuEstRetenteAChaqueCycle() {
        VisageSuivi precedent = new VisageSuivi(new RecognizedFace(100, 100, 50, 50), null, 0);
        RecognizedFace detecte = new RecognizedFace(102, 100, 50, 50);
        CompteurDeReconnaissances sface = new CompteurDeReconnaissances(NICOLAS);

        VisageSuivi resultat = SuiviDesVisages.identifier(detecte, precedent, MAINTENANT, sface);

        assertEquals(1, sface.appels);
        assertEquals("id-nicolas", resultat.idPersonne());
    }

    private static VisageSuivi visageSuivi(int x, int y, int taille, String idPersonne, String prenom) {
        RecognizedFace boite = new RecognizedFace(x, y, taille, taille);
        boite.setName(prenom);
        return new VisageSuivi(boite, idPersonne, 0);
    }

    private static VisageSuivi identifie(int x, int y, Personne personne, long instantIdentification) {
        RecognizedFace boite = new RecognizedFace(x, y, 50, 50);
        boite.setName(personne.prenom());
        return new VisageSuivi(boite, personne.id(), instantIdentification);
    }

    /** Compte les appels à SFace : l'économie qu'apporte le suivi est aussi ce qui se vérifie. */
    private static final class CompteurDeReconnaissances implements java.util.function.Supplier<Personne> {

        private final Personne personne;
        private int appels;

        private CompteurDeReconnaissances(Personne personne) {
            this.personne = personne;
        }

        @Override
        public Personne get() {
            appels++;
            return personne;
        }
    }
}
