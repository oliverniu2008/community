#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "🔧 Loading environment variables from .env file..."
    export $(grep -v '^#' .env | xargs)
    echo "✅ Environment variables loaded successfully!"
    echo "API Key configured: ${OPENAI_API_KEY:0:20}..."
else
    echo "⚠️  .env file not found. Please create one with your API keys."
    echo "📝 Example .env file content:"
    echo "OPENAI_API_KEY=sk-your-actual-api-key-here"
    echo "TAVILY_API_KEY=your-tavily-api-key-here"
    exit 1
fi

echo "🚀 Starting Spring Boot application..."

# Start the application with Maven
mvn spring-boot:run
