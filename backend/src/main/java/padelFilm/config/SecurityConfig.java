package com.padelPlay.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // auth → public
                        .requestMatchers("/api/auth/**").permitAll()

                        // swagger → public
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // membres → lecture publique (le frontend membre en a besoin)
                        .requestMatchers(HttpMethod.GET, "/api/membres/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/membres").permitAll()

                        // matches → lecture publique (voir les matchs publics)
                        .requestMatchers(HttpMethod.GET, "/api/matches/**").permitAll()
                        // matches → creation membre (pas de login user, uniquement matricule)
                        .requestMatchers(HttpMethod.POST, "/api/matches").permitAll()

                        // sites/terrains/jours de fermeture → lecture publique (creation match + affichage)
                        .requestMatchers(HttpMethod.GET, "/api/sites/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/terrains/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/jours-fermeture/**").permitAll()

                        // réservations → lecture publique
                        .requestMatchers(HttpMethod.GET, "/api/reservations/**").permitAll()

                        // réservations → création et annulation accessibles aux membres
                        .requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/reservations/*/cancel").permitAll()

                        // paiements → accessibles aux membres
                        .requestMatchers("/api/paiements/**").permitAll()

                        // tout le reste → admin uniquement
                        .anyRequest().hasAnyRole("GLOBAL", "SITE")
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
