package com.mugentwo.http_shadower.controller;

import com.mugentwo.http_shadower.service.HttpForwardingService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class HttpProxyController {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyController.class);
    
    private final HttpForwardingService forwardingService;

    public HttpProxyController(HttpForwardingService forwardingService) {
        this.forwardingService = forwardingService;
    }

    @RequestMapping(value = "/**", method = {
        RequestMethod.GET, 
        RequestMethod.POST, 
        RequestMethod.PUT, 
        RequestMethod.DELETE, 
        RequestMethod.PATCH, 
        RequestMethod.HEAD, 
        RequestMethod.OPTIONS
    })
    public ResponseEntity<String> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body) throws IOException {
        
        logger.info("Received {} request for path: {}", request.getMethod(), request.getRequestURI());
        
        String requestBody = body;
        if (requestBody == null && hasBody(request.getMethod())) {
            requestBody = forwardingService.extractRequestBody(request);
        }
        
        ResponseEntity<String> response = forwardingService.forwardRequestAndGetResponse(request, requestBody);
        
        logger.info("Returning response with status: {}", response.getStatusCode());
        return response;
    }
    
    private boolean hasBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}