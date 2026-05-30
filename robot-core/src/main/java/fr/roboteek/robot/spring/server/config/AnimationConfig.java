package fr.roboteek.robot.spring.server.config;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationRepository;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationValidator;
import fr.roboteek.robot.organes.actionneurs.animation.MotorConstraintsFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

@Configuration
public class AnimationConfig {

    @Bean
    public AnimationRepository animationRepository() {
        return new AnimationRepository(Constantes.DOSSIER_ANIMATIONS);
    }

    @Bean
    public AnimationValidator animationValidator() {
        return new AnimationValidator(MotorConstraintsFactory.fromConfig(phidgetsConfig()));
    }
}
