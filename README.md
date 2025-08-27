# HTTP Shadower

**Safely test your applications with real production traffic** without impacting your users.

## Requirements

- Java 21+
- Gradle 8+
- Node.js 18+ (for test destinations)

HTTP Shadower is a Spring Boot application that intercepts production HTTP requests and intelligently forwards them to multiple environments (DEV/ITG/STAGE) while ensuring your users always receive responses from your production system.

### **Test with Real Production Traffic**
No need to rely on synthetic test data. HTTP Shadower lets you validate your ITG, STAGE, and DEV environments using actual production requests, giving you confidence that your changes work with real-world scenarios.

### **Reduced Risk to Production**
Your users always receive responses from your production environment. Shadow environments receive identical traffic but their responses are safely ignored, meaning bugs in your test environments never affect your customers.

### **Perfect for CI/CD Pipelines**
- **Pre-deployment validation**: Test new features against real traffic before going live
- **Regression testing**: Ensure new deployments handle production workloads correctly  
- **Performance testing**: Validate that your staging environment can handle production load
- **API compatibility**: Verify new API versions work with existing client requests

### **Debug with Confidence**
Forward production issues to your development environment where you can debug with real data, authentication headers, and actual user workflows.

## Common Use Cases

### 1. **Staging Environment Validation**
Forward 100% of production API traffic to your staging environment to ensure it handles real-world scenarios before deployment.

### 2. **New Feature Testing**  
Deploy new features to a separate environment and shadow production traffic to validate behavior without risking user experience.

### 3. **Database Migration Testing**
Test database schema changes against real query patterns by forwarding production traffic to environments with new database structures.

### 4. **Load Testing with Real Patterns**
Use actual production traffic patterns and volumes to load test your infrastructure instead of artificial load testing tools.

### 5. **API Version Compatibility**
Ensure new API versions are compatible with existing clients by forwarding real client requests to both old and new API versions.

## How It Works

The HTTP Shadower enables you to:
- **Mirror production requests** to DEV/ITG/STAGE environments in real-time
- **Preserve all authentication** (Bearer tokens, API keys, session cookies)
- **Maintain request integrity** (headers, query parameters, request bodies)
- **Choose response source** - users get production responses while test environments process identical requests
- **Handle failures gracefully** - if test environments are down, production continues normally

## Architecture

```
Client Request → HTTP Shadower → Response Source (synchronous)
                              → Shadow Destinations (async, fire-and-forget)
                              ↓
Client Response ← Response from designated source
```

The application uses a hybrid approach:
- **One destination** is designated as the response source (synchronous)
- **Other destinations** receive identical traffic asynchronously
- **Client always receives** the response from the designated source

## Configuration

### Application Configuration

Configure destinations in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: http-shadower

server:
  port: 8080

shadower:
  destinations:
    - name: production
      url: http://prod-api.example.com
      enabled: true
      responseSource: true
    - name: staging
      url: http://staging-api.example.com
      enabled: true
      responseSource: false
    - name: development
      url: http://dev-api.example.com
      enabled: false
      responseSource: false

logging:
  level:
    com.mugentwo.http_shadower: INFO
```

### Configuration Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Friendly name for the destination |
| `url` | String | Base URL of the destination application |
| `enabled` | Boolean | Whether to forward traffic to this destination (default: true) |
| `responseSource` | Boolean | Whether this destination's response should be returned to the client (default: false) |

### Configuration Rules

**IMPORTANT**: The application validates configuration at startup and will crash if these rules are violated:

1. **Exactly one enabled destination** must have `responseSource: true`
2. **At least one destination** must be `enabled: true`
3. **Multiple response sources** will cause startup failure
4. **No response source configured** will cause startup failure
5. Disabled destinations are completely ignored during validation

## Quick Start

### 1. Start Everything at Once

The easiest way to get started is to run the complete setup script:

```bash
./start-all.sh
```

This script will:
1. Build the Spring Boot application
2. Start Node.js destination simulators (App1 on port 3001, App2 on port 3002)
3. Start the HTTP Shadower application (port 8080) in the foreground with logs
4. Handle graceful shutdown when you press Ctrl+C

### 2. Send Test Requests

See the [Testing with Curl Examples](#testing-with-curl-examples) section below for comprehensive test commands.

### Alternative: Manual Startup

If you prefer to run components separately:

```bash
# Terminal 1: Start destination apps
cd test-destinations/app1 && npm install && npm start &
cd test-destinations/app2 && npm install && npm start &
cd test-destinations/app3 && npm install && npm start &

