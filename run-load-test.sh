#!/bin/bash

echo "HTTP Shadower Load Testing Script"
echo "================================="
echo ""

if ! command -v python3 &> /dev/null; then
    echo "Python3 is not installed. Please install Python3 first."
    exit 1
fi

if ! command -v pip3 &> /dev/null && ! command -v pip &> /dev/null; then
    echo "pip is not installed. Please install pip first."
    exit 1
fi

echo "Installing Python dependencies..."
if command -v pip3 &> /dev/null; then
    pip3 install -r requirements.txt --quiet
else
    pip install -r requirements.txt --quiet
fi

echo ""
echo "Starting load test..."
echo "Make sure start-all.sh is running in another terminal!"
echo ""

REQUESTS=${1:-200}
CONCURRENT=${2:-20}
URL=${3:-"http://localhost:8080"}

python3 load_test.py \
    --url "$URL" \
    --requests "$REQUESTS" \
    --concurrent "$CONCURRENT" \
    --wait \
    --endpoints "/" "/api/test" "/health" "/status"

echo ""
echo "Load test completed!"
echo ""
echo "Usage: $0 [requests] [concurrent_users] [url]"
echo "Example: $0 500 50 http://localhost:8080"