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

    @Override
    public Executor getAsyncExecutor() {
        return robotEventExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // TODO Phase 1 (watchdog) : déclencher l'arrêt des moteurs si l'exception vient d'un organe critique
        return (throwable, method, params) ->
                logger.error("Exception non rattrapée dans le listener {} : ", method, throwable);
    }
}
