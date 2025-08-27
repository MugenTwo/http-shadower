package com.mugentwo.http_shadower.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;

import com.mugentwo.http_shadower.HttpShadowerApplication;

import static org.junit.jupiter.api.Assertions.*;

class ShadowerConfigurationStartupValidationTest {

    @Test
    void testStartupFailsWithNoResponseSource() {
        System.setProperty("shadower.destinations[0].name", "test-app1");
        System.setProperty("shadower.destinations[0].url", "http://localhost:3001");
        System.setProperty("shadower.destinations[0].enabled", "true");
        System.setProperty("shadower.destinations[0].responseSource", "false");
        
        SpringApplication app = new SpringApplication(HttpShadowerApplication.class);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try (ConfigurableApplicationContext context = app.run()) {
            }
        });
        
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Exactly one enabled destination must be configured as responseSource", 
                     exception.getCause().getMessage());
        
        clearSystemProperties();
    }

    @Test
    void testStartupFailsWithMultipleResponseSources() {
        System.setProperty("shadower.destinations[0].name", "test-app1");
        System.setProperty("shadower.destinations[0].url", "http://localhost:3001");
        System.setProperty("shadower.destinations[0].enabled", "true");
        System.setProperty("shadower.destinations[0].responseSource", "true");
        System.setProperty("shadower.destinations[1].name", "test-app2");
        System.setProperty("shadower.destinations[1].url", "http://localhost:3002");
        System.setProperty("shadower.destinations[1].enabled", "true");
        System.setProperty("shadower.destinations[1].responseSource", "true");
        
        SpringApplication app = new SpringApplication(HttpShadowerApplication.class);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try (ConfigurableApplicationContext context = app.run()) {
            }
        });
        
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Only one destination can be configured as responseSource, found: 2", 
                     exception.getCause().getMessage());
        
        clearSystemProperties();
    }

    @Test
    void testStartupFailsWithNoEnabledDestinations() {
        System.setProperty("shadower.destinations[0].name", "test-app1");
        System.setProperty("shadower.destinations[0].url", "http://localhost:3001");
        System.setProperty("shadower.destinations[0].enabled", "false");
        System.setProperty("shadower.destinations[0].responseSource", "true");
        
        SpringApplication app = new SpringApplication(HttpShadowerApplication.class);
        
        Exception exception = assertThrows(Exception.class, () -> {
            try (ConfigurableApplicationContext context = app.run()) {
            }
        });
        
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("At least one destination must be enabled", 
                     exception.getCause().getMessage());
        
        clearSystemProperties();
    }

    private void clearSystemProperties() {
        System.clearProperty("shadower.destinations[0].name");
        System.clearProperty("shadower.destinations[0].url");
        System.clearProperty("shadower.destinations[0].enabled");
        System.clearProperty("shadower.destinations[0].responseSource");
        System.clearProperty("shadower.destinations[1].name");
        System.clearProperty("shadower.destinations[1].url");
        System.clearProperty("shadower.destinations[1].enabled");
        System.clearProperty("shadower.destinations[1].responseSource");
    }
}