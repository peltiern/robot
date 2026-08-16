package fr.roboteek.robot.memoire.courtterme;

import fr.roboteek.robot.memoire.RecognizedFace;
import fr.roboteek.robot.memoire.personne.Personne;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Ce que le robot retient des visages <b>d'une image à l'autre</b> : lequel est le même qu'avant,
 * et quelle identité reste valable.
 * <p>
 * Mémoire court terme, et non perception : le calcul appartient aux services de vision (YuNet
 * détecte, SFace compare), l'acquisition à l'organe. Ici on ne fait que se souvenir — assez pour
 * enjamber les trous de détection et pour s'éviter des reconnaissances inutiles.
 * <p>
 * <b>L'image n'entre jamais ici.</b> SFace en a besoin, mais le {@code Mat} est unique et réécrit
 * à chaque lecture de la webcam : le faire circuler obligerait à le cloner. L'organe passe donc
 * une fonction de reconnaissance qui, elle, tient l'image ; ce qu'on manipule ici se limite à des
 * boîtes englobantes et à des identités.
 */
@Component
public class SuiviDesVisages {

    /**
     * Rayon de suivi, <b>exprimé en largeurs du visage détecté</b> : sous cette distance de
     * centroïde, un visage est considéré comme le même qu'à la frame précédente et on réutilise
     * son nom sans relancer SFace (alignCrop + feature + comparaison à toute la base coûtent
     * ~68 ms par visage, voir fr.roboteek.robot.poc.FaceRecognitionPoc).
     * <p>
     * Relatif et non absolu, parce que c'est la seule échelle qui ait un sens : un visage proche
     * occupe 200 px et se déplace de plusieurs dizaines de pixels d'une frame à l'autre, un
     * visage lointain en occupe 40 et bouge d'autant moins. Les valeurs absolues essayées avant
     * (40 px, puis 80) étaient trop serrées dans un cas et trop larges dans l'autre.
     */
    private static final double RAYON_SUIVI_EN_LARGEURS_VISAGE = 0.7;

    /**
     * Écart de taille au-delà duquel deux visages proches ne sont pas le même : ce qui sépare une
     * photo qu'on retire d'un visage qui prend sa place, c'est la taille, pas la position.
     */
    private static final double RAPPORT_TAILLE_MAX = 1.6;

    /**
     * Durée pendant laquelle les visages du dernier cycle restent une référence de <b>position</b>
     * valable, même si la détection n'a rien vu entre-temps.
     * <p>
     * <b>C'est ce qui permet d'enjamber les trous de détection</b>, mesurés de 100 à 250 ms sur le
     * robot. Sans elle, la mémoire du suivi était perdue à chaque clignotement, la personne
     * redevenait un visage tout neuf, et une présence d'inconnu se constituait en parallèle de
     * quelqu'un pourtant reconnu — assez pour déclencher une présentation. Constaté le 2026-08-12
     * sur Einstein, reconnu puis abordé.
     * <p>
     * Elle ne dit rien de la <b>fraîcheur</b> des identités portées par ces visages : c'est
     * {@link #DUREE_REUTILISATION_IDENTITE_MS} qui s'en charge, et confondre les deux était le
     * défaut de la première version.
     */
    private static final long REMANENCE_POSITIONS_MS = 1500;

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

    /** Visages identifiés au dernier cycle utile : la référence du suivi. */
    private List<VisageSuivi> derniersVisagesIdentifies;

    /** Instant qui date {@link #derniersVisagesIdentifies}. */
    private long instantDerniersVisagesIdentifies;

    /** Tous les visages du dernier cycle, identifiés ou non : ce que le robot a vu en dernier. */
    private List<VisageSuivi> derniersVisages = List.of();

