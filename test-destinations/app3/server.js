const express = require('express');
const app = express();
const PORT = process.env.PORT || 3003;

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD');
    res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-API-Key, X-Request-ID, X-Client-Version');
    if (req.method === 'OPTIONS') {
        res.sendStatus(200);
        return;
    }
    next();
});

app.use((req, res, next) => {
    console.log('\n=== INCOMING REQUEST ===');
    console.log(`Timestamp: ${new Date().toISOString()}`);
    console.log(`Method: ${req.method}`);
    console.log(`URL: ${req.originalUrl}`);
    console.log(`Path: ${req.path}`);
    console.log('Query Parameters:', req.query);
    console.log('Headers:', JSON.stringify(req.headers, null, 2));
    
    if (req.body && Object.keys(req.body).length > 0) {
        console.log('Body:', JSON.stringify(req.body, null, 2));
    }
    
    console.log('========================\n');
    next();
});

const errorScenarios = [
    {
        status: 400,
        body: {
            error: "Bad Request",
            message: "Invalid request parameters",
            code: "INVALID_PARAMS",
            details: "The request contains malformed or missing parameters"
        }
    },
    {
        status: 401,
        body: {
            error: "Unauthorized",
            message: "Authentication required",
            code: "AUTH_REQUIRED"
        }
    },
    {
        status: 403,
        body: {
            error: "Forbidden", 
            message: "Access denied",
            code: "ACCESS_DENIED",
            details: "You do not have permission to access this resource"
        }
    },
    {
        status: 404,
        body: {
            error: "Not Found",
            message: "Resource not found",
            code: "RESOURCE_NOT_FOUND",
            requestedPath: null
        }
    },
    {
        status: 409,
        body: {
            error: "Conflict",
            message: "Resource already exists",
            code: "DUPLICATE_RESOURCE",
            conflictField: "email"
        }
    },
    {
        status: 422,
        body: {
            error: "Unprocessable Entity",
            message: "Validation failed",
            code: "VALIDATION_ERROR",
            errors: [
                { field: "email", message: "Invalid email format" },
                { field: "age", message: "Must be a positive number" }
            ]
        }
    },
    {
        status: 429,
        body: {
            error: "Too Many Requests",
            message: "Rate limit exceeded",
            code: "RATE_LIMIT_EXCEEDED",
            retryAfter: 60
        }
    },
    {
        status: 500,
        body: {
            error: "Internal Server Error",
            message: "An unexpected error occurred",
            code: "INTERNAL_ERROR",
            traceId: null
        }
    },
    {
        status: 502,
        body: {
            error: "Bad Gateway",
            message: "Upstream service unavailable",
            code: "UPSTREAM_ERROR"
        }
    },
    {
        status: 503,
        body: {
            error: "Service Unavailable",
            message: "Service is temporarily unavailable",
            code: "SERVICE_UNAVAILABLE",
            retryAfter: 30
        }
    }
];

app.all('*', (req, res) => {
    const shouldError = Math.random() < 0.7;
    
    if (shouldError) {
        const scenario = errorScenarios[Math.floor(Math.random() * errorScenarios.length)];
        
        const errorResponse = {
            ...scenario.body,
            timestamp: new Date().toISOString(),
            path: req.originalUrl,
            method: req.method,
            app: 'test-destination-app3-error-simulator',
            port: PORT,
            note: 'This error response came from APP3 (Error Simulator)'
        };
        
        if (scenario.body.requestedPath !== undefined) {
            errorResponse.requestedPath = req.originalUrl;
        }
        
        if (scenario.body.traceId !== undefined) {
            errorResponse.traceId = 'trace-' + Math.random().toString(36).substr(2, 9);
        }
        
        console.log(`Sending ${scenario.status} error response:`, JSON.stringify(errorResponse, null, 2));
        res.status(scenario.status).json(errorResponse);
    } else {
        let statusCode = 200;
        let message = `${req.method} request successfully processed by App3`;
        
        if (req.method === 'POST') {
            statusCode = 201;
            message = 'Resource created successfully by App3';
        } else if (req.method === 'DELETE') {
            statusCode = 204;
            message = 'Resource deleted successfully by App3';
        }

        const successResponse = {
            app: 'test-destination-app3-error-simulator',
            port: PORT,
            timestamp: new Date().toISOString(),
            received: {
                method: req.method,
                url: req.originalUrl,
                path: req.path,
                query: req.query,
                headers: req.headers,
                body: req.body
            },
            message,
            note: 'This success response came from APP3 (Error Simulator)',
            lucky: 'You got a success response! (30% chance)'
        };
        
        console.log(`Sending ${statusCode} success response:`, JSON.stringify(successResponse, null, 2));
        
        if (req.method === 'DELETE') {
            res.status(statusCode).send();
        } else {
            res.status(statusCode).json(successResponse);
        }
    }
});

app.listen(PORT, () => {
    console.log(`Test Destination App3 (Error Simulator) is running on port ${PORT}`);
    console.log(`Ready to receive forwarded traffic from HTTP Shadower`);
    console.log(`This app randomly throws errors (70% chance) or returns success (30% chance)\n`);
});

process.on('SIGTERM', () => {
    console.log('\nApp3 shutting down gracefully...');
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log('\nApp3 shutting down gracefully...');
    process.exit(0);
});