package fr.roboteek.robot.util.webcam;

import fr.roboteek.robot.memoire.RecognizedFace;
import fr.roboteek.robot.memoire.personne.Personne;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.geometry.euclidean.twod.Vector2D;

import java.util.List;
import java.util.function.Supplier;

/**
 * Classe utilitaire pour le suivi léger de visages entre deux frames throttlées,
 * par proximité de centroïde : permet à {@code CapteurVisionWebSocketGrpc} de
 * réutiliser une identité déjà trouvée sans relancer la reconnaissance (SFace, ~68 ms/visage).
 * <p>
 * Deux questions y sont traitées, qu'il ne faut surtout pas confondre — les avoir confondues est
 * l'origine du défaut du 2026-08-15 : <b>où</b> est le visage qu'on suivait (proximité de
 * centroïde), et <b>depuis quand</b> son identité n'a pas été vérifiée (péremption).
 */
public class SuiviVisageUtils {

    /**
     * Durée pendant laquelle une identité trouvée par SFace peut être réutilisée telle quelle,
     * sans relancer la reconnaissance.
     * <p>
     * <b>C'est ce qui empêche le suivi de devenir un verrou.</b> L'identité héritée conservant la
     * date de sa dernière confirmation par SFace — et non celle de sa recopie —, elle périme
     * forcément, et le visage repasse par la reconnaissance. Sans cette péremption, une étiquette
     * posée une fois se recopiait de cycle en cycle sans plus jamais être vérifiée : le
     * 2026-08-15 sur le robot, un visage ayant hérité du nom d'une photo voisine le gardait tant
     * que la personne restait dans le champ. Retirer la photo n'y changeait rien ; il fallait
     * sortir du champ et revenir.
     * <p>
     * Une seconde est un compromis : assez long pour ne relancer SFace qu'une fois sur trois
     * environ, assez court pour qu'une erreur ne survive pas à la phrase en cours.
     */
    private static final long DUREE_REUTILISATION_IDENTITE_MS = 1000;

    /**
     * Durée au-delà de laquelle une identité que SFace ne confirme plus est abandonnée.
     * <p>
     * Entre la péremption ci-dessus et cette échéance, le visage garde son nom bien que la
     * reconnaissance échoue : c'est délibéré, et c'est ce qui évite d'aborder quelqu'un de connu.
     * SFace oscille autour de son seuil et rend régulièrement « personne » sur un visage
     * parfaitement identifié la seconde d'avant ; rétrograder en inconnu au premier échec était
     * le défaut corrigé le 2026-08-12. Passé ce délai en revanche, l'obstination n'a plus de
     * sens : ce n'est probablement plus la même personne.
     */
    private static final long DUREE_MAX_IDENTITE_NON_CONFIRMEE_MS = 3000;

    private SuiviVisageUtils() {
    }

    /**
     * Donne son identité à un visage détecté, en trois temps.
     * <p>
     * D'abord l'identité du cycle précédent si elle est encore fraîche — c'est l'économie de
     * SFace, et la seule raison d'être du suivi. Sinon la reconnaissance, qui a le dernier mot :
     * c'est elle, et elle seule, qui pose ou corrige un nom. Et si elle ne rend rien, le nom
     * précédent est conservé un temps plutôt que rétrogradé en inconnu, parce qu'un échec isolé
     * de SFace est la règle et non l'exception.
     * <p>
     * Le point qui compte : une identité héritée <b>garde la date de sa dernière confirmation</b>.
     * C'est ce qui la fait périmer, donc revérifier, au lieu de se recopier indéfiniment.
     *
     * @param boite                 boîte du visage détecté, dont le prénom est renseigné ici
     * @param visagePrecedentProche visage suivi auquel le rattacher, {@code null} si aucun
     * @param maintenant            horodatage du cycle en cours
     * @param reconnaissance        lance SFace et rend la personne reconnue, {@code null} sinon.
     *                              Volontairement paresseux : c'est l'appel coûteux, et tout
     *                              l'intérêt du suivi est de ne pas le faire.
     */
    public static VisageSuivi identifier(RecognizedFace boite, VisageSuivi visagePrecedentProche,
                                         long maintenant, Supplier<Personne> reconnaissance) {
        boolean identiteHeritable = visagePrecedentProche != null && visagePrecedentProche.estIdentifie();
        if (identiteHeritable && visagePrecedentProche.ageIdentification(maintenant) < DUREE_REUTILISATION_IDENTITE_MS) {
            return heriter(boite, visagePrecedentProche);
        }

        Personne personne = reconnaissance.get();
        if (personne != null) {
            boite.setName(personne.prenom());
            return new VisageSuivi(boite, personne.id(), maintenant);
        }

        if (identiteHeritable && visagePrecedentProche.ageIdentification(maintenant) < DUREE_MAX_IDENTITE_NON_CONFIRMEE_MS) {
            return heriter(boite, visagePrecedentProche);
        }

        boite.setName(null);
        return new VisageSuivi(boite, null, 0);
    }

