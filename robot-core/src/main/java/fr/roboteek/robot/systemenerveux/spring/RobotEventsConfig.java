package fr.roboteek.robot.systemenerveux.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration du système nerveux basé sur les évènements Spring.
 * <p>
 * Les évènements Spring sont synchrones par défaut : les listeners qui doivent être asynchrones
 * le déclarent explicitement avec {@code @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)}.
 * <p>
 * Toute exception non rattrapée d'un listener asynchrone passe par
 * {@link #getAsyncUncaughtExceptionHandler()} : c'est le point d'ancrage du futur watchdog
 * (arrêt des moteurs si un organe critique meurt).
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Configuration
// proxyTargetClass : les organes implémentent SmartLifecycle ; un proxy JDK n'exposerait
// que les méthodes d'interface et casserait leurs @EventListener
@EnableAsync(proxyTargetClass = true)
public class RobotEventsConfig implements AsyncConfigurer {

    /** Nom du bean executor dédié aux évènements du robot. */
    public static final String ROBOT_EVENT_EXECUTOR = "robotEventExecutor";

    /**
     * Nom du bean executor MONO-THREAD dédié aux commandes des yeux (mouvements d'oeil + roulis).
     * Ces commandes visent le même matériel et doivent être traitées dans l'ordre de publication,
     * sans concurrence : sinon un arrêt et un roulis publiés coup sur coup (ex. après avoir bougé
     * un oeil puis lancé un roulis) tournent en parallèle et figent un oeil en cours de route, d'où
     * un roulis asymétrique ou un seul oeil qui bouge.
     */
    public static final String YEUX_EVENT_EXECUTOR = "yeuxEventExecutor";

    /**
     * Nom de l'executor MONO-THREAD dédié aux commandes du cou, pour la même raison que celui des
     * yeux : trois axes sur le même contrôleur, des consignes qui doivent partir dans l'ordre de
     * publication.
     * <p>
     * Sur le pool partagé, deux {@code MouvementCouEvent} consécutifs pouvaient être pris par deux
     * threads et s'exécuter dans le désordre. Anodin tant que le cou ne recevait qu'un ordre de
     * temps en temps ; certain dès qu'une animation envoie une trajectoire échantillonnée — la tête
     * reviendrait alors en arrière d'un échantillon sur l'autre.
     */
    public static final String COU_EVENT_EXECUTOR = "couEventExecutor";

    private static final Logger logger = LoggerFactory.getLogger(RobotEventsConfig.class);

    /**
     * Executor dédié au traitement asynchrone des évènements du robot.
     * Dimensionné pour le Jetson Nano (4 coeurs) : inutile d'avoir les 100 threads
     * de l'ancien bus Guava.
     */
    @Bean(name = ROBOT_EVENT_EXECUTOR)
    public ThreadPoolTaskExecutor robotEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("robot-event-");
        return executor;
    }

    /**
     * Executor MONO-THREAD (FIFO) dédié aux commandes des yeux : garantit que mouvements d'oeil et
     * roulis sont exécutés l'un après l'autre, dans l'ordre où ils ont été publiés, jamais en
     * parallèle. Sérialiser ces commandes moteur (même matériel) supprime les courses qui rendaient
     * le roulis asymétrique de façon intermittente.
     */
    @Bean(name = YEUX_EVENT_EXECUTOR)
    public ThreadPoolTaskExecutor yeuxEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("yeux-event-");
        return executor;
    }

    /**
     * Executor MONO-THREAD (FIFO) dédié aux commandes du cou (voir {@link #COU_EVENT_EXECUTOR}).
     * File plus profonde que celle des yeux : le cou porte trois axes, et une animation lui envoie
     * une trajectoire échantillonnée, pas des ordres isolés.
     */
    @Bean(name = COU_EVENT_EXECUTOR)
    public ThreadPoolTaskExecutor couEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("cou-event-");
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return robotEventExecutor();
    }

    /**
     * Les exceptions des listeners asynchrones sont journalisées, sans plus : couper les moteurs
     * ici serait à la fois trop et pas assez. Trop, parce qu'un listener qui échoue une fois n'a pas
     * tué son organe — l'exécuteur lui donnera l'évènement suivant. Pas assez, parce que la vraie
     * panne à craindre, une boucle d'organe morte, ne passe justement pas par ce point : le thread
     * s'arrête sans que personne ne le remarque.
     * <p>
     * C'est le rôle du {@code WatchDog}, qui juge sur les battements des organes et non sur les
     * exceptions qu'ils lèvent.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                logger.error("Exception non rattrapée dans le listener {} : ", method, throwable);
    }
}
