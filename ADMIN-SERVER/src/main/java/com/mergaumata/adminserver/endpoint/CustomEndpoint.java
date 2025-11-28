package com.mergaumata.adminserver.endpoint;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Custom actuator endpoint for service statistics
 */
@Slf4j
@Component
@Endpoint(id = "services")
@RequiredArgsConstructor
public class CustomEndpoint {

    private final InstanceRepository instanceRepository;

    @ReadOperation
    public Mono<Map<String, Object>> getServiceStatistics() {
        return instanceRepository.findAll()
                .collectList()
                .map(instances -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("total-services", instances.size());

                    long upCount = instances.stream()
                            .filter(i -> "UP".equals(i.getStatusInfo().getStatus()))
                            .count();

                    long downCount = instances.stream()
                            .filter(i -> "DOWN".equals(i.getStatusInfo().getStatus()))
                            .count();

                    long unknownCount = instances.stream()
                            .filter(i -> "UNKNOWN".equals(i.getStatusInfo().getStatus()))
                            .count();

                    stats.put("services-up", upCount);
                    stats.put("services-down", downCount);
                    stats.put("services-unknown", unknownCount);
                    stats.put("health-percentage",
                            instances.isEmpty() ? 0 : (upCount * 100.0 / instances.size()));

                    return stats;
                });
    }
}