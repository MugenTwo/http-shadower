package com.mugentwo.http_shadower.service;

import com.mugentwo.http_shadower.config.DestinationProperties;
import com.mugentwo.http_shadower.config.ShadowerConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpForwardingServiceTest {

    @Mock
    private ShadowerConfiguration shadowerConfiguration;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private RestTemplate restTemplate;
    
    private HttpForwardingService forwardingService;

    @BeforeEach
    void setUp() {
        forwardingService = new HttpForwardingService(shadowerConfiguration);
        // Use reflection to inject the mock RestTemplate for testing
        try {
            var field = HttpForwardingService.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(forwardingService, restTemplate);
        } catch (Exception e) {
            fail("Failed to inject mock RestTemplate");
        }
    }

    @Test
    void testForwardRequest_WithEnabledDestinations() {
        // Arrange
        var destination1 = new DestinationProperties("app1", "http://localhost:3001", true);
        var destination2 = new DestinationProperties("app2", "http://localhost:3002", true);
        var destinations = List.of(destination1, destination2);
        
        when(shadowerConfiguration.getEnabledDestinations()).thenReturn(destinations);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn("param=value");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Content-Type", "Authorization")));
        when(request.getHeaders("Content-Type")).thenReturn(Collections.enumeration(List.of("application/json")));
        when(request.getHeaders("Authorization")).thenReturn(Collections.enumeration(List.of("Bearer token123")));
        
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act
        forwardingService.forwardRequest(request, "{\"test\": \"data\"}");

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert
        verify(restTemplate, times(2)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testForwardRequest_WithDisabledDestinations() {
        // Arrange
        var destination1 = new DestinationProperties("app1", "http://localhost:3001", true);
        var destination2 = new DestinationProperties("app2", "http://localhost:3002", false);
        
        when(shadowerConfiguration.getEnabledDestinations()).thenReturn(List.of(destination1));
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of()));
        
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act
        forwardingService.forwardRequest(request, null);

        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert
        verify(restTemplate, times(1)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testForwardRequest_HandlesRestClientException() {
        // Arrange
        var destination = new DestinationProperties("app1", "http://localhost:3001", true);
        when(shadowerConfiguration.getEnabledDestinations()).thenReturn(List.of(destination));
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of()));
        
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> forwardingService.forwardRequest(request, null));
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify the call was made despite the exception
        verify(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testExtractRequestBody() throws IOException {
        // Arrange
        String expectedBody = "{\"key\": \"value\"}";
        BufferedReader reader = new BufferedReader(new StringReader(expectedBody));
        when(request.getReader()).thenReturn(reader);

        // Act
        String actualBody = forwardingService.extractRequestBody(request);

        // Assert
        assertEquals(expectedBody, actualBody);
    }

    @Test
    void testGetEnabledDestinationsCount() {
        // Arrange
        var destinations = List.of(
            new DestinationProperties("app1", "http://localhost:3001", true),
            new DestinationProperties("app2", "http://localhost:3002", true)
        );
        when(shadowerConfiguration.getEnabledDestinations()).thenReturn(destinations);

        // Act
        int count = forwardingService.getEnabledDestinationsCount();

        // Assert
        assertEquals(2, count);
    }

    @Test
    void testForwardRequestAndGetResponse_WithResponseSource() {
        // Arrange
        var responseSource = new DestinationProperties("app1", "http://localhost:3001", true, true);
        var otherDest = new DestinationProperties("app2", "http://localhost:3002", true, false);
        var destinations = List.of(responseSource, otherDest);
        
        when(shadowerConfiguration.getEnabledDestinations()).thenReturn(destinations);
        when(shadowerConfiguration.getResponseSourceDestination()).thenReturn(responseSource);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of()));
        
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Success from app1", HttpStatus.OK);
        when(restTemplate.exchange(contains("3001"), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> result = forwardingService.forwardRequestAndGetResponse(request, null);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Success from app1", result.getBody());
        verify(restTemplate, times(1)).exchange(contains("3001"), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testForwardRequestAndGetResponse_NoResponseSource_UseFirst() {
        // Arrange
        var destination1 = new DestinationProperties("app1", "http://localhost:3001", true, false);
        var destination2 = new DestinationProperties("app2", "http://localhost:3002", true, false);
        var destinations = List.of(destination1, destination2);
        
        when(shadowerConfiguration.getEnabledDestinations()).thenReturn(destinations);
        when(shadowerConfiguration.getResponseSourceDestination()).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/create");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of()));
        
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Created", HttpStatus.CREATED);
        when(restTemplate.exchange(contains("3001"), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> result = forwardingService.forwardRequestAndGetResponse(request, "{\"data\":\"test\"}");

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("Created", result.getBody());
    }

    @Test
    void testForwardRequestAndGetResponse_AllDestinationsFail() {
        // Arrange
        var destination = new DestinationProperties("app1", "http://localhost:3001", true, true);
        when(shadowerConfiguration.getEnabledDestinations()).thenReturn(List.of(destination));
        when(shadowerConfiguration.getResponseSourceDestination()).thenReturn(destination);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of()));
        
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // Act
        ResponseEntity<String> result = forwardingService.forwardRequestAndGetResponse(request, null);

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        assertTrue(result.getBody().contains("Service temporarily unavailable"));
    }
}