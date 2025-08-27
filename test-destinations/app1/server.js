const express = require('express');
const app = express();
const PORT = process.env.PORT || 3001;

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

app.all('*', (req, res) => {
    let statusCode = 200;
    let message = `${req.method} request successfully processed by App1`;
    
    if (req.method === 'POST') {
        statusCode = 201;
        message = 'Resource created successfully by App1';
    } else if (req.method === 'DELETE') {
        statusCode = 204;
        message = 'Resource deleted successfully by App1';
    }

    const response = {
        app: 'test-destination-app1',
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
        note: 'This response came from APP1 (Response Source)'
    };
    
    console.log(`Sending ${statusCode} response:`, JSON.stringify(response, null, 2));
    
    if (req.method === 'DELETE') {
        res.status(statusCode).send();
    } else {
        res.status(statusCode).json(response);
    }
});

app.listen(PORT, () => {
    console.log(`Test Destination App1 is running on port ${PORT}`);
    console.log(`Ready to receive forwarded traffic from HTTP Shadower`);
    console.log(`All incoming requests will be logged to console\n`);
});

process.on('SIGTERM', () => {
    console.log('\nApp1 shutting down gracefully...');
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log('\nApp1 shutting down gracefully...');
    process.exit(0);
});