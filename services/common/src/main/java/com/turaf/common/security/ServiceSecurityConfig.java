package com.turaf.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Shared Spring Security configuration for downstream microservices.
 *
 * Activated by setting {@code turaf.security.service-mode: true} in a service's
 * {@code application.yml}. Services that manage their own security (identity-service)
 * omit that property and this config is never loaded.
 *
 * Security model:
 * <ul>
 *   <li>Stateless — no sessions.</li>
 *   <li>HTTP Basic and form login disabled.</li>
 *   <li>{@code /actuator/**} is public.</li>
 *   <li>All other endpoints require an authenticated principal set by
 *       {@link ServiceJwtAuthenticationFilter}.</li>
 * </ul>
 *
 * {@code @EnableMethodSecurity} activates {@code @PreAuthorize} annotations used
 * by controllers such as {@code ExperimentController} and {@code MetricController}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "turaf.security.service-mode", havingValue = "true")
public class ServiceSecurityConfig {

    private final ServiceJwtAuthenticationFilter serviceJwtAuthenticationFilter;

    public ServiceSecurityConfig(ServiceJwtAuthenticationFilter serviceJwtAuthenticationFilter) {
        this.serviceJwtAuthenticationFilter = serviceJwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain serviceSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            // ServiceJwtAuthenticationFilter runs as a servlet filter (auto-registered via @Component)
            // and sets SecurityContextHolder before Spring Security's chain evaluates the request.
            // Adding it here explicitly (before UsernamePasswordAuthenticationFilter) ensures it
            // also runs within the security chain ordering when the request reaches Spring Security.
            .addFilterBefore(serviceJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
