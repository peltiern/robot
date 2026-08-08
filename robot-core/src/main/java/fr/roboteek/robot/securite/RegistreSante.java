package fr.roboteek.robot.securite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Registre des battements des organes : la donnée unique que lisent, chacun de son côté, le
 * {@link WatchDog} (qui coupe les moteurs) et l'interface (qui affiche des pastilles d'état).
 * <p>
 * Un même relevé pour les deux usages, et non deux mécanismes parallèles : les indicateurs de
 * l'interface sont exactement ce sur quoi le watchdog raisonne, ce qui rend son
 * déclenchement lisible après coup — la pastille était déjà passée au rouge.
 * <p>
 * Le registre ne stocke aucun état de santé : il interroge les organes ({@link OrganeSurveille})
 * à la demande et calcule. La seule chose qu'il mémorise, c'est <b>l'instant où chaque organe est
 * entré en service</b> : sans ça, un organe qui vient de démarrer et n'a pas encore eu le temps de
 * battre afficherait un âge égal à la date Unix, et serait déclaré muet dès la première seconde.
 */
@Component
public class RegistreSante {

    /** Organes qui donnent signe de vie, collectés par Spring. */
    private final List<OrganeSurveille> organes;

    /** Instant d'entrée en service, par organe. Effacé dès qu'un organe repasse hors service. */
    private final Map<String, Long> entreesEnService = new ConcurrentHashMap<>();

    /** Horloge, injectable pour que les tests puissent vieillir un battement sans attendre. */
    private final LongSupplier horloge;

    /** Instant de fin du démarrage de l'application, ou {@code 0} tant qu'elle démarre encore. */
    private volatile long instantDemarrageComplet = 0L;

    @Autowired
    public RegistreSante(List<OrganeSurveille> organes) {
        this(organes, System::currentTimeMillis);
    }

    RegistreSante(List<OrganeSurveille> organes, LongSupplier horloge) {
        this.organes = organes;
        this.horloge = horloge;
    }

    /**
     * Santé de tous les organes surveillés, à cet instant.
     *
     * @param delaiSilenceMillis au-delà de quel âge un battement est considéré comme perdu
     */
    public List<SanteOrgane> releve(long delaiSilenceMillis) {
        long maintenant = horloge.getAsLong();
        List<SanteOrgane> releve = new ArrayList<>(organes.size());
        for (OrganeSurveille organe : organes) {
            releve.add(sante(organe, maintenant, delaiSilenceMillis));
        }
        return releve;
    }

    /**
     * Ancre le délai de grâce à la fin du démarrage de l'application.
     * <p>
     * Les battements du cou, des yeux et des chenilles sont émis depuis des tâches
     * {@code @Scheduled}, et Spring ne lance l'ordonnanceur qu'<b>après</b> le rafraîchissement du
     * contexte, donc après le {@code start()} de tous les organes. Entre les deux, un organe est en
     * service et pourtant hors d'état de battre. La conduite différentielle, démarrée cinq secondes
     * avant la fin du démarrage, était ainsi déclarée muette avant même que le robot ne soit prêt
     * (constaté sur le robot le 2026-08-08). Aucun organe n'est donc jugé tant que l'application
     * n'a pas fini de démarrer.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void surDemarrageComplet() {
        demarrageCompletA(horloge.getAsLong());
    }

    /** Visible pour les tests, qui n'ont pas de contexte Spring pour publier l'évènement. */
    void demarrageCompletA(long instant) {
        instantDemarrageComplet = instant;
    }

    /**
     * Vrai si au moins un organe surveillé fait effectivement bouger quelque chose. Garde-fou du
     * watchdog : un organe muet sur un robot immobile est un incident à journaliser, pas une
     * urgence.
     */
    public boolean quelqueChoseBouge() {
        return organes.stream().anyMatch(organe -> organe.enService() && organe.enMouvement());
    }

    private SanteOrgane sante(OrganeSurveille organe, long maintenant, long delaiSilenceMillis) {
        String id = organe.idOrgane();
        if (!organe.enService()) {
            entreesEnService.remove(id);
            return new SanteOrgane(id, organe.libelleOrgane(), organe.nature(), EtatSante.ETEINT,
                    null, organe.provoqueUnMouvement());
        }

        long entreeEnService = entreesEnService.computeIfAbsent(id, cle -> maintenant);
        long dernierBattement = organe.dernierBattement();
        // Un organe qui vient d'entrer en service n'a pas encore battu : on compte son silence à
        // partir de son démarrage, ce qui lui laisse le même délai de grâce qu'aux autres — ou à
        // partir de la fin du démarrage de l'application si elle est plus tardive, l'ordonnanceur
        // qui porte les battements ne tournant pas avant (voir surDemarrageComplet()).
        long ancrage = instantDemarrageComplet == 0L ? maintenant : instantDemarrageComplet;
        long depuis = Math.max(Math.max(dernierBattement, entreeEnService), ancrage);
        long age = Math.max(0, maintenant - depuis);

        EtatSante etat = age > delaiSilenceMillis ? EtatSante.MUET : EtatSante.VIVANT;
        // Âge affiché seulement s'il y a eu un vrai battement : « 3 s » alors que l'organe n'a
        // jamais rien émis serait un mensonge poli.
        Long ageAffiche = dernierBattement > 0 ? maintenant - dernierBattement : null;
        return new SanteOrgane(id, organe.libelleOrgane(), organe.nature(), etat, ageAffiche,
                organe.provoqueUnMouvement());
    }
}
