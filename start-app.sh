#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "🔧 Loading environment variables from .env file..."
    export $(grep -v '^#' .env | xargs)
    echo "✅ Environment variables loaded successfully!"
else
    echo "⚠️  .env file not found. Please create one with your API keys."
    exit 1
fi

echo "🚀 Starting Spring Boot application..."
echo "API Key configured: ${OPENAI_API_KEY:0:20}..."

# Start the application
./mvnw spring-boot:run
