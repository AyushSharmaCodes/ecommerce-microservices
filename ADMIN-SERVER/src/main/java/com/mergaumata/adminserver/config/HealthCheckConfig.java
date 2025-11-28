package com.mergaumata.adminserver.config;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom health checks for Admin Server
 */
@Configuration
public class HealthCheckConfig {

    private final InstanceRepository instanceRepository;

    public HealthCheckConfig(InstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    @Bean
    public HealthIndicator registeredServicesHealthIndicator() {
        return () -> {
            long count = instanceRepository.findAll().count().block();

            if (count > 0) {
                return Health.up()
                        .withDetail("registered-services", count)
                        .withDetail("status", "Monitoring active services")
                        .build();
            } else {
                return Health.down()
                        .withDetail("registered-services", 0)
                        .withDetail("status", "No services registered")
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator adminServerHealthIndicator() {
        return () -> Health.up()
                .withDetail("admin-server", "operational")
                .withDetail("version", "1.0.0")
                .build();
    }
}