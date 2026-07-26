package fr.roboteek.robot.spring.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(Customizer.withDefaults()).disable()) // Désactive les restrictions sur les frames
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        //.requestMatchers("/wsendpoint").permitAll()
                        .requestMatchers("/**").permitAll()
                );

        return http.build();
    }

    /**
     * Apply CORS configuration before Spring Security.
     * By default, "http.cors" take a bean called corsConfigurationSource.
     *
     * @return a CORS configuration source.
     * @implNote https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#cors
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // allowedOriginPatterns (et non allowedOrigins) : avec allowCredentials=true, Spring
        // interdit la valeur "*" pour allowedOrigins (impossible de la renvoyer dans
        // Access-Control-Allow-Origin). Le pattern "*" est autorisé et renvoie l'origine reçue.
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
