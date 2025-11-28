package com.merigaumata.eureka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        http
                // For API servers, CSRF is typically disabled — keep this explicit and documented
                .csrf(AbstractHttpConfigurer::disable)
                // Stateless sessions for service-to-service interactions
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Basic auth for service registration and internal calls
                .httpBasic(Customizer.withDefaults())
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                                // Actuator public info endpoints
                                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                // All other actuator endpoints protected and require ADMIN
                                .requestMatchers("/actuator/**").hasRole("ADMIN")
                                // Eureka endpoints: allow SERVICE role for registration, and SERVICE or ADMIN for reads
                                .requestMatchers(HttpMethod.POST, "/eureka/**").hasRole("SERVICE")
                                .requestMatchers(HttpMethod.PUT, "/eureka/**").hasRole("SERVICE")
                                .requestMatchers(HttpMethod.DELETE, "/eureka/**").hasRole("SERVICE")
                                .requestMatchers(HttpMethod.GET, "/eureka/**").hasAnyRole("SERVICE", "ADMIN")
                                // Any other endpoint requires authenticated user with any role
                                .anyRequest().authenticated()
                );

        // Enforce HTTPS only in production (if TLS termination is at load balancer, ensure headers are set)
        // This is moved after authorize requests to avoid conflicts
        if (!isDev) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
        // Security headers: keep defaults, but make explicit adjustments if needed
        //From now on, only talk to this server using HTTPS. Never use an insecure HTTP connection, even if the user asks for it.
        //HSTS (HTTP Strict Transport Security)
        http.headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
        );

        return http.build();
    }

    // Use JdbcUserDetailsManager so users and roles live in a RDBMS (Postgres / MySQL)
    @Bean
    public UserDetailsManager users(DataSource dataSource) {
        // JdbcUserDetailsManager expects the default schema (users + authorities). If you
        // use a custom schema, set queries via jdbc.setUsersByUsernameQuery(...)
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}