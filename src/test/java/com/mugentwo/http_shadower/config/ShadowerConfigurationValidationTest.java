package com.mugentwo.http_shadower.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShadowerConfigurationValidationTest {

    private ShadowerConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ShadowerConfiguration();
    }

    @Test
    void testValidConfiguration_OneResponseSource() {
        List<DestinationProperties> destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", true, true),
            new DestinationProperties("app2", "http://localhost:3002", true, false)
        );
        configuration.setDestinations(destinations);

        assertDoesNotThrow(() -> configuration.validateConfiguration());
    }

    @Test
    void testValidConfiguration_SingleDestination() {
        List<DestinationProperties> destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", true, true)
        );
        configuration.setDestinations(destinations);

        assertDoesNotThrow(() -> configuration.validateConfiguration());
    }

    @Test
    void testInvalidConfiguration_NoEnabledDestinations() {
        List<DestinationProperties> destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", false, true),
            new DestinationProperties("app2", "http://localhost:3002", false, false)
        );
        configuration.setDestinations(destinations);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.validateConfiguration());
        assertEquals("At least one destination must be enabled", exception.getMessage());
    }

    @Test
    void testInvalidConfiguration_NoResponseSource() {
        List<DestinationProperties> destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", true, false),
            new DestinationProperties("app2", "http://localhost:3002", true, false)
        );
        configuration.setDestinations(destinations);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.validateConfiguration());
        assertEquals("Exactly one enabled destination must be configured as responseSource", exception.getMessage());
    }

    @Test
    void testInvalidConfiguration_MultipleResponseSources() {
        List<DestinationProperties> destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", true, true),
            new DestinationProperties("app2", "http://localhost:3002", true, true)
        );
        configuration.setDestinations(destinations);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.validateConfiguration());
        assertEquals("Only one destination can be configured as responseSource, found: 2", exception.getMessage());
    }

    @Test
    void testInvalidConfiguration_ThreeResponseSources() {
        List<DestinationProperties> destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", true, true),
            new DestinationProperties("app2", "http://localhost:3002", true, true),
            new DestinationProperties("app3", "http://localhost:3003", true, true)
        );
        configuration.setDestinations(destinations);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.validateConfiguration());
        assertEquals("Only one destination can be configured as responseSource, found: 3", exception.getMessage());
    }

    @Test
    void testValidConfiguration_DisabledResponseSourceIgnored() {
        List<DestinationProperties> destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", true, true),
            new DestinationProperties("app2", "http://localhost:3002", false, true),
            new DestinationProperties("app3", "http://localhost:3003", true, false)
        );
        configuration.setDestinations(destinations);

        assertDoesNotThrow(() -> configuration.validateConfiguration());
    }

    @Test
    void testInvalidConfiguration_EmptyDestinations() {
        configuration.setDestinations(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.validateConfiguration());
        assertEquals("At least one destination must be enabled", exception.getMessage());
    }
}