# Terminal 2: Build and start HTTP Shadower
./gradlew build
./gradlew bootRun
```

## Traffic Forwarding

### What Gets Forwarded

The HTTP Shadower preserves and forwards:

- **HTTP Method** (GET, POST, PUT, DELETE, PATCH, etc.)
- **Request Path** and query parameters
- **All Headers** (except `host` to prevent conflicts)
- **Request Body** (for methods that support it)
- **Authorization headers** (Bearer tokens, API keys, etc.)

### Response Handling

- **Client receives**: Response from the designated response source
- **Status codes**: Preserved from the response source (200, 201, 404, 500, etc.)
- **Response body**: Exact response from the designated source
- **Headers**: Response headers from the designated source

### Error Handling

- **Response source fails**: Attempts fallback to first enabled destination
- **All destinations fail**: Returns HTTP 503 Service Unavailable
- **Shadow destination failures**: Logged but don't affect client response

## Use Cases

### 1. Production Traffic Shadowing

```yaml
shadower:
  destinations:
    - name: production
      url: https://api.yourcompany.com
      enabled: true
      responseSource: true
    - name: staging-test
      url: https://staging.yourcompany.com
      enabled: true
      responseSource: false
```

Users get production responses while staging receives identical traffic for testing.

### 2. A/B Testing Preparation

```yaml
shadower:
  destinations:
    - name: current-version
      url: http://v1.api.local
      enabled: true
      responseSource: true
    - name: new-version
      url: http://v2.api.local
      enabled: true
      responseSource: false
```

Test new API version with real traffic before switching over.

### 3. Load Testing

```yaml
shadower:
  destinations:
    - name: main-api
      url: http://main.api.local
      enabled: true
      responseSource: true
    - name: load-test-1
      url: http://test1.api.local
      enabled: true
      responseSource: false
    - name: load-test-2
      url: http://test2.api.local
      enabled: true
      responseSource: false
```

Send production traffic to multiple test environments simultaneously.

## Monitoring and Debugging

### Logging

The application provides detailed logging at different levels:

```yaml
logging:
  level:
    com.mugentwo.http_shadower: DEBUG
```

Log levels:
- **INFO**: Request forwarding summary
- **DEBUG**: Detailed request/response information
- **ERROR**: Destination failures and fallback attempts

### Health Checks

The application includes Spring Boot Actuator endpoints:

```bash
curl http://localhost:8080/actuator/health
```

## Simulation and Testing

### Running the Complete Simulation

1. **Start the complete environment:**
   ```bash
   ./start-all.sh
   ```
   
   This single command starts everything and shows the Spring Boot logs in the foreground.

2. **In another terminal, send various requests:**
   ```bash
   # Test different HTTP methods
   curl http://localhost:8080/api/test
   curl -X POST http://localhost:8080/api/test -d '{"test": "data"}'
   curl -X PUT http://localhost:8080/api/test/1 -d '{"update": true}'
   curl -X DELETE http://localhost:8080/api/test/1
   ```

3. **Observe the behavior:**
   - **Your curl terminal**: Receives responses from App1 (response source)
   - **Main terminal**: Shows Spring Boot logs and forwarding activity
   - **Background**: App1 and App2 process requests (App1 synchronously, App2 asynchronously)

### Sample Response

When you send a request, you'll receive a response like:

```json
{
  "app": "test-destination-app1",
  "port": 3001,
  "timestamp": "2025-08-27T12:30:45.123Z",
  "received": {
    "method": "POST",
    "url": "/api/users",
    "path": "/api/users",
    "query": {},
    "headers": {
      "content-type": "application/json",
      "authorization": "Bearer your-token"
    },
    "body": {
      "name": "John Doe",
      "email": "john@example.com"
    }
  },
  "message": "Resource created successfully by App1",
  "note": "This response came from APP1 (Response Source)"
}
```

### Testing Different Scenarios

#### Scenario 1: Response Source Failure
1. Stop App1 (response source)
2. Send requests - should get responses from App2 (fallback)

#### Scenario 2: Shadow Destination Failure
1. Stop App2 (shadow destination)
2. Send requests - should still get responses from App1 normally

#### Scenario 3: All Destinations Down
1. Stop both apps
2. Send requests - should get HTTP 503 Service Unavailable

## Development

### Prerequisites

- Java 21+
- Gradle 8+
- Node.js 18+ (for test destinations)

### Building

```bash
# Build the application
./gradlew build

# Run tests
./gradlew test

# Create executable JAR
./gradlew bootJar
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests HttpForwardingServiceTest
```

## Deployment

### JAR Deployment

```bash
# Build executable JAR
./gradlew bootJar

# Run the JAR
java -jar build/libs/http-shadower-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

```dockerfile
FROM openjdk:21-jre-slim
COPY build/libs/http-shadower-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Configuration Override

Override configuration via environment variables or external files:

```bash
# Via external config
java -jar app.jar --spring.config.location=file:./config/application.yml
```

### Configuration Validation Errors

If the application fails to start due to invalid configuration, you'll see error messages like:

```
IllegalStateException: Exactly one enabled destination must be configured as responseSource
IllegalStateException: Only one destination can be configured as responseSource, found: 2
IllegalStateException: At least one destination must be enabled
```

**Common fixes:**
- Ensure exactly one enabled destination has `responseSource: true`
- Set at least one destination to `enabled: true`
- Remove `responseSource: true` from additional destinations

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass: `./gradlew test`
5. Submit a pull request

## Testing with Curl Examples

Once the application is running, you can test it with these curl commands:

### Basic Requests

```bash
# Basic GET request
curl -v http://localhost:8080/api/users

# GET with query parameters
curl -v "http://localhost:8080/api/users?page=1&limit=10&sort=name&filter=active"

# DELETE request
curl -v -X DELETE http://localhost:8080/api/users/123 \
  -H "Authorization: Bearer test-token-123"
```

### Authentication Methods

```bash
# POST with Bearer Token
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token-123" \
  -d '{"name": "John Doe", "email": "john@example.com", "role": "admin"}'

# POST with Basic Authentication
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -u "username:password" \
  -d '{"remember": true, "device": "web"}'

# PUT with JWT Token
curl -X PUT http://localhost:8080/api/users/123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" \
  -d '{"name": "Jane Smith", "email": "jane@example.com", "status": "active"}'
```

### Complex Requests

```bash
# POST with multiple headers and nested JSON
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token" \
  -H "X-Request-ID: req-12345" \
  -H "X-Client-Version: 1.2.3" \
  -d '{
    "items": [
      {"product_id": "abc123", "quantity": 2},
      {"product_id": "def456", "quantity": 1}
    ],
    "shipping_address": {
      "street": "123 Main St",
      "city": "Springfield",
      "zip": "12345"
    }
  }'
```

### Testing Error Forwarding

With App3 configured as the response source, test how errors are forwarded:

```bash
# Random error or success (70% error chance, 30% success chance)
curl -v http://localhost:8080/api/test-errors

# Try multiple requests to see different error types
for i in {1..5}; do
  echo "Request $i:"
  curl -s http://localhost:8080/api/random-test
done
```

**Expected Error Responses from the test-destinations app3 (the simulator):**
- **400 Bad Request**: Invalid parameters
- **401 Unauthorized**: Authentication required  
- **403 Forbidden**: Access denied
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource already exists
- **422 Unprocessable Entity**: Validation errors
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server error
- **502 Bad Gateway**: Upstream service error
- **503 Service Unavailable**: Service temporarily down

### What You'll See

When you run these commands:
- **Your terminal**: Receives JSON responses from the configured response source app
- **start-all.sh terminal**: Shows Spring Boot logs with forwarding details
- **Response format**: JSON showing which app responded and all the request details that were forwarded

The responses will indicate which destination app provided the response and include all the headers, body, and parameters that were forwarded.
