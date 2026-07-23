package fr.roboteek.robot.systemenerveux.spring;

import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que les évènements postés sur le bus Guava sont bien relayés
 * vers les listeners Spring pendant la phase de migration.
 */
class GuavaSpringEventBridgeTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(
                RobotEventsConfig.class,
                GuavaSpringEventBridge.class,
                EvenementsRecus.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void unEvenementPublieSurLeBusGuavaEstRecuParUnListenerSpring() {
        PlaySoundEvent event = new PlaySoundEvent();

        RobotEventBus.getInstance().publish(event);

        EvenementsRecus recus = context.getBean(EvenementsRecus.class);
        assertEquals(List.of(event), recus.evenements);
    }

    @Test
    void unEvenementPublieEnAsynchroneSurLeBusGuavaEstRecuParUnListenerSpring() throws InterruptedException {
        PlaySoundEvent event = new PlaySoundEvent();

        RobotEventBus.getInstance().publishAsync(event);

        EvenementsRecus recus = context.getBean(EvenementsRecus.class);
        long timeout = System.currentTimeMillis() + 5000;
        while (recus.evenements.isEmpty() && System.currentTimeMillis() < timeout) {
            Thread.sleep(10);
        }
        assertEquals(List.of(event), recus.evenements);
    }

    @Test
    void leBridgeSeDesabonneALaFermetureDuContexte() {
        EvenementsRecus recus = context.getBean(EvenementsRecus.class);
        context.close();

        // Après fermeture, un évènement Guava ne doit plus être relayé
        RobotEventBus.getInstance().publish(new PlaySoundEvent());
        assertTrue(recus.evenements.isEmpty());
    }

    @Component
    static class EvenementsRecus {

        private final List<RobotEvent> evenements = new CopyOnWriteArrayList<>();

        @EventListener
        void onRobotEvent(RobotEvent event) {
            evenements.add(event);
        }
    }
}
