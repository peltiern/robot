package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.spring.GuavaSpringEventBridge;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de fumée du {@link SoundPlayer} migré : joue réellement un son sur le poste
 * via le circuit complet (bus Guava → pont → listener Spring asynchrone → commande {@code play}).
 * <p>
 * Ne s'exécute que si la variable d'environnement {@code ROBOT_HOME} est définie
 * (par exemple {@code /home/npeltier/Robot/Programme}) : il nécessite les fichiers
 * de sons et la commande {@code play} (sox). Ignoré en CI.
 */
@EnabledIfEnvironmentVariable(named = "ROBOT_HOME", matches = ".+")
class SoundPlayerSmokeTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(
                RobotEventsConfig.class,
                GuavaSpringEventBridge.class,
                SoundPlayer.class,
                FinDeLectureDetectee.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void unPlaySoundEventPublieSurLeBusGuavaJoueLeSonSurLePoste() throws InterruptedException {
        File fichierSon = new File(Constantes.DOSSIER_SONS, RobotSound.WALLE.getFileName());
        assertTrue(fichierSon.exists(), "Fichier son introuvable : " + fichierSon
                + " (vérifier ROBOT_HOME)");

        PlaySoundEvent event = new PlaySoundEvent();
        event.setSound(RobotSound.WALLE);
        RobotEventBus.getInstance().publish(event);

        // Le SoundPlayer publie DEMARRER (reprise de la reconnaissance vocale) en fin de lecture
        FinDeLectureDetectee finDeLecture = context.getBean(FinDeLectureDetectee.class);
        assertTrue(finDeLecture.latch.await(10, TimeUnit.SECONDS),
                "Le son n'a pas été joué dans les 10 secondes");
    }

    @Component
    static class FinDeLectureDetectee {

        private final CountDownLatch latch = new CountDownLatch(1);

        @EventListener
        void onControleReconnaissanceVocale(ReconnaissanceVocaleControleEvent event) {
                if (event.getControle() == ReconnaissanceVocaleControleEvent.CONTROLE.DEMARRER) {
                latch.countDown();
            }
        }
    }
}
