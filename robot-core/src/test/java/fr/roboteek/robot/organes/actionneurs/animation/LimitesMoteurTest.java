package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimitesMoteurTest {

    /** Réglages réels du robot, pour que le test dise quelque chose du vrai matériel. */
    private static PhidgetsConfig configurationDuRobot() {
        PhidgetsConfig configuration = mock(PhidgetsConfig.class);
        when(configuration.eyeLeftSpeed()).thenReturn(40d);
        when(configuration.eyeLeftAcceleration()).thenReturn(60d);
        when(configuration.eyeRightSpeed()).thenReturn(40d);
        when(configuration.eyeRightAcceleration()).thenReturn(60d);
        when(configuration.eyePositionMin()).thenReturn(-5d);
        when(configuration.eyePositionMax()).thenReturn(20d);
        when(configuration.neckLeftRightMotorSpeed()).thenReturn(40d);
        when(configuration.neckLeftRightMotorAcceleration()).thenReturn(200d);
        when(configuration.neckLeftRightMotorInitialPosition()).thenReturn(95d);
        when(configuration.neckLeftRightMotorMinPosition()).thenReturn(35d);
        when(configuration.neckLeftRightMotorMaxPosition()).thenReturn(155d);
        when(configuration.neckTiltMotorSpeed()).thenReturn(10d);
        when(configuration.neckTiltMotorAcceleration()).thenReturn(200d);
        when(configuration.neckTiltMotorInitialPosition()).thenReturn(67d);
        when(configuration.neckTiltMotorMinPosition()).thenReturn(60d);
        when(configuration.neckTiltMotorMaxPosition()).thenReturn(75d);
        when(configuration.neckUpDownMotorSpeed()).thenReturn(100d);
        when(configuration.neckUpDownMotorAcceleration()).thenReturn(200d);
        when(configuration.neckUpDownMotorInitialPosition()).thenReturn(140d);
        when(configuration.neckUpDownMotorMinPosition()).thenReturn(80d);
        when(configuration.neckUpDownMotorMaxPosition()).thenReturn(150d);
        return configuration;
    }

    /**
     * Les butées moteur passent par la <b>transmission de l'axe</b>, et le rapport n'est pas 1.
     * <p>
     * Elles étaient converties à la main par {@code init - positionMoteur}, ce qui supposait un
     * rapport unitaire et un signe négatif partout. Résultat : la timeline laissait dessiner des
     * courbes en unités moteur pendant que le robot les jouait en degrés, et l'inclinaison passait
     * pour « à peine 15° de course » alors qu'elle en fait 75. Se tromper de sens ou d'échelle fait
     * accepter des animations qui vont taper la butée opposée.
     */
    @Test
    void lesButeesMoteurPassentParLaTransmission() {
        Map<Axe, LimitesMoteur> limites = LimitesMoteur.parAxe(configurationDuRobot());

        // Panoramique : init 95, moteur [35 ; 155], rapport +1,583 → ±95° de tête
        assertEquals(-95.0, limites.get(Axe.COU_GAUCHE_DROITE).positionMin(), 0.05);
        assertEquals(95.0, limites.get(Axe.COU_GAUCHE_DROITE).positionMax(), 0.05);
        // Inclinaison : init 67, moteur [60 ; 75], rapport -4,97 → le maximum moteur donne le
        // minimum d'axe, et la course fait 75° et non 15
        assertEquals(-39.8, limites.get(Axe.COU_HAUT_BAS).positionMin(), 0.05);
        assertEquals(34.8, limites.get(Axe.COU_HAUT_BAS).positionMax(), 0.05);
        // Monter/descendre : rapport laissé à -1, faute d'avoir jamais été mesuré
        assertEquals(-10, limites.get(Axe.COU_MONTER_DESCENDRE).positionMin(), 0.05);
        assertEquals(60, limites.get(Axe.COU_MONTER_DESCENDRE).positionMax(), 0.05);
    }

    /** Les yeux sont déjà exprimés en relatif dans la configuration : rien à convertir. */
    @Test
    void lesYeuxGardentLeursButeesRelatives() {
        Map<Axe, LimitesMoteur> limites = LimitesMoteur.parAxe(configurationDuRobot());

        assertEquals(-5, limites.get(Axe.OEIL_GAUCHE).positionMin());
        assertEquals(20, limites.get(Axe.OEIL_DROIT).positionMax());
    }

    /**
     * Le seul axe sans valeur par défaut dans la configuration : sur un robot où il n'est pas
     * réglé, le lire lève. Une animation non contrôlée sur cet axe vaut mieux qu'un démarrage
     * refusé.
     */
    @Test
    void unAxeNonConfigureEstSimplementAbsent() {
        PhidgetsConfig configuration = configurationDuRobot();
        when(configuration.neckUpDownMotorInitialPosition()).thenThrow(new IllegalArgumentException("non réglé"));

        Map<Axe, LimitesMoteur> limites = LimitesMoteur.parAxe(configuration);

        assertFalse(limites.containsKey(Axe.COU_MONTER_DESCENDRE));
        assertTrue(limites.containsKey(Axe.COU_GAUCHE_DROITE), "Les autres axes restent contrôlables");
    }

    /**
     * La configuration ne porte que les vitesses de travail. Les vraies capacités se lisent sur le
     * servo attaché, et doivent pouvoir s'y substituer sans toucher aux butées.
     */
    @Test
    void lesCapacitesDuServoRemplacentCellesDeLaConfiguration() {
        LimitesMoteur depuisConfiguration = LimitesMoteur.parAxe(configurationDuRobot()).get(Axe.COU_GAUCHE_DROITE);

        LimitesMoteur reelles = depuisConfiguration.avecCapacites(400, 5000);

        assertEquals(400, reelles.vitesseMax());
        assertEquals(5000, reelles.accelerationMax());
        assertEquals(depuisConfiguration.positionMin(), reelles.positionMin());
        assertEquals(depuisConfiguration.positionMax(), reelles.positionMax());
    }

    @Test
    void contientDitCeQuiEstAtteignable() {
        LimitesMoteur limites = new LimitesMoteur(40, 60, -5, 20);

        assertTrue(limites.contient(-5));
        assertTrue(limites.contient(20));
        assertFalse(limites.contient(-5.1));
        assertFalse(limites.contient(20.1));
    }
}
