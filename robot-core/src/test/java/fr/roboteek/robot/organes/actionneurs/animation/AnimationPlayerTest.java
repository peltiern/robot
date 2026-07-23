package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.spring.GuavaSpringEventBridge;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que l'AnimationPlayer migré déroule une animation : réception d'un
 * {@link PlayAnimationEvent} via les évènements Spring, puis publication des
 * évènements de mouvements par le thread de la boucle (aucun matériel requis).
 */
class AnimationPlayerTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(
                RobotEventsConfig.class,
                GuavaSpringEventBridge.class,
                AnimationPlayer.class,
                MouvementsRecus.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void uneAnimationPublieLesEvenementsDeMouvements() throws InterruptedException {
        AnimationPlayer animationPlayer = context.getBean(AnimationPlayer.class);
        assertTrue(animationPlayer.isRunning(), "L'AnimationPlayer doit être démarré par le contexte Spring");

        PlayAnimationEvent event = new PlayAnimationEvent();
        event.setAnimation(Animation.NEUTRAL);
        RobotEventBus.getInstance().publish(event);

        // L'unique étape de NEUTRAL a un délai de 200 ms, la boucle scrute toutes les 100 ms
        MouvementsRecus recus = context.getBean(MouvementsRecus.class);
        long timeout = System.currentTimeMillis() + 5000;
        while ((recus.mouvementsYeux.isEmpty() || recus.mouvementsCou.isEmpty())
                && System.currentTimeMillis() < timeout) {
            Thread.sleep(50);
        }
        assertFalse(recus.mouvementsYeux.isEmpty(), "Aucun évènement de mouvement des yeux publié");
        assertFalse(recus.mouvementsCou.isEmpty(), "Aucun évènement de mouvement du cou publié");
    }

    @Test
    void laFermetureDuContexteArreteLOrgane() {
        AnimationPlayer animationPlayer = context.getBean(AnimationPlayer.class);
        context.close();
        assertFalse(animationPlayer.isRunning(), "L'AnimationPlayer doit être arrêté à la fermeture du contexte");
    }

    @Component
    static class MouvementsRecus {

        private final List<MouvementYeuxEvent> mouvementsYeux = new CopyOnWriteArrayList<>();
        private final List<MouvementCouEvent> mouvementsCou = new CopyOnWriteArrayList<>();

        @EventListener
        void onMouvementYeux(MouvementYeuxEvent event) {
            mouvementsYeux.add(event);
        }

        @EventListener
        void onMouvementCou(MouvementCouEvent event) {
            mouvementsCou.add(event);
        }
    }
}
