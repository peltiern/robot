package fr.roboteek.robot.securite;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie le calcul des trois états de santé, horloge sous contrôle : aucun matériel, aucun
 * contexte Spring, et surtout aucune attente réelle pour faire vieillir un battement.
 */
class RegistreSanteTest {

    private static final long DELAI_SILENCE_MS = 3000;

    private final AtomicLong maintenant = new AtomicLong(100_000);

    /** Registre d'un robot déjà démarré : le cas courant, où les organes sont jugeables. */
    private RegistreSante registre(OrganeSurveille... organes) {
        RegistreSante registre = new RegistreSante(List.of(organes), maintenant::get);
        registre.demarrageCompletA(maintenant.get());
        return registre;
    }

    private SanteOrgane premier(RegistreSante registre) {
        return registre.releve(DELAI_SILENCE_MS).getFirst();
    }

    @Test
    void unOrganeHorsServiceEstEteintEtNonMuet() {
        OrganeFactice vision = new OrganeFactice("vision", false).enService(false);

        SanteOrgane sante = premier(registre(vision));

        // C'est tout l'intérêt du troisième état : la vision désactivée par configuration ne doit
        // pas se lire comme une panne, sans quoi la pastille rouge permanente perd tout son sens.
        assertEquals(EtatSante.ETEINT, sante.etat());
        assertNull(sante.ageMillis());
    }

    @Test
    void unOrganeQuiBatEstVivantEtPorteSonAge() {
        OrganeFactice cou = new OrganeFactice("cou", true).battuA(maintenant.get() - 40);

        SanteOrgane sante = premier(registre(cou));

        assertEquals(EtatSante.VIVANT, sante.etat());
        assertEquals(40L, sante.ageMillis());
        assertTrue(sante.surveille());
    }

    @Test
    void unOrganeSilencieuxAuDelaDuDelaiEstMuet() {
        OrganeFactice cou = new OrganeFactice("cou", true).battuA(maintenant.get() - 10_000);
        RegistreSante registre = registre(cou);

        // Premier relevé : l'organe vient d'être vu, le délai de grâce court encore.
        assertEquals(EtatSante.VIVANT, premier(registre).etat());

        maintenant.addAndGet(DELAI_SILENCE_MS + 1);

        assertEquals(EtatSante.MUET, premier(registre).etat());
    }

    @Test
    void unOrganeQuiVientDeDemarrerNestPasMuetAvantLeDelai() {
        // Jamais battu : sans le délai de grâce, son âge vaudrait la date Unix entière et il
        // serait déclaré mort dès le premier relevé, avant même d'avoir eu le temps de vivre.
        OrganeFactice manette = new OrganeFactice("manette", true);
        RegistreSante registre = registre(manette);

        SanteOrgane sante = premier(registre);

        assertEquals(EtatSante.VIVANT, sante.etat());
        assertNull(sante.ageMillis(), "Aucun battement : afficher un âge serait un mensonge");
    }

    @Test
    void leDelaiDeGraceRepartDeZeroApresUnRetourEnService() {
        OrganeFactice manette = new OrganeFactice("manette", true);
        RegistreSante registre = registre(manette);
        premier(registre);

        // Manette débranchée un long moment, puis rebranchée.
        manette.enService(false);
        assertEquals(EtatSante.ETEINT, premier(registre).etat());
        maintenant.addAndGet(60_000);
        manette.enService(true);

        assertEquals(EtatSante.VIVANT, premier(registre).etat(),
                "Un organe qui revient doit avoir droit au même délai de grâce qu'au démarrage");
    }

    @Test
    void aucunOrganeNestJugeTantQueLApplicationDemarre() {
        // Les battements viennent de tâches @Scheduled, que Spring ne lance qu'après avoir démarré
        // tous les organes : entre les deux, un organe est en service sans pouvoir battre. La
        // conduite différentielle, démarrée cinq secondes avant la fin du démarrage, était déclarée
        // muette avant même que le robot ne soit prêt.
        OrganeFactice chenille = new OrganeFactice("chenille-gauche", true);
        RegistreSante registre = new RegistreSante(List.of(chenille), maintenant::get);
        premier(registre);

        maintenant.addAndGet(DELAI_SILENCE_MS + 1);

        assertEquals(EtatSante.VIVANT, premier(registre).etat(),
                "Un organe démarré avant l'ordonnanceur ne peut pas être jugé muet");
    }

    @Test
    void seulUnOrganeEnServicePeutFaireBouger() {
        OrganeFactice chenilles = new OrganeFactice("chenilles", true).enMouvement(true);
        RegistreSante registre = registre(chenilles);

        assertTrue(registre.quelqueChoseBouge());

        chenilles.enService(false);

        assertFalse(registre.quelqueChoseBouge(),
                "Un organe arrêté ne fait rien bouger, quoi qu'il ait mémorisé");
    }
}
