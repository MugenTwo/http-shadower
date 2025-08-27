package com.mugentwo.http_shadower.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "shadower.destinations[0].name=test-destination",
    "shadower.destinations[0].url=http://localhost:9999", 
    "shadower.destinations[0].enabled=true",
    "shadower.destinations[0].responseSource=true"
})
class HttpShadowerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetRequestForwarding() {
        String url = "http://localhost:" + port + "/api/test";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Since destination is unreachable, should get SERVICE_UNAVAILABLE
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Service temporarily unavailable"));
    }

    @Test
    void testPostRequestForwarding() {
        String url = "http://localhost:" + port + "/api/users";
        String requestBody = "{\"name\": \"John Doe\", \"email\": \"john@example.com\"}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer test-token");
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        // Since destination is unreachable, should get SERVICE_UNAVAILABLE
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Service temporarily unavailable"));
    }

    @Test
    void testPutRequestForwarding() {
        String url = "http://localhost:" + port + "/api/users/123";
        String requestBody = "{\"name\": \"Jane Doe\"}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.PUT, request, String.class);
        
        // Since destination is unreachable, should get SERVICE_UNAVAILABLE
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Service temporarily unavailable"));
    }

    @Test
    void testDeleteRequestForwarding() {
        String url = "http://localhost:" + port + "/api/users/123";
        
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.DELETE, null, String.class);
        
        // Since destination is unreachable, should get SERVICE_UNAVAILABLE
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Service temporarily unavailable"));
    }
}