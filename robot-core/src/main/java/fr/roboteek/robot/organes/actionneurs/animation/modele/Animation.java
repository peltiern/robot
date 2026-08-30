package fr.roboteek.robot.organes.actionneurs.animation.modele;

import java.util.List;
import java.util.Optional;

/**
 * Une animation écrite dans l'éditeur : ce que chaque axe fait, du début à la fin.
 * <p>
 * C'est aussi le format du fichier JSON enregistré dans {@code $ROBOT_HOME/animations} et celui
 * qui circule avec l'éditeur. Les noms de champs sont donc un contrat : les changer invalide les
 * animations déjà écrites.
 *
 * @param nom         nom de l'animation, qui est aussi celui de son fichier
 * @param dureeTotale durée de la timeline en millisecondes, indépendante de la dernière image-clé
 *                    (une animation peut finir sur une pause)
 * @param pistes      une par axe animé ; un axe absent n'est pas commandé du tout, ce qui n'est pas
 *                    la même chose qu'un axe tenu à zéro
 * @param sons        emplacement réservé, toujours vide aujourd'hui (voir {@link SonDeclenche})
 */
public record Animation(String nom, long dureeTotale, List<Piste> pistes, List<SonDeclenche> sons) {

    public Animation {
        pistes = pistes == null ? List.of() : List.copyOf(pistes);
        sons = sons == null ? List.of() : List.copyOf(sons);
    }

    /** La piste d'un axe, si l'animation le commande. */
    public Optional<Piste> piste(Axe axe) {
        return pistes.stream().filter(piste -> piste.axe() == axe).findFirst();
    }

    /**
     * La même animation sous un autre nom. Sert au renommage par la couche REST, où le nom fait
     * foi dans l'URL et non dans le corps de la requête.
     */
    public Animation avecNom(String nouveauNom) {
        return new Animation(nouveauNom, dureeTotale, pistes, sons);
    }

    /**
     * La même animation réduite aux pistes qui commandent réellement quelque chose. L'éditeur
     * envoie les pistes désactivées comme les autres ; les garder ferait tenir un axe à zéro alors
     * que l'intention était de ne pas y toucher.
     */
    public Animation sansPistesVides() {
        return new Animation(nom, dureeTotale, pistes.stream().filter(piste -> !piste.estVide()).toList(), sons);
    }
}
