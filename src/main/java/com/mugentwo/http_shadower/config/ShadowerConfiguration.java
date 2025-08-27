package com.mugentwo.http_shadower.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "shadower")
public class ShadowerConfiguration {
    private List<DestinationProperties> destinations = new ArrayList<>();

    public List<DestinationProperties> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<DestinationProperties> destinations) {
        this.destinations = destinations;
    }

    public List<DestinationProperties> getEnabledDestinations() {
        return destinations.stream()
                .filter(DestinationProperties::isEnabled)
                .toList();
    }

    public DestinationProperties getResponseSourceDestination() {
        return destinations.stream()
                .filter(DestinationProperties::isEnabled)
                .filter(DestinationProperties::isResponseSource)
                .findFirst()
                .orElse(null);
    }

    @PostConstruct
    public void validateConfiguration() {
        List<DestinationProperties> enabledDestinations = getEnabledDestinations();
        
        if (enabledDestinations.isEmpty()) {
            throw new IllegalStateException("At least one destination must be enabled");
        }

        long responseSourceCount = enabledDestinations.stream()
                .filter(DestinationProperties::isResponseSource)
                .count();

        if (responseSourceCount == 0) {
            throw new IllegalStateException("Exactly one enabled destination must be configured as responseSource");
        }

        if (responseSourceCount > 1) {
            throw new IllegalStateException("Only one destination can be configured as responseSource, found: " + responseSourceCount);
        }
    }
}