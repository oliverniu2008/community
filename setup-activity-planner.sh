#!/bin/bash

echo "🎯 Setting up Multi-Step Activity Planner AI Agent..."

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed. Please install Python 3.8 or higher."
    exit 1
fi

# Check if pip is installed
if ! command -v pip3 &> /dev/null; then
    echo "❌ pip3 is not installed. Please install pip3."
    exit 1
fi

# Install Python dependencies
echo "📦 Installing Python dependencies..."
pip3 install -r requirements.txt

# Check if Streamlit is installed
if ! command -v streamlit &> /dev/null; then
    echo "❌ Streamlit installation failed. Please check your Python environment."
    exit 1
fi

echo "✅ Python dependencies installed successfully!"

# Create environment file template
echo "🔧 Creating environment configuration..."
cat > .env.template << EOF
# OpenAI API Configuration
OPENAI_API_KEY=your-openai-api-key-here

# Tavily API Configuration  
TAVILY_API_KEY=your-tavily-api-key-here

# Streamlit Configuration
STREAMLIT_SERVER_PORT=8502
STREAMLIT_SERVER_ADDRESS=0.0.0.0
STREAMLIT_SERVER_HEADLESS=true
STREAMLIT_SERVER_ENABLE_CORS=false
EOF

echo "📋 Environment template created: .env.template"
echo ""
echo "🔑 Setup Instructions:"
echo "1. Copy .env.template to .env"
echo "2. Add your OpenAI API key to OPENAI_API_KEY"
echo "3. Add your Tavily API key to TAVILY_API_KEY"
echo "4. Run the Spring Boot application"
echo "5. Navigate to /admin/activity-planner in the admin panel"
echo ""
echo "🌐 The Activity Planner will be available at: http://localhost:8502"
echo ""
echo "✅ Setup complete! Happy planning! 🎯"
