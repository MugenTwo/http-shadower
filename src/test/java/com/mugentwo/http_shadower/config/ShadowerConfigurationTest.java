package com.mugentwo.http_shadower.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "shadower.destinations[0].name=test-app1",
    "shadower.destinations[0].url=http://localhost:3001",
    "shadower.destinations[0].enabled=true",
    "shadower.destinations[0].responseSource=true",
    "shadower.destinations[1].name=test-app2", 
    "shadower.destinations[1].url=http://localhost:3002",
    "shadower.destinations[1].enabled=false",
    "shadower.destinations[1].responseSource=false"
})
class ShadowerConfigurationTest {

    @Autowired
    private ShadowerConfiguration shadowerConfiguration;

    @Test
    void testConfigurationLoading() {
        List<DestinationProperties> destinations = shadowerConfiguration.getDestinations();
        
        assertEquals(2, destinations.size());
        
        DestinationProperties dest1 = destinations.get(0);
        assertEquals("test-app1", dest1.getName());
        assertEquals("http://localhost:3001", dest1.getUrl());
        assertTrue(dest1.isEnabled());
        assertTrue(dest1.isResponseSource());
        
        DestinationProperties dest2 = destinations.get(1);
        assertEquals("test-app2", dest2.getName());
        assertEquals("http://localhost:3002", dest2.getUrl());
        assertFalse(dest2.isEnabled());
        assertFalse(dest2.isResponseSource());
    }

    @Test
    void testGetEnabledDestinations() {
        List<DestinationProperties> enabledDestinations = shadowerConfiguration.getEnabledDestinations();
        
        assertEquals(1, enabledDestinations.size());
        assertEquals("test-app1", enabledDestinations.get(0).getName());
        assertTrue(enabledDestinations.get(0).isEnabled());
    }

    @Test
    void testGetResponseSourceDestination() {
        DestinationProperties responseSource = shadowerConfiguration.getResponseSourceDestination();
        
        assertNotNull(responseSource);
        assertEquals("test-app1", responseSource.getName());
        assertTrue(responseSource.isResponseSource());
        assertTrue(responseSource.isEnabled());
    }
}