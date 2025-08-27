#!/bin/bash

echo "Starting HTTP Shadower Complete Setup..."

if ! command -v node &> /dev/null; then
    echo "Node.js is not installed. Please install Node.js first."
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install Java first."
    exit 1
fi

echo "Building Spring Boot application..."
./gradlew build
if [ $? -ne 0 ]; then
    echo "Failed to build Spring Boot application"
    exit 1
fi

start_node_app() {
    local app_dir=$1
    local app_name=$2
    
    echo "Installing dependencies for $app_name..."
    cd test-destinations/$app_dir
    npm install
    
    echo "Starting $app_name..."
    npm start &
    
    cd ../..
}

cleanup() {
    echo ""
    echo "Shutting down all applications..."
    pkill -f "node.*server.js"
    pkill -f "java.*http-shadower"
    exit 0
}

trap cleanup SIGINT SIGTERM

start_node_app "app1" "App1 (port 3001)"
start_node_app "app2" "App2 (port 3002)"
start_node_app "app3" "App3 Error Simulator (port 3003)"

echo ""
echo "Destination apps are starting up..."
echo "App1 running on http://localhost:3001"
echo "App2 running on http://localhost:3002"
echo "App3 (Error Simulator) running on http://localhost:3003"
echo ""
echo "Waiting for destination apps to be ready..."
sleep 3

echo "Starting HTTP Shadower (Spring Boot) application..."
echo "Application will be available on http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop all applications"
echo "=========================================="

./gradlew bootRun