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
        when(configuration.eyeLeftMotorSpeed()).thenReturn(40d);
        when(configuration.eyeLeftMotorAcceleration()).thenReturn(60d);
        when(configuration.eyeRightMotorSpeed()).thenReturn(40d);
        when(configuration.eyeRightMotorAcceleration()).thenReturn(60d);
        when(configuration.eyeMotorRelativePositionMin()).thenReturn(-5d);
        when(configuration.eyeMotorRelativePositionMax()).thenReturn(20d);
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
     * Le repère logique est inversé par rapport au moteur : le maximum moteur donne le minimum
     * relatif. Se tromper de sens ferait accepter des animations qui vont taper la butée opposée.
     */
    @Test
    void lesButeesMoteurDeviennentDesButeesRelatives() {
        Map<Axe, LimitesMoteur> limites = LimitesMoteur.parAxe(configurationDuRobot());

        // Panoramique : init 95, moteur [35 ; 155] → relatif [95-155 ; 95-35]
        assertEquals(-60, limites.get(Axe.COU_GAUCHE_DROITE).positionMin());
        assertEquals(60, limites.get(Axe.COU_GAUCHE_DROITE).positionMax());
        // Inclinaison : à peine 15° de course, l'axe le moins expressif du robot
        assertEquals(-8, limites.get(Axe.COU_HAUT_BAS).positionMin());
        assertEquals(7, limites.get(Axe.COU_HAUT_BAS).positionMax());
        assertEquals(-10, limites.get(Axe.COU_MONTER_DESCENDRE).positionMin());
        assertEquals(60, limites.get(Axe.COU_MONTER_DESCENDRE).positionMax());
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
