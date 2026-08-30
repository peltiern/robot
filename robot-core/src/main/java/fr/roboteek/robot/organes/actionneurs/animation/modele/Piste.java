package fr.roboteek.robot.organes.actionneurs.animation.modele;

import java.util.Comparator;
import java.util.List;

/**
 * Ce qu'un axe fait pendant toute la durée de l'animation : une suite d'images-clés, et les
 * vitesse et accélération à appliquer quand une image-clé n'en impose pas.
 *
 * @param axe                    l'axe piloté
 * @param vitesseParDefaut       vitesse en °/s appliquée aux images-clés qui n'en portent pas
 * @param accelerationParDefaut  accélération en °/s² appliquée de même
 * @param imagesCles             les points posés, <b>toujours triés par instant croissant</b>
 */
public record Piste(Axe axe, double vitesseParDefaut, double accelerationParDefaut, List<ImageCle> imagesCles) {

    public Piste {
        // Tri fait une fois ici, et non à chaque lecture : l'interpolation cherche l'intervalle qui
        // encadre un instant en parcourant la liste dans l'ordre, et une piste qui arrive du réseau
        // n'offre aucune garantie là-dessus. Copie immuable au passage — une piste circule entre le
        // lecteur, le contrôle de faisabilité et la couche REST, sans propriétaire désigné.
        imagesCles = imagesCles == null
                ? List.of()
                : imagesCles.stream().sorted(Comparator.comparingLong(ImageCle::instant)).toList();
    }

    /** Vitesse à tenir pour rejoindre cette image-clé : la sienne si elle en porte une, sinon celle de la piste. */
    public double vitesseDe(ImageCle imageCle) {
        return imageCle.vitesse() != null ? imageCle.vitesse() : vitesseParDefaut;
    }

    /** Accélération à appliquer pour rejoindre cette image-clé (voir {@link #vitesseDe}). */
    public double accelerationDe(ImageCle imageCle) {
        return imageCle.acceleration() != null ? imageCle.acceleration() : accelerationParDefaut;
    }

    /** Une piste sans aucun point ne commande rien ; elle est ignorée plutôt que traitée comme un zéro. */
    public boolean estVide() {
        return imagesCles.isEmpty();
    }
}