    /**
     * Rattache les visages qui viennent d'être détectés à ce qu'on savait, et leur donne une
     * identité — en relançant la reconnaissance seulement quand il le faut.
     *
     * @param visagesDetectes ce que la détection vient de trouver
     * @param reconnaissance  lance SFace sur un visage et rend la personne, {@code null} sinon.
     *                        Volontairement paresseux, et fourni par l'appelant : c'est lui qui
     *                        tient l'image, et c'est l'appel coûteux qu'on cherche à éviter.
     * @return les visages du cycle, avec leur identité
     */
    public synchronized List<VisageSuivi> suivre(List<VisageDetecte> visagesDetectes,
                                                 Function<VisageDetecte, Personne> reconnaissance) {
        long maintenant = System.currentTimeMillis();
        List<VisageSuivi> visagesPrecedents = positionsEncoreUtilisables(maintenant);
        List<VisageSuivi> visagesSuivis = new ArrayList<>();
        for (VisageDetecte visageDetecte : visagesDetectes) {
            RecognizedFace boite = new RecognizedFace(
                    visageDetecte.x(), visageDetecte.y(), visageDetecte.width(), visageDetecte.height());
            VisageSuivi precedent = trouverVisagePrecedentProche(visagesPrecedents, boite);
            visagesSuivis.add(identifier(boite, precedent, maintenant, () -> reconnaissance.apply(visageDetecte)));
        }

        // Seuls les visages identifiés servent de référence — un précédent sans identité n'évite
        // aucun calcul — et un cycle qui n'identifie personne ne doit pas effacer ce qu'on savait :
        // c'est précisément le trou de détection qu'il faut enjamber.
        List<VisageSuivi> visagesIdentifies = visagesSuivis.stream().filter(VisageSuivi::estIdentifie).toList();
        if (!visagesIdentifies.isEmpty()) {
            derniersVisagesIdentifies = visagesIdentifies;
            instantDerniersVisagesIdentifies = maintenant;
        }
        derniersVisages = visagesSuivis;
        return visagesSuivis;
    }

    /** Les visages du dernier cycle, pour qui veut les afficher. Jamais {@code null}. */
    public synchronized List<VisageSuivi> derniersVisages() {
        return derniersVisages;
    }

    /**
     * Oublie tout. Appelé quand la reconnaissance devient indisponible : mieux vaut repartir de
     * zéro que suivre à partir d'un état dont on ne sait plus s'il est à jour.
     */
    public synchronized void oublierTout() {
        derniersVisagesIdentifies = null;
        instantDerniersVisagesIdentifies = 0;
        derniersVisages = List.of();
    }

    /** Positions du dernier cycle utile, tant qu'elles ne sont pas trop vieilles pour servir. */
    private List<VisageSuivi> positionsEncoreUtilisables(long maintenant) {
        if (derniersVisagesIdentifies == null
                || maintenant - instantDerniersVisagesIdentifies > REMANENCE_POSITIONS_MS) {
            return null;
        }
        return derniersVisagesIdentifies;
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
     */
    static VisageSuivi identifier(RecognizedFace boite, VisageSuivi visagePrecedentProche,
                                  long maintenant, java.util.function.Supplier<Personne> reconnaissance) {
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
     * Cherche, parmi les visages retenus, celui dont le centroïde est le plus proche du visage
     * détecté — à condition qu'il soit assez près <b>et</b> de taille comparable.
     * <p>
     * La condition de taille n'est pas une précaution de principe : deux visages peuvent occuper
     * le même endroit de l'image à quelques instants d'intervalle sans être le même — une photo
     * qu'on retire et un vrai visage qui prend sa place, par exemple. Ils se distinguent alors par
     * la taille bien plus sûrement que par la position.
     */
    static VisageSuivi trouverVisagePrecedentProche(List<VisageSuivi> visagesPrecedents, RecognizedFace visageDetecte) {
        if (CollectionUtils.isEmpty(visagesPrecedents)) {
            return null;
        }
        Vector2D centroideDetecte = visageDetecte.getCentroid();
        VisageSuivi plusProche = null;
        double distanceMin = RAYON_SUIVI_EN_LARGEURS_VISAGE * visageDetecte.getWidth();
        for (VisageSuivi visagePrecedent : visagesPrecedents) {
            double distance = centroideDetecte.distance(visagePrecedent.boite().getCentroid());
            if (distance < distanceMin && taillesComparables(visageDetecte, visagePrecedent.boite())) {
                distanceMin = distance;
                plusProche = visagePrecedent;
            }
        }
        return plusProche;
    }

    private static boolean taillesComparables(RecognizedFace detecte, RecognizedFace precedent) {
        double largeurDetectee = detecte.getWidth();
        double largeurPrecedente = precedent.getWidth();
        if (largeurDetectee <= 0 || largeurPrecedente <= 0) {
            return false;
        }
        return Math.max(largeurDetectee, largeurPrecedente) / Math.min(largeurDetectee, largeurPrecedente) <= RAPPORT_TAILLE_MAX;
    }
}
