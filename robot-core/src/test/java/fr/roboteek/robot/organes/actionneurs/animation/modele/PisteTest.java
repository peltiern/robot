package fr.roboteek.robot.organes.actionneurs.animation.modele;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PisteTest {

    /**
     * L'interpolation cherche l'intervalle qui encadre un instant en parcourant la liste dans
     * l'ordre : des images-clés désordonnées lui feraient rendre n'importe quoi. Rien ne garantit
     * l'ordre d'une piste qui arrive du réseau ou d'un fichier édité à la main.
     */
    @Test
    void lesImagesClesSontTrieesALaConstruction() {
        Piste piste = new Piste(Axe.OEIL_GAUCHE, 40, 60, List.of(
                new ImageCle(1000, 3),
                new ImageCle(0, 1),
                new ImageCle(500, 2)));

        assertEquals(List.of(0L, 500L, 1000L), piste.imagesCles().stream().map(ImageCle::instant).toList());
    }

    @Test
    void lesImagesClesSontImmuables() {
        List<ImageCle> source = new ArrayList<>(List.of(new ImageCle(0, 1)));
        Piste piste = new Piste(Axe.OEIL_GAUCHE, 40, 60, source);

        source.add(new ImageCle(500, 9));

        assertEquals(1, piste.imagesCles().size(), "La piste ne doit pas suivre la liste d'origine");
        assertThrows(UnsupportedOperationException.class, () -> piste.imagesCles().add(new ImageCle(1, 1)));
    }

    @Test
    void unePisteSansImageCleEstVide() {
        assertTrue(new Piste(Axe.OEIL_DROIT, 40, 60, List.of()).estVide());
        assertTrue(new Piste(Axe.OEIL_DROIT, 40, 60, null).estVide());
    }

    @Test
    void uneImageCleSansVitesseRetombeSurCelleDeLaPiste() {
        Piste piste = new Piste(Axe.COU_GAUCHE_DROITE, 40, 200, List.of());

        assertEquals(40, piste.vitesseDe(new ImageCle(0, 0)));
        assertEquals(200, piste.accelerationDe(new ImageCle(0, 0)));
        assertEquals(15, piste.vitesseDe(new ImageCle(0, 0, 15d, null)));
        assertEquals(80, piste.accelerationDe(new ImageCle(0, 0, null, 80d)));
    }
}