    /** Reprend l'identité du visage précédent <b>avec sa date d'origine</b>, qui la fera périmer. */
    private static VisageSuivi heriter(RecognizedFace boite, VisageSuivi visagePrecedent) {
        boite.setName(visagePrecedent.prenom());
        return new VisageSuivi(boite, visagePrecedent.idPersonne(), visagePrecedent.instantIdentification());
    }

    /**
     * Cherche, parmi {@code visagesPrecedents}, celui dont le centroïde est le plus
     * proche de {@code visageDetecte}, à condition que cette distance reste
     * strictement sous {@code distanceMaxPixels} <b>et</b> que les deux visages soient de
     * taille comparable.
     * <p>
     * La condition de taille n'est pas une précaution de principe : deux visages peuvent
     * occuper le même endroit de l'image à quelques instants d'intervalle sans être le même —
     * une photo qu'on retire et un vrai visage qui prend sa place, par exemple. Ils se
     * distinguent alors par la taille bien plus sûrement que par la position, et sans cette
     * condition le second héritait de l'identité du premier.
     *
     * @param distanceMaxPixels distance de centroïde à ne pas atteindre
     * @param rapportTailleMax  rapport de largeur maximal toléré entre les deux visages, dans un
     *                          sens comme dans l'autre (1.6 = jusqu'à 60 % plus large)
     * @return le visage précédent le plus proche, ou {@code null} si aucun ne convient
     * (ou si {@code visagesPrecedents} est vide/nul)
     */
    public static VisageSuivi trouverVisagePrecedentProche(List<VisageSuivi> visagesPrecedents, RecognizedFace visageDetecte,
                                                           double distanceMaxPixels, double rapportTailleMax) {
        if (CollectionUtils.isEmpty(visagesPrecedents)) {
            return null;
        }
        Vector2D centroideDetecte = visageDetecte.getCentroid();
        VisageSuivi plusProche = null;
        double distanceMin = distanceMaxPixels;
        for (VisageSuivi visagePrecedent : visagesPrecedents) {
            double distance = centroideDetecte.distance(visagePrecedent.boite().getCentroid());
            if (distance < distanceMin && taillesComparables(visageDetecte, visagePrecedent.boite(), rapportTailleMax)) {
                distanceMin = distance;
                plusProche = visagePrecedent;
            }
        }
        return plusProche;
    }

    private static boolean taillesComparables(RecognizedFace detecte, RecognizedFace precedent, double rapportTailleMax) {
        double largeurDetectee = detecte.getWidth();
        double largeurPrecedente = precedent.getWidth();
        if (largeurDetectee <= 0 || largeurPrecedente <= 0) {
            return false;
        }
        double rapport = Math.max(largeurDetectee, largeurPrecedente) / Math.min(largeurDetectee, largeurPrecedente);
        return rapport <= rapportTailleMax;
    }
}
