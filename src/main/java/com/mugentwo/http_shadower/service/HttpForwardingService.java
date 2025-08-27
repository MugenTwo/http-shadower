package com.mugentwo.http_shadower.service;

import com.mugentwo.http_shadower.config.DestinationProperties;
import com.mugentwo.http_shadower.config.ShadowerConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class HttpForwardingService {
    private static final Logger logger = LoggerFactory.getLogger(HttpForwardingService.class);
    
    private final ShadowerConfiguration shadowerConfiguration;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;

    public HttpForwardingService(ShadowerConfiguration shadowerConfiguration) {
        this.shadowerConfiguration = shadowerConfiguration;
        this.restTemplate = new RestTemplate();
        this.executorService = Executors.newCachedThreadPool();
    }

    public void forwardRequest(HttpServletRequest request, String requestBody) {
        var enabledDestinations = shadowerConfiguration.getEnabledDestinations();
        
        logger.info("Forwarding {} request to {} destinations: {}", 
                request.getMethod(), 
                enabledDestinations.size(),
                request.getRequestURI());

        enabledDestinations.forEach(destination -> 
            CompletableFuture.runAsync(() -> forwardToDestination(request, requestBody, destination))
        );
    }

    public ResponseEntity<String> forwardRequestAndGetResponse(HttpServletRequest request, String requestBody) {
        var enabledDestinations = shadowerConfiguration.getEnabledDestinations();
        var responseSource = shadowerConfiguration.getResponseSourceDestination();
        
        logger.info("Forwarding {} request to {} destinations: {}", 
                request.getMethod(), 
                enabledDestinations.size(),
                request.getRequestURI());

        ResponseEntity<String> primaryResponse = null;
        
        for (DestinationProperties destination : enabledDestinations) {
            if (destination.isResponseSource()) {
                primaryResponse = forwardToDestinationSync(request, requestBody, destination);
                logger.info("Retrieved response from primary destination: {}", destination.getName());
            } else {
                CompletableFuture.runAsync(() -> forwardToDestination(request, requestBody, destination), executorService);
            }
        }
        
        if (primaryResponse == null && responseSource == null && !enabledDestinations.isEmpty()) {
            logger.warn("No response source configured, using first enabled destination");
            primaryResponse = forwardToDestinationSync(request, requestBody, enabledDestinations.get(0));
        }
        
        if (primaryResponse == null) {
            logger.error("All destination requests failed");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service temporarily unavailable - all destination apps unreachable");
        }
        
        return primaryResponse;
    }

    private ResponseEntity<String> forwardToDestinationSync(HttpServletRequest request, String requestBody, DestinationProperties destination) {
        try {
            String targetUrl = buildTargetUrl(destination.getUrl(), request);
            HttpHeaders headers = extractHeaders(request);
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            logger.debug("Forwarding synchronously to {}: {} {}", destination.getName(), method, targetUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl, 
                method, 
                entity, 
                String.class
            );
            
            logger.debug("Response from {}: {} - {}", 
                destination.getName(), 
                response.getStatusCode(), 
                response.getBody());
            
            return response;
                
        } catch (RestClientException e) {
            logger.error("Failed to forward request synchronously to {}: {}", destination.getName(), e.getMessage());
            return null;
        }
    }

    private void forwardToDestination(HttpServletRequest request, String requestBody, DestinationProperties destination) {
        try {
            String targetUrl = buildTargetUrl(destination.getUrl(), request);
            HttpHeaders headers = extractHeaders(request);
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            logger.debug("Forwarding to {}: {} {}", destination.getName(), method, targetUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl, 
                method, 
                entity, 
                String.class
            );
            
            logger.debug("Response from {}: {} - {}", 
                destination.getName(), 
                response.getStatusCode(), 
                response.getBody());
                
        } catch (RestClientException e) {
            logger.error("Failed to forward request to {}: {}", destination.getName(), e.getMessage());
        }
    }

    private String buildTargetUrl(String baseUrl, HttpServletRequest request) {
        StringBuilder targetUrl = new StringBuilder(baseUrl);
        targetUrl.append(request.getRequestURI());
        
        if (request.getQueryString() != null) {
            targetUrl.append("?").append(request.getQueryString());
        }
        
        return targetUrl.toString();
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!"host".equalsIgnoreCase(headerName)) {
                headers.put(headerName, Collections.list(request.getHeaders(headerName)));
            }
        }
        
        return headers;
    }

    public String extractRequestBody(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public int getEnabledDestinationsCount() {
        return shadowerConfiguration.getEnabledDestinations().size();
    }
}