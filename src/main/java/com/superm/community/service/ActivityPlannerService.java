package com.superm.community.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ActivityPlannerService {
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${tavily.api.key:}")
    private String tavilyApiKey;
    
    private final String STREAMLIT_SCRIPT = """
        import streamlit as st
        import os
        from langchain.agents import create_openai_functions_agent, AgentExecutor
        from langchain_openai import ChatOpenAI
        from langchain.tools import Tool
        from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
        from langchain.schema import HumanMessage, AIMessage
        import json
        import requests
        from datetime import datetime
        import time
        
        # Configure Streamlit page
        st.set_page_config(
            page_title="Multi-Step Activity Planner AI Agent",
            page_icon="🎯",
            layout="wide",
            initial_sidebar_state="expanded"
        )
        
        # Load API keys from environment
        OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
        TAVILY_API_KEY = os.getenv("TAVILY_API_KEY")
        
        if not OPENAI_API_KEY:
            st.error("❌ OPENAI_API_KEY environment variable not set!")
            st.stop()
        
        if not TAVILY_API_KEY:
            st.error("❌ TAVILY_API_KEY environment variable not set!")
            st.stop()
        
        # Initialize ChatOpenAI with GPT-4o
        llm = ChatOpenAI(
            model="gpt-4o",
            api_key=OPENAI_API_KEY,
            temperature=0.7,
            max_tokens=2000
        )
        
        # Custom Planning Tool
        def planning_tool(input_data: str) -> str:
            '''Simulates schedule checking and planning validation.
            This tool should be called FIRST to validate the planning process.
            Returns a success confirmation with planning details.'''
            
            try:
                # Parse input if it's JSON, otherwise treat as plain text
                if input_data.strip().startswith('{'):
                    data = json.loads(input_data)
                    child_age = data.get('age', 'unknown')
                    location = data.get('location', 'unknown location')
                    activity_type = data.get('activity_type', 'general activities')
                else:
                    child_age = "unknown"
                    location = "unknown location"
                    activity_type = input_data
                
                # Simulate planning validation
                planning_result = {
                    "status": "success",
                    "message": f"✅ Planning validated for {child_age} year old in {location}",
                    "planning_phase": "completed",
                    "recommended_activity_type": activity_type,
                    "timestamp": datetime.now().isoformat(),
                    "next_step": "Ready to search for local activity options"
                }
                
                return json.dumps(planning_result, indent=2)
                
            except Exception as e:
                return f"❌ Planning validation failed: {str(e)}"
        
        # Tavily Search Tool
        def tavily_search_tool(query: str) -> str:
            '''Performs real-time web search using Tavily API to find local activities.
            This tool should be called SECOND after planning validation.'''
            
            try:
                url = "https://api.tavily.com/search"
                headers = {
                    "Content-Type": "application/json"
                }
                data = {
                    "api_key": TAVILY_API_KEY,
                    "query": query,
                    "search_depth": "basic",
                    "include_answer": True,
                    "include_raw_content": False,
                    "max_results": 5
                }
                
                response = requests.post(url, headers=headers, json=data)
                
                if response.status_code == 200:
                    result = response.json()
                    search_results = []
                    
                    for item in result.get('results', []):
                        search_results.append({
                            "title": item.get('title', 'No title'),
                            "url": item.get('url', 'No URL'),
                            "content": item.get('content', 'No content')[:200] + "...",
                            "score": item.get('score', 0)
                        })
                    
                    return json.dumps({
                        "search_query": query,
                        "results_count": len(search_results),
                        "results": search_results,
                        "answer": result.get('answer', 'No summary available'),
                        "timestamp": datetime.now().isoformat()
                    }, indent=2)
                else:
                    return f"❌ Search failed with status {response.status_code}: {response.text}"
                    
            except Exception as e:
                return f"❌ Search error: {str(e)}"
        
        # Create tools
        tools = [
            Tool(
                name="PlanningTool",
                description="""Simulates schedule checking and planning validation. 
                MUST be called FIRST it validates the planning process and confirms readiness for activity search.
                Input should be JSON with child age, location, and activity type, or plain text description.""",
                func=planning_tool
            ),
            Tool(
                name="TavilySearchTool", 
                description="""Performs real-time web search to find local activity options.
                MUST be called SECOND after planning validation to search for specific activities in the specified location.
                Input should be a search query like 'indoor activities for 3 year olds in [city]'.""",
                func=tavily_search_tool
            )
        ]
        
        # System prompt with warm, empathetic tone
        system_prompt = """You are a warm, empathetic Multi-Step Activity Planner AI Agent specialized in helping parents find the perfect activities for their children. 

        Your role is to:
        1. **ALWAYS start with PlanningTool** - Validate the planning request and confirm readiness
        2. **THEN use TavilySearchTool** - Search for specific local activity options
        3. **Provide comprehensive recommendations** - Combine planning insights with search results
        
        Key Guidelines:
        - Be warm, understanding, and supportive in your tone
        - Acknowledge the challenges parents face in finding suitable activities
        - Prioritize safety, age-appropriateness, and educational value
        - Consider practical factors like weather, location, and timing
        - Always demonstrate the multi-step process clearly
        
        When a parent asks for activity recommendations:
        1. First, use PlanningTool to validate their request and confirm planning readiness
        2. Then, use TavilySearchTool to search for specific activities in their location
        3. Finally, provide a comprehensive response combining both insights
        
        Be thorough, empathetic, and always show your multi-step reasoning process."""
        
        # Create prompt template
        prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            MessagesPlaceholder(variable_name="chat_history"),
            ("human", "{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad")
        ])
        
        # Create agent
        agent = create_openai_functions_agent(llm, tools, prompt)
        agent_executor = AgentExecutor(
            agent=agent,
            tools=tools,
            verbose=True,
            return_intermediate_steps=True,
            max_iterations=5,
            handle_parsing_errors=True
        )
        
        # Streamlit UI
        st.title("🎯 Multi-Step Activity Planner AI Agent")
        st.markdown("**Powered by GPT-4o with LangChain** | *Helping parents find perfect activities for their children*")
        
        # Sidebar for configuration
        with st.sidebar:
            st.header("⚙️ Configuration")
            st.info(f"**Model:** GPT-4o\\n**Tools:** {len(tools)} tools loaded")
            
            st.subheader("🔑 API Status")
            st.success("✅ OpenAI API Key: Configured")
            st.success("✅ Tavily API Key: Configured")
            
            st.subheader("🛠️ Available Tools")
            for tool in tools:
                st.write(f"• **{tool.name}**: {tool.description[:50]}...")
        
        # Initialize session state
        if "messages" not in st.session_state:
            st.session_state.messages = []
        
        if "chat_history" not in st.session_state:
            st.session_state.chat_history = []
        
        # Display chat history
        st.subheader("💬 Conversation History")
        for message in st.session_state.messages:
            with st.chat_message(message["role"]):
                st.markdown(message["content"])
        
        # Chat input
        if prompt := st.chat_input("Ask for activity recommendations for your child..."):
            # Add user message to chat history
            st.session_state.messages.append({"role": "user", "content": prompt})
            with st.chat_message("user"):
                st.markdown(prompt)
            
            # Show typing indicator
            with st.chat_message("assistant"):
                with st.spinner("🤖 AI Agent is planning and searching..."):
                    try:
                        # Execute agent with verbose logging
                        result = agent_executor.invoke({
                            "input": prompt,
                            "chat_history": st.session_state.chat_history
                        })
                        
                        # Display response
                        response = result["output"]
                        
                        # Show intermediate steps if available
                        if "intermediate_steps" in result:
                            st.subheader("🔍 Multi-Step Process Details")
                            for i, (action, observation) in enumerate(result["intermediate_steps"]):
                                with st.expander(f"Step {i+1}: {action.tool}", expanded=True):
                                    st.write("**Action:**")
                                    st.code(action.tool_input, language="json")
                                    st.write("**Result:**")
                                    st.code(observation, language="json")
                        
                        # Display final response
                        st.markdown("**🤖 AI Agent Response:**")
                        st.markdown(response)
                        
                        # Add to chat history
                        st.session_state.messages.append({"role": "assistant", "content": response})
                        st.session_state.chat_history.extend([
                            HumanMessage(content=prompt),
                            AIMessage(content=response)
                        ])
                        
                    except Exception as e:
                        error_msg = f"❌ Error: {str(e)}"
                        st.error(error_msg)
                        st.session_state.messages.append({"role": "assistant", "content": error_msg})
        
        # Example queries
        st.subheader("💡 Example Queries")
        example_queries = [
            "Find indoor activities for my 3-year-old in Singapore",
            "What are some educational activities for a 5-year-old in Tokyo?",
            "Looking for outdoor activities for toddlers in London",
            "Suggest age-appropriate activities for my 4-year-old in New York"
        ]
        
        for query in example_queries:
            if st.button(f"💬 {query}", key=f"example_{hash(query)}"):
                st.session_state.messages.append({"role": "user", "content": query})
                st.rerun()
        
        # Footer
        st.markdown("---")
        st.markdown("**Multi-Step Activity Planner AI Agent** | Built with Streamlit, LangChain, and OpenAI GPT-4o")
        """;
    
    /**
     * Start the Streamlit Activity Planner service
     */
    public String startActivityPlannerService() {
        try {
            // Create temporary directory for Streamlit app
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "activity-planner");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // Write Streamlit script to file
            File streamlitFile = new File(tempDir, "activity_planner.py");
            try (FileWriter writer = new FileWriter(streamlitFile)) {
                writer.write(STREAMLIT_SCRIPT);
            }
            
            // Create environment file with API keys
            File envFile = new File(tempDir, ".env");
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("OPENAI_API_KEY=" + (openaiApiKey.isEmpty() ? "your-openai-api-key" : openaiApiKey) + "\\n");
                writer.write("TAVILY_API_KEY=" + (tavilyApiKey.isEmpty() ? "your-tavily-api-key" : tavilyApiKey) + "\\n");
            }
            
            // Start Streamlit server asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "streamlit", "run", streamlitFile.getAbsolutePath(),
                        "--server.port", "8502",
                        "--server.address", "0.0.0.0",
                        "--server.headless", "true",
                        "--server.enableCORS", "false"
                    );
                    pb.directory(tempDir);
                    pb.redirectErrorStream(true);
                    
                    Process process = pb.start();
                    
                    // Log the output
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("[Activity Planner] " + line);
                        }
                    }
                    
                    process.waitFor();
                } catch (Exception e) {
                    System.err.println("Error running Streamlit: " + e.getMessage());
                }
            });
            
            return "http://localhost:8502";
            
        } catch (Exception e) {
            return "Error starting service: " + e.getMessage();
        }
    }
    
    /**
     * Get service status
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Check if API keys are configured
        status.put("openaiConfigured", !openaiApiKey.isEmpty());
        status.put("tavilyConfigured", !tavilyApiKey.isEmpty());
        
        // Check if Streamlit is running
        boolean streamlitRunning = false;
        try {
            ProcessBuilder pb = new ProcessBuilder("curl", "-s", "http://localhost:8502");
            Process process = pb.start();
            streamlitRunning = (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0);
        } catch (Exception e) {
            // Ignore
        }
        
        status.put("streamlitRunning", streamlitRunning);
        status.put("serviceUrl", streamlitRunning ? "http://localhost:8502" : "Service not running");
        
        return status;
    }
    
    /**
     * Stop the service
     */
    public String stopActivityPlannerService() {
        try {
            // Kill any running Streamlit processes on port 8502
            ProcessBuilder pb = new ProcessBuilder("pkill", "-f", "streamlit.*8502");
            Process process = pb.start();
            process.waitFor();
            
            return "Activity Planner service stopped successfully";
        } catch (Exception e) {
            return "Error stopping service: " + e.getMessage();
        }
    }
    
    /**
     * Get service documentation
     */
    public Map<String, Object> getServiceDocumentation() {
        Map<String, Object> docs = new HashMap<>();
        
        docs.put("name", "Multi-Step Activity Planner AI Agent");
        docs.put("description", "AI-powered activity planner using GPT-4o with LangChain, featuring multi-step planning and real-time web search");
        docs.put("model", "GPT-4o");
        docs.put("tools", Arrays.asList("PlanningTool", "TavilySearchTool"));
        docs.put("features", Arrays.asList(
            "Multi-step planning process",
            "Real-time web search for local activities",
            "Warm, empathetic AI responses",
            "Verbose logging and process transparency",
            "Age-appropriate activity recommendations",
            "Location-based search capabilities"
        ));
        
        docs.put("setup", Map.of(
            "openaiKey", "Set OPENAI_API_KEY environment variable",
            "tavilyKey", "Set TAVILY_API_KEY environment variable",
            "dependencies", Arrays.asList("streamlit", "langchain", "langchain-openai", "tavily-python")
        ));
        
        return docs;
    }
}
