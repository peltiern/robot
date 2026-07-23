package fr.roboteek.robot.systemenerveux.spring;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Pont entre l'ancien bus Guava ({@link RobotEventBus}) et les évènements Spring,
 * le temps de la migration des organes.
 * <p>
 * Règle de transition : <b>on publie encore sur le bus Guava, on écoute côté Spring</b>.
 * Tout évènement posté sur le bus Guava (synchrone ou asynchrone) est relayé vers
 * {@link ApplicationEventPublisher} : les organes migrés le reçoivent via
 * {@code @EventListener}, les organes non migrés continuent de le recevoir via
 * {@code @Subscribe}. Un organe migré ne doit plus être abonné au bus Guava,
 * sinon il recevrait l'évènement deux fois.
 * <p>
 * À supprimer une fois tous les organes migrés.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Component
public class GuavaSpringEventBridge {

    private final ApplicationEventPublisher applicationEventPublisher;

    public GuavaSpringEventBridge(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostConstruct
    public void abonner() {
        RobotEventBus.getInstance().subscribe(this);
    }

    @PreDestroy
    public void desabonner() {
        RobotEventBus.getInstance().unsubscribe(this);
    }

    /**
     * Relaie tout évènement du bus Guava vers les évènements Spring.
     *
     * @param event évènement du robot
     */
    @Subscribe
    public void relayer(RobotEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
