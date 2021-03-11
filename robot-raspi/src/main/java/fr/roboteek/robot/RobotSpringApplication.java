package fr.roboteek.robot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RobotSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobotSpringApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> new Robot();
    }

}
