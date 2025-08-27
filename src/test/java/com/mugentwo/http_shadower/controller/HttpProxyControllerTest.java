package com.mugentwo.http_shadower.controller;

import com.mugentwo.http_shadower.service.HttpForwardingService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpProxyControllerTest {

    @Mock
    private HttpForwardingService forwardingService;
    
    @Mock
    private HttpServletRequest request;
    
    private HttpProxyController controller;

    @BeforeEach
    void setUp() {
        controller = new HttpProxyController(forwardingService);
    }

    @Test
    void testProxyRequest_PostWithBody() throws IOException {
        // Arrange
        String requestBody = "{\"test\": \"data\"}";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Success from destination", HttpStatus.OK);
        
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(forwardingService.forwardRequestAndGetResponse(request, requestBody)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> response = controller.proxyRequest(request, requestBody);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success from destination", response.getBody());
        verify(forwardingService).forwardRequestAndGetResponse(request, requestBody);
    }

    @Test
    void testProxyRequest_PostWithoutProvidedBody() throws IOException {
        // Arrange
        String extractedBody = "{\"extracted\": \"data\"}";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Response with extracted body", HttpStatus.CREATED);
        
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(forwardingService.extractRequestBody(request)).thenReturn(extractedBody);
        when(forwardingService.forwardRequestAndGetResponse(request, extractedBody)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> response = controller.proxyRequest(request, null);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Response with extracted body", response.getBody());
        verify(forwardingService).extractRequestBody(request);
        verify(forwardingService).forwardRequestAndGetResponse(request, extractedBody);
    }

    @Test
    void testProxyRequest_GetRequest() throws IOException {
        // Arrange
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("[{\"id\":1,\"name\":\"User1\"}]", HttpStatus.OK);
        
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/users");
        when(forwardingService.forwardRequestAndGetResponse(request, null)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> response = controller.proxyRequest(request, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("[{\"id\":1,\"name\":\"User1\"}]", response.getBody());
        verify(forwardingService, never()).extractRequestBody(request);
        verify(forwardingService).forwardRequestAndGetResponse(request, null);
    }

    @Test
    void testProxyRequest_PutRequest() throws IOException {
        // Arrange
        String requestBody = "{\"update\": \"data\"}";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Updated successfully", HttpStatus.OK);
        
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/users/123");
        when(forwardingService.forwardRequestAndGetResponse(request, requestBody)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> response = controller.proxyRequest(request, requestBody);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated successfully", response.getBody());
        verify(forwardingService).forwardRequestAndGetResponse(request, requestBody);
    }

    @Test
    void testProxyRequest_DeleteRequest() throws IOException {
        // Arrange
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Deleted", HttpStatus.NO_CONTENT);
        
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/users/123");
        when(forwardingService.forwardRequestAndGetResponse(request, null)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> response = controller.proxyRequest(request, null);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("Deleted", response.getBody());
        verify(forwardingService).forwardRequestAndGetResponse(request, null);
    }
}