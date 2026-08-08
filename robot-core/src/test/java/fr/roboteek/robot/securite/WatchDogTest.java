package fr.roboteek.robot.securite;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la décision du watchdog — la partie qui compte, puisqu'un déclenchement à tort le
 * ferait débrancher. Horloge sous contrôle, aucun matériel : on fait vieillir les battements à la
 * main plutôt que d'attendre trois secondes par cas de test.
 */
class WatchDogTest {

    /** Bien au-delà du délai de silence configuré par défaut (3 s). */
    private static final long SILENCE_FRANC_MS = 30_000;

    private final AtomicLong maintenant = new AtomicLong(1_000_000);
    private final ApplicationEventPublisher publicationMuette = evenement -> {
    };
    private final ArretUrgence arretUrgence = new ArretUrgence(publicationMuette);

    private WatchDog watchDog(OrganeSurveille... organes) {
        RegistreSante registre = new RegistreSante(List.of(organes), maintenant::get);
        // Robot déjà démarré : sans ça, aucun organe n'est jugeable (cf. RegistreSanteTest).
        registre.demarrageCompletA(maintenant.get());
        WatchDog watchDog = new WatchDog(registre, arretUrgence, publicationMuette);
        // Premier tour : les organes entrent en service et bénéficient du délai de grâce.
        watchDog.verifier();
        maintenant.addAndGet(SILENCE_FRANC_MS);
        return watchDog;
    }

    @Test
    void unOrganeDuMouvementMuetPendantQueLeRobotBougeCoupeLesMoteurs() {
        // Le scénario qui justifie tout : le robot roule, et la manette — seule à pouvoir envoyer
        // le STOPPER qui l'arrêterait, bouton d'arrêt d'urgence compris — a cessé de répondre.
        OrganeFactice manette = new OrganeFactice("manette", true);
        OrganeFactice chenilles = new OrganeFactice("chenilles", true).enMouvement(true);

        WatchDog watchDog = watchDog(manette, chenilles);
        chenilles.battuA(maintenant.get());

        watchDog.verifier();

        assertTrue(arretUrgence.estActif());
    }

    @Test
    void unOrganeMuetSurUnRobotImmobileNeCoupeRien() {
        OrganeFactice manette = new OrganeFactice("manette", true);
        OrganeFactice chenilles = new OrganeFactice("chenilles", true).enMouvement(false);

        WatchDog watchDog = watchDog(manette, chenilles);
        chenilles.battuA(maintenant.get());

        watchDog.verifier();

        assertFalse(arretUrgence.estActif(),
                "Couper les moteurs d'un robot à l'arrêt n'apporte rien et coûte un réarmement");
    }

    @Test
    void unCapteurMuetNeCoupeRienMemeSiLeRobotBouge() {
        // Périmètre étroit : le micro ou la vision peuvent mourir sans que les moteurs y soient
        // pour quoi que ce soit. Leur silence se paie d'une pastille, pas d'un arrêt d'urgence.
        OrganeFactice micro = new OrganeFactice("micro", false);
        OrganeFactice chenilles = new OrganeFactice("chenilles", true).enMouvement(true);

        WatchDog watchDog = watchDog(micro, chenilles);
        chenilles.battuA(maintenant.get());

        watchDog.verifier();

        assertFalse(arretUrgence.estActif());
    }

    @Test
    void unOrganeEteintNeCoupeRien() {
        // Manette non branchée : elle ne bat pas, et c'est normal.
        OrganeFactice manette = new OrganeFactice("manette", true).enService(false);
        OrganeFactice chenilles = new OrganeFactice("chenilles", true).enMouvement(true);

        WatchDog watchDog = watchDog(manette, chenilles);
        chenilles.battuA(maintenant.get());

        watchDog.verifier();

        assertFalse(arretUrgence.estActif());
    }

    @Test
    void unEpisodeDeSilenceNeProduitQuUneSeuleLigne() {
        // Le registre doit mémoriser TOUS les muets au premier passage. Sinon l'un d'eux paraît
        // nouveau au tour suivant et la ligne « rien ne bouge » est réémise autant de fois qu'il y
        // a d'organes muets — deux fois pour « Cou, Yeux », comme observé sur le robot le
        // 2026-08-08.
        WatchDog watchDog = watchDog();
        List<SanteOrgane> muets = List.of(muet("cou", "Cou"), muet("yeux", "Yeux"));

        assertTrue(watchDog.enregistrerNouveauxMuets(muets));
        assertFalse(watchDog.enregistrerNouveauxMuets(muets),
                "Un même épisode de silence ne doit être signalé qu'une fois");
    }

    private static SanteOrgane muet(String id, String libelle) {
        return new SanteOrgane(id, libelle, NatureOrgane.ACTIONNEUR, EtatSante.MUET, SILENCE_FRANC_MS, true);
    }

    @Test
    void unRobotEnBonneSanteNestJamaisCoupe() {
        OrganeFactice manette = new OrganeFactice("manette", true);
        OrganeFactice chenilles = new OrganeFactice("chenilles", true).enMouvement(true);

        WatchDog watchDog = watchDog(manette, chenilles);
        manette.battuA(maintenant.get());
        chenilles.battuA(maintenant.get());

        watchDog.verifier();

        assertFalse(arretUrgence.estActif());
    }
}
