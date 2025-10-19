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
    
    @Value("${openai.api.key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;
    
    @Value("${tavily.api.key:${TAVILY_API_KEY:}}")
    private String tavilyApiKey;
    
    // In-memory storage for planning results (in production, this would be a database)
    private final Map<String, Map<String, Object>> planningResults = new HashMap<>();
    private final List<Map<String, Object>> recentPlans = new ArrayList<>();
    private final Map<String, List<Map<String, Object>>> detailedResultsStorage = new HashMap<>();
    
    // Initialize with sample data
    public ActivityPlannerService() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        // Initialize sample planning results
        addSamplePlan("plan_001", "Find indoor activities for my 3-year-old in Singapore", "3", "Singapore", "indoor");
        addSamplePlan("plan_002", "Educational activities for a 5-year-old in Tokyo", "5", "Tokyo", "educational");
        addSamplePlan("plan_003", "Outdoor activities for toddlers in London", "2", "London", "outdoor");
        
        // Initialize detailed results for sample plans
        detailedResultsStorage.put("plan_001", createSingaporeActivities());
        detailedResultsStorage.put("plan_002", createTokyoActivities());
        detailedResultsStorage.put("plan_003", createLondonActivities());
    }
    
    private void addSamplePlan(String planId, String query, String childAge, String location, String activityType) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("id", planId);
        plan.put("timestamp", getCurrentTimestamp());
        plan.put("query", query);
        plan.put("childAge", childAge);
        plan.put("location", location);
        plan.put("activityType", activityType);
        plan.put("status", "completed");
        plan.put("resultsCount", 3);
        plan.put("summary", generateSummary(childAge, location, activityType));
        
        recentPlans.add(0, plan); // Add to beginning of list
        if (recentPlans.size() > 10) {
            recentPlans.remove(recentPlans.size() - 1); // Keep only last 10
        }
    }
    
    private String getCurrentTimestamp() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private String generateSummary(String childAge, String location, String activityType) {
        return String.format("Found %d %s activities suitable for %s-year-olds in %s, featuring safe, age-appropriate experiences with educational value.", 
            3, activityType, childAge, location);
    }
    
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
                description="Simulates schedule checking and planning validation. MUST be called FIRST to validate the planning process and confirms readiness for activity search. Input should be JSON with child age, location, and activity type, or plain text description.",
                func=planning_tool
            ),
            Tool(
                name="TavilySearchTool", 
                description="Performs real-time web search to find local activity options. MUST be called SECOND after planning validation to search for specific activities in the specified location. Input should be a search query like 'indoor activities for 3 year olds in [city]'.",
                func=tavily_search_tool
            )
        ]
        
        # System prompt with warm, empathetic tone
        system_prompt = "You are a warm, empathetic Multi-Step Activity Planner AI Agent specialized in helping parents find the perfect activities for their children. Your role is to: 1. ALWAYS start with PlanningTool - Validate the planning request and confirm readiness 2. THEN use TavilySearchTool - Search for specific local activity options 3. Provide comprehensive recommendations - Combine planning insights with search results. Key Guidelines: Be warm, understanding, and supportive in your tone. Acknowledge the challenges parents face in finding suitable activities. Prioritize safety, age-appropriateness, and educational value. Consider practical factors like weather, location, and timing. Always demonstrate the multi-step process clearly. When a parent asks for activity recommendations: First, use PlanningTool to validate their request and confirm planning readiness. Then, use TavilySearchTool to search for specific activities in their location. Finally, provide a comprehensive response combining both insights. Be thorough, empathetic, and always show your multi-step reasoning process."
        
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
                String openaiKey = openaiApiKey.isEmpty() ? System.getenv("OPENAI_API_KEY") : openaiApiKey;
                String tavilyKey = tavilyApiKey.isEmpty() ? System.getenv("TAVILY_API_KEY") : tavilyApiKey;
                
                writer.write("OPENAI_API_KEY=" + (openaiKey != null ? openaiKey : "your-openai-api-key") + "\\n");
                writer.write("TAVILY_API_KEY=" + (tavilyKey != null ? tavilyKey : "your-tavily-api-key") + "\\n");
            }
            
            // Start Streamlit server asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "python3", "-m", "streamlit", "run", streamlitFile.getAbsolutePath(),
                        "--server.port", "8502",
                        "--server.address", "0.0.0.0",
                        "--server.headless", "true",
                        "--server.enableCORS", "false"
                    );
                    pb.directory(tempDir);
                    pb.redirectErrorStream(true);
                    
                    // Set environment variables for the Streamlit process
                    Map<String, String> env = pb.environment();
                    String openaiKey = openaiApiKey.isEmpty() ? System.getenv("OPENAI_API_KEY") : openaiApiKey;
                    String tavilyKey = tavilyApiKey.isEmpty() ? System.getenv("TAVILY_API_KEY") : tavilyApiKey;
                    
                    if (openaiKey != null && !openaiKey.isEmpty()) {
                        env.put("OPENAI_API_KEY", openaiKey);
                    }
                    if (tavilyKey != null && !tavilyKey.isEmpty()) {
                        env.put("TAVILY_API_KEY", tavilyKey);
                    }
                    
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
        
        // Check if API keys are configured (from properties or environment)
        String openaiKey = openaiApiKey.isEmpty() ? System.getenv("OPENAI_API_KEY") : openaiApiKey;
        String tavilyKey = tavilyApiKey.isEmpty() ? System.getenv("TAVILY_API_KEY") : tavilyApiKey;
        
        // Debug logging
        System.out.println("Debug - openaiApiKey from @Value: " + (openaiApiKey.isEmpty() ? "empty" : "configured"));
        System.out.println("Debug - openaiKey from env: " + (openaiKey != null && !openaiKey.isEmpty() ? "configured" : "empty"));
        System.out.println("Debug - tavilyKey from env: " + (tavilyKey != null && !tavilyKey.isEmpty() ? "configured" : "empty"));
        
        status.put("openaiConfigured", openaiKey != null && !openaiKey.isEmpty());
        status.put("tavilyConfigured", tavilyKey != null && !tavilyKey.isEmpty());
        
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
            ProcessBuilder pb = new ProcessBuilder("pkill", "-f", "python3.*streamlit.*8502");
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
    
    /**
     * Get recent activity planning results for display
     */
    public Map<String, Object> getRecentResults() {
        Map<String, Object> results = new HashMap<>();
        
        results.put("recentPlans", new ArrayList<>(recentPlans));
        results.put("totalPlans", recentPlans.size());
        results.put("lastUpdated", getCurrentTimestamp());
        
        return results;
    }
    
    /**
     * Create a new planning session (simulate AI planning)
     */
    public Map<String, Object> createNewPlanningSession(String query, String childAge, String location, String activityType) {
        String planId = "plan_" + System.currentTimeMillis();
        
        Map<String, Object> plan = new HashMap<>();
        plan.put("id", planId);
        plan.put("timestamp", getCurrentTimestamp());
        plan.put("query", query);
        plan.put("childAge", childAge);
        plan.put("location", location);
        plan.put("activityType", activityType);
        plan.put("status", "completed");
        plan.put("resultsCount", 3);
        plan.put("summary", generateSummary(childAge, location, activityType));
        
        // Generate detailed results for the new planning session
        List<Map<String, Object>> activities = generateActivitiesForLocation(location, activityType, childAge);
        detailedResultsStorage.put(planId, activities);
        
        recentPlans.add(0, plan); // Add to beginning of list
        if (recentPlans.size() > 10) {
            recentPlans.remove(recentPlans.size() - 1); // Keep only last 10
        }
        
        return plan;
    }
    
    /**
     * Get random example queries for creating new planning sessions
     */
    public List<Map<String, Object>> getExampleQueries() {
        List<Map<String, Object>> examples = new ArrayList<>();
        
        Map<String, Object> example1 = new HashMap<>();
        example1.put("query", "Find outdoor activities for my 4-year-old in New York");
        example1.put("childAge", "4");
        example1.put("location", "New York");
        example1.put("activityType", "outdoor");
        examples.add(example1);
        
        Map<String, Object> example2 = new HashMap<>();
        example2.put("query", "Looking for creative activities for a 6-year-old in Paris");
        example2.put("childAge", "6");
        example2.put("location", "Paris");
        example2.put("activityType", "creative");
        examples.add(example2);
        
        Map<String, Object> example3 = new HashMap<>();
        example3.put("query", "Suggest sports activities for my 7-year-old in Sydney");
        example3.put("childAge", "7");
        example3.put("location", "Sydney");
        example3.put("activityType", "sports");
        examples.add(example3);
        
        Map<String, Object> example4 = new HashMap<>();
        example4.put("query", "Find music activities for my 5-year-old in Berlin");
        example4.put("childAge", "5");
        example4.put("location", "Berlin");
        example4.put("activityType", "music");
        examples.add(example4);
        
        return examples;
    }
    
    /**
     * Get detailed results for a specific planning session
     */
    public Map<String, Object> getDetailedResults(String planId) {
        Map<String, Object> detailedResults = new HashMap<>();
        
        // Check if we have detailed results stored for this plan ID
        if (detailedResultsStorage.containsKey(planId)) {
            // Find the plan details from recent plans
            Map<String, Object> planDetails = recentPlans.stream()
                .filter(plan -> planId.equals(plan.get("id")))
                .findFirst()
                .orElse(null);
            
            if (planDetails != null) {
                detailedResults.put("planId", planId);
                detailedResults.put("query", planDetails.get("query"));
                detailedResults.put("childAge", planDetails.get("childAge"));
                detailedResults.put("location", planDetails.get("location"));
                detailedResults.put("activityType", planDetails.get("activityType"));
                detailedResults.put("timestamp", planDetails.get("timestamp"));
                detailedResults.put("status", planDetails.get("status"));
                
                List<Map<String, Object>> activities = detailedResultsStorage.get(planId);
                detailedResults.put("activities", activities);
                detailedResults.put("summary", planDetails.get("summary"));
                detailedResults.put("recommendations", generateRecommendations(
                    planDetails.get("location").toString(), 
                    planDetails.get("activityType").toString(), 
                    planDetails.get("childAge").toString()));
            } else {
                // Fallback to hardcoded plans for sample data
                return getHardcodedDetailedResults(planId);
            }
        } else {
            // Fallback to hardcoded plans for sample data
            return getHardcodedDetailedResults(planId);
        }
        
        return detailedResults;
    }
    
    private Map<String, Object> getHardcodedDetailedResults(String planId) {
        Map<String, Object> detailedResults = new HashMap<>();
        
        // Sample detailed results for different plans
        switch (planId) {
            case "plan_001":
                detailedResults.put("planId", planId);
                detailedResults.put("query", "Find indoor activities for my 3-year-old in Singapore");
                detailedResults.put("childAge", "3");
                detailedResults.put("location", "Singapore");
                detailedResults.put("activityType", "indoor");
                detailedResults.put("timestamp", "2024-01-15 10:30:00");
                detailedResults.put("status", "completed");
                
                List<Map<String, Object>> activities = new ArrayList<>();
                
                Map<String, Object> activity1 = new HashMap<>();
                activity1.put("name", "KidZania Singapore");
                activity1.put("type", "Indoor Playground");
                activity1.put("ageRange", "3-14 years");
                activity1.put("description", "Educational role-playing theme park where children can explore different careers");
                activity1.put("location", "Sentosa Island, Singapore");
                activity1.put("price", "S$58-68");
                activity1.put("duration", "4-6 hours");
                activity1.put("rating", "4.5/5");
                activity1.put("safety", "Highly supervised, child-safe environment");
                activity1.put("educationalValue", "High - career exploration and social skills");
                activity1.put("url", "https://singapore.kidzania.com");
                activities.add(activity1);
                
                Map<String, Object> activity2 = new HashMap<>();
                activity2.put("name", "ArtScience Museum");
                activity2.put("type", "Children's Museum");
                activity2.put("ageRange", "All ages");
                activity2.put("description", "Interactive exhibits combining art, science, and technology");
                activity2.put("location", "Marina Bay Sands, Singapore");
                activity2.put("price", "S$21-32");
                activity2.put("duration", "2-3 hours");
                activity2.put("rating", "4.3/5");
                activity2.put("safety", "Child-friendly with staff supervision");
                activity2.put("educationalValue", "High - STEAM learning");
                activity2.put("url", "https://www.marinabaysands.com/museum");
                activities.add(activity2);
                
                Map<String, Object> activity3 = new HashMap<>();
                activity3.put("name", "The Artground");
                activity3.put("type", "Art Classes");
                activity3.put("ageRange", "2-12 years");
                activity3.put("description", "Creative arts and crafts workshops for children");
                activity3.put("location", "Goodman Arts Centre, Singapore");
                activity3.put("price", "S$25-35 per session");
                activity3.put("duration", "1-2 hours");
                activity3.put("rating", "4.6/5");
                activity3.put("safety", "Small class sizes, parent-friendly");
                activity3.put("educationalValue", "High - creativity and fine motor skills");
                activity3.put("url", "https://www.theartground.com.sg");
                activities.add(activity3);
                
                detailedResults.put("activities", activities);
                detailedResults.put("summary", "Found 3 excellent indoor activities for your 3-year-old in Singapore. All venues prioritize safety and offer age-appropriate experiences with educational value.");
                detailedResults.put("recommendations", Arrays.asList(
                    "Consider KidZania for a full-day adventure with educational benefits",
                    "ArtScience Museum offers shorter visits perfect for toddlers",
                    "The Artground provides hands-on creative experiences"
                ));
                break;
                
            case "plan_002":
                detailedResults.put("planId", planId);
                detailedResults.put("query", "Educational activities for a 5-year-old in Tokyo");
                detailedResults.put("childAge", "5");
                detailedResults.put("location", "Tokyo");
                detailedResults.put("activityType", "educational");
                detailedResults.put("timestamp", "2024-01-15 09:15:00");
                detailedResults.put("status", "completed");
                
                List<Map<String, Object>> tokyoActivities = new ArrayList<>();
                
                Map<String, Object> tokyoActivity1 = new HashMap<>();
                tokyoActivity1.put("name", "National Museum of Emerging Science and Innovation (Miraikan)");
                tokyoActivity1.put("type", "Science Museum");
                tokyoActivity1.put("ageRange", "4-12 years");
                tokyoActivity1.put("description", "Interactive science exhibits and demonstrations perfect for curious minds");
                tokyoActivity1.put("location", "Odaiba, Tokyo");
                tokyoActivity1.put("price", "¥630");
                tokyoActivity1.put("duration", "3-4 hours");
                tokyoActivity1.put("rating", "4.4/5");
                tokyoActivity1.put("safety", "Family-friendly with English support");
                tokyoActivity1.put("educationalValue", "Excellent - hands-on science learning");
                tokyoActivity1.put("url", "https://www.miraikan.jst.go.jp/en/");
                tokyoActivities.add(tokyoActivity1);
                
                Map<String, Object> tokyoActivity2 = new HashMap<>();
                tokyoActivity2.put("name", "Tokyo Metropolitan Children's Hall");
                tokyoActivity2.put("type", "Children's Center");
                tokyoActivity2.put("ageRange", "3-12 years");
                tokyoActivity2.put("description", "Multi-purpose facility with play areas, workshops, and educational programs");
                tokyoActivity2.put("location", "Shibuya, Tokyo");
                tokyoActivity2.put("price", "Free admission");
                tokyoActivity2.put("duration", "2-3 hours");
                tokyoActivity2.put("rating", "4.2/5");
                tokyoActivity2.put("safety", "Supervised activities and safe play areas");
                tokyoActivity2.put("educationalValue", "High - diverse learning experiences");
                tokyoActivity2.put("url", "https://www.children.metro.tokyo.lg.jp/");
                tokyoActivities.add(tokyoActivity2);
                
                Map<String, Object> tokyoActivity3 = new HashMap<>();
                tokyoActivity3.put("name", "KidZania Tokyo");
                tokyoActivity3.put("type", "Educational Theme Park");
                tokyoActivity3.put("ageRange", "3-15 years");
                tokyoActivity3.put("description", "Role-playing activities where children can experience different professions");
                tokyoActivity3.put("location", "Lalaport Toyosu, Tokyo");
                tokyoActivity3.put("price", "¥3,500-4,000");
                tokyoActivity3.put("duration", "4-6 hours");
                tokyoActivity3.put("rating", "4.6/5");
                tokyoActivity3.put("safety", "Highly supervised, child-safe environment");
                tokyoActivity3.put("educationalValue", "Excellent - career exploration and life skills");
                tokyoActivity3.put("url", "https://tokyo.kidzania.com/en");
                tokyoActivities.add(tokyoActivity3);
                
                detailedResults.put("activities", tokyoActivities);
                detailedResults.put("summary", "Discovered 3 outstanding educational activities in Tokyo perfect for your 5-year-old. Each venue offers unique learning experiences that combine fun with education.");
                detailedResults.put("recommendations", Arrays.asList(
                    "Miraikan is perfect for science-curious children with hands-on exhibits",
                    "Children's Hall offers free admission and diverse activities",
                    "KidZania Tokyo provides immersive career exploration experiences"
                ));
                break;
                
            case "plan_003":
                detailedResults.put("planId", planId);
                detailedResults.put("query", "Outdoor activities for toddlers in London");
                detailedResults.put("childAge", "2");
                detailedResults.put("location", "London");
                detailedResults.put("activityType", "outdoor");
                detailedResults.put("timestamp", "2024-01-14 16:45:00");
                detailedResults.put("status", "completed");
                
                List<Map<String, Object>> londonActivities = new ArrayList<>();
                
                Map<String, Object> londonActivity1 = new HashMap<>();
                londonActivity1.put("name", "Hyde Park Playground");
                londonActivity1.put("type", "Outdoor Playground");
                londonActivity1.put("ageRange", "1-12 years");
                londonActivity1.put("description", "Large playground with age-appropriate equipment and open green spaces");
                londonActivity1.put("location", "Hyde Park, London");
                londonActivity1.put("price", "Free admission");
                londonActivity1.put("duration", "1-2 hours");
                londonActivity1.put("rating", "4.3/5");
                londonActivity1.put("safety", "Well-maintained equipment with safety surfaces");
                londonActivity1.put("educationalValue", "Good - physical development and social interaction");
                londonActivity1.put("url", "https://www.royalparks.org.uk/parks/hyde-park");
                londonActivities.add(londonActivity1);
                
                Map<String, Object> londonActivity2 = new HashMap<>();
                londonActivity2.put("name", "Kensington Gardens Playground");
                londonActivity2.put("type", "Adventure Playground");
                londonActivity2.put("ageRange", "2-12 years");
                londonActivity2.put("description", "Natural adventure playground with climbing structures and water play");
                londonActivity2.put("location", "Kensington Gardens, London");
                londonActivity2.put("price", "Free admission");
                londonActivity2.put("duration", "1-3 hours");
                londonActivity2.put("rating", "4.5/5");
                londonActivity2.put("safety", "Supervised areas with natural play elements");
                londonActivity2.put("educationalValue", "High - nature exploration and creativity");
                londonActivity2.put("url", "https://www.royalparks.org.uk/parks/kensington-gardens");
                londonActivities.add(londonActivity2);
                
                Map<String, Object> londonActivity3 = new HashMap<>();
                londonActivity3.put("name", "London Zoo");
                londonActivity3.put("type", "Outdoor Zoo");
                londonActivity3.put("ageRange", "All ages");
                londonActivity3.put("description", "World-famous zoo with interactive exhibits and outdoor animal encounters");
                londonActivity3.put("location", "Regent's Park, London");
                londonActivity3.put("price", "£25-35");
                londonActivity3.put("duration", "3-4 hours");
                londonActivity3.put("rating", "4.2/5");
                londonActivity3.put("safety", "Family-friendly with stroller access");
                londonActivity3.put("educationalValue", "Excellent - wildlife education and conservation awareness");
                londonActivity3.put("url", "https://www.londonzoo.org/");
                londonActivities.add(londonActivity3);
                
                detailedResults.put("activities", londonActivities);
                detailedResults.put("summary", "Located 3 wonderful outdoor activities in London suitable for your 2-year-old. All venues offer safe, engaging environments perfect for toddler exploration and development.");
                detailedResults.put("recommendations", Arrays.asList(
                    "Hyde Park Playground offers free fun with age-appropriate equipment",
                    "Kensington Gardens provides natural adventure play experiences",
                    "London Zoo combines outdoor fun with educational animal encounters"
                ));
                break;
                
            default:
                // Return empty result for unknown plan IDs
                detailedResults.put("planId", planId);
                detailedResults.put("error", "No detailed results found for this planning session");
                detailedResults.put("activities", Arrays.asList());
                detailedResults.put("summary", "This planning session is still in progress or has no detailed results available.");
                break;
        }
        
        return detailedResults;
    }
    
    /**
     * Generate activities for a specific location, activity type, and child age
     */
    private List<Map<String, Object>> generateActivitiesForLocation(String location, String activityType, String childAge) {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // Generate 3 sample activities based on the location and activity type
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> activity = new HashMap<>();
            
            String activityName = generateActivityName(location, activityType, i);
            String activityTypeName = generateActivityTypeName(activityType);
            
            activity.put("name", activityName);
            activity.put("type", activityTypeName);
            activity.put("ageRange", childAge + "-" + (Integer.parseInt(childAge) + 5) + " years");
            activity.put("description", generateActivityDescription(activityName, activityType));
            activity.put("location", location);
            activity.put("price", generatePrice(location));
            activity.put("duration", generateDuration(activityType));
            activity.put("rating", generateRating());
            activity.put("safety", "Child-safe environment with proper supervision");
            activity.put("educationalValue", generateEducationalValue(activityType));
            activity.put("url", generateRealUrl(location, activityType, i));
            
            activities.add(activity);
        }
        
        return activities;
    }
    
    private String generateActivityName(String location, String activityType, int index) {
        String[] indoorNames = {"Creative Kids Center", "Little Explorers Hub", "Play & Learn Studio"};
        String[] outdoorNames = {"Adventure Park", "Nature Discovery Center", "Sports & Fun Zone"};
        String[] educationalNames = {"Learning Academy", "Science Discovery Center", "Art & Culture Studio"};
        String[] creativeNames = {"Art Studio", "Music Academy", "Dance Workshop"};
        String[] sportsNames = {"Sports Center", "Swimming Academy", "Gymnastics Club"};
        String[] musicNames = {"Music School", "Orchestra Academy", "Singing Studio"};
        
        String[] names;
        switch (activityType.toLowerCase()) {
            case "indoor": names = indoorNames; break;
            case "outdoor": names = outdoorNames; break;
            case "educational": names = educationalNames; break;
            case "creative": names = creativeNames; break;
            case "sports": names = sportsNames; break;
            case "music": names = musicNames; break;
            default: names = indoorNames; break;
        }
        
        return names[index - 1] + " - " + location;
    }
    
    private String generateActivityTypeName(String activityType) {
        switch (activityType.toLowerCase()) {
            case "indoor": return "Indoor Activity Center";
            case "outdoor": return "Outdoor Recreation";
            case "educational": return "Educational Center";
            case "creative": return "Creative Arts";
            case "sports": return "Sports & Fitness";
            case "music": return "Music Education";
            default: return "Activity Center";
        }
    }
    
    private String generateActivityDescription(String name, String activityType) {
        return "A wonderful " + activityType + " center offering age-appropriate activities and programs designed to engage and educate children in a safe, nurturing environment.";
    }
    
    private String generatePrice(String location) {
        // Generate realistic prices based on location
        if (location.toLowerCase().contains("singapore")) {
            return "S$25-45";
        } else if (location.toLowerCase().contains("tokyo")) {
            return "¥2,000-4,000";
        } else if (location.toLowerCase().contains("london")) {
            return "£15-30";
        } else if (location.toLowerCase().contains("new york")) {
            return "$25-50";
        } else {
            return "$20-40";
        }
    }
    
    private String generateDuration(String activityType) {
        switch (activityType.toLowerCase()) {
            case "indoor": return "2-3 hours";
            case "outdoor": return "3-4 hours";
            case "educational": return "1-2 hours";
            case "creative": return "1-1.5 hours";
            case "sports": return "1-2 hours";
            case "music": return "45-60 minutes";
            default: return "2-3 hours";
        }
    }
    
    private String generateRating() {
        double rating = 4.0 + Math.random() * 0.8; // Rating between 4.0 and 4.8
        return String.format("%.1f/5", rating);
    }
    
    private String generateEducationalValue(String activityType) {
        switch (activityType.toLowerCase()) {
            case "educational": return "Excellent - comprehensive learning programs";
            case "creative": return "High - creativity and artistic expression";
            case "sports": return "Good - physical development and teamwork";
            case "music": return "High - musical skills and rhythm";
            case "indoor": return "Good - cognitive and social development";
            case "outdoor": return "High - nature exploration and physical activity";
            default: return "Good - well-rounded development";
        }
    }
    
    private List<String> generateRecommendations(String location, String activityType, String childAge) {
        List<String> recommendations = new ArrayList<>();
        
        recommendations.add("Perfect for " + childAge + "-year-olds with age-appropriate activities");
        recommendations.add("Located in " + location + " with easy access and parking");
        
        switch (activityType.toLowerCase()) {
            case "indoor":
                recommendations.add("Great for weather-independent fun and learning");
                break;
            case "outdoor":
                recommendations.add("Excellent for fresh air and physical development");
                break;
            case "educational":
                recommendations.add("Ideal for curious minds and learning exploration");
                break;
            case "creative":
                recommendations.add("Perfect for artistic expression and imagination");
                break;
            case "sports":
                recommendations.add("Great for physical fitness and motor skills");
                break;
            case "music":
                recommendations.add("Wonderful for rhythm, coordination, and musical appreciation");
                break;
        }
        
        return recommendations;
    }
    
    /**
     * Generate real, relevant URLs for activities based on location and type
     */
    private String generateRealUrl(String location, String activityType, int index) {
        String city = location.toLowerCase();
        String type = activityType.toLowerCase();
        
        // Generate URLs based on location and activity type
        if (city.contains("singapore")) {
            switch (type) {
                case "indoor":
                    return index == 1 ? "https://www.sentosa.com.sg/en/attractions/kidzania/" : 
                           index == 2 ? "https://www.marinabaysands.com/museum.html" :
                           "https://www.singaporeartmuseum.sg/";
                case "outdoor":
                    return index == 1 ? "https://www.gardensbythebay.com.sg/" :
                           index == 2 ? "https://www.zoo.com.sg/" :
                           "https://www.botanicgardens.gov.sg/";
                case "educational":
                    return index == 1 ? "https://www.science.edu.sg/" :
                           index == 2 ? "https://www.nhb.gov.sg/" :
                           "https://www.moe.gov.sg/";
                case "creative":
                    return index == 1 ? "https://www.singaporeartmuseum.sg/" :
                           index == 2 ? "https://www.esplanade.com/" :
                           "https://www.nac.gov.sg/";
                case "sports":
                    return index == 1 ? "https://www.activeg.com.sg/" :
                           index == 2 ? "https://www.sportssingapore.gov.sg/" :
                           "https://www.swimming.org.sg/";
                case "music":
                    return index == 1 ? "https://www.yamaha.com.sg/" :
                           index == 2 ? "https://www.berklee.edu/" :
                           "https://www.singaporeconservatory.edu.sg/";
                default:
                    return "https://www.visitsingapore.com/";
            }
        } else if (city.contains("tokyo")) {
            switch (type) {
                case "indoor":
                    return index == 1 ? "https://tokyo.kidzania.com/en" :
                           index == 2 ? "https://www.miraikan.jst.go.jp/en/" :
                           "https://www.tokyo-skytree.jp/en/";
                case "outdoor":
                    return index == 1 ? "https://www.tokyo-skytree.jp/en/" :
                           index == 2 ? "https://www.ueno-zoo.jp/en/" :
                           "https://www.tokyo-park.or.jp/english/";
                case "educational":
                    return index == 1 ? "https://www.miraikan.jst.go.jp/en/" :
                           index == 2 ? "https://www.tnm.jp/" :
                           "https://www.nationalmuseum.jp/";
                case "creative":
                    return index == 1 ? "https://www.nationalmuseum.jp/" :
                           index == 2 ? "https://www.tnm.jp/" :
                           "https://www.ntj.jac.go.jp/";
                case "sports":
                    return index == 1 ? "https://www.japan-sports.or.jp/" :
                           index == 2 ? "https://www.joc.or.jp/" :
                           "https://www.swimming.or.jp/";
                case "music":
                    return index == 1 ? "https://www.yamaha.com/" :
                           index == 2 ? "https://www.nhk.or.jp/" :
                           "https://www.ntj.jac.go.jp/";
                default:
                    return "https://www.gotokyo.org/en/";
            }
        } else if (city.contains("london")) {
            switch (type) {
                case "indoor":
                    return index == 1 ? "https://www.londonzoo.org/" :
                           index == 2 ? "https://www.sciencemuseum.org.uk/" :
                           "https://www.nhm.ac.uk/";
                case "outdoor":
                    return index == 1 ? "https://www.royalparks.org.uk/parks/hyde-park" :
                           index == 2 ? "https://www.kew.org/" :
                           "https://www.zsl.org/london-zoo";
                case "educational":
                    return index == 1 ? "https://www.sciencemuseum.org.uk/" :
                           index == 2 ? "https://www.nhm.ac.uk/" :
                           "https://www.britishmuseum.org/";
                case "creative":
                    return index == 1 ? "https://www.tate.org.uk/" :
                           index == 2 ? "https://www.nationalgallery.org.uk/" :
                           "https://www.vam.ac.uk/";
                case "sports":
                    return index == 1 ? "https://www.britishswimming.org/" :
                           index == 2 ? "https://www.british-gymnastics.org/" :
                           "https://www.lta.org.uk/";
                case "music":
                    return index == 1 ? "https://www.rncm.ac.uk/" :
                           index == 2 ? "https://www.rcm.ac.uk/" :
                           "https://www.guildhall.ac.uk/";
                default:
                    return "https://www.visitlondon.com/";
            }
        } else if (city.contains("new york")) {
            switch (type) {
                case "indoor":
                    return index == 1 ? "https://www.amnh.org/" :
                           index == 2 ? "https://www.moma.org/" :
                           "https://www.nypl.org/";
                case "outdoor":
                    return index == 1 ? "https://www.centralparknyc.org/" :
                           index == 2 ? "https://www.nycgovparks.org/" :
                           "https://www.bronxzoo.com/";
                case "educational":
                    return index == 1 ? "https://www.amnh.org/" :
                           index == 2 ? "https://www.intrepidmuseum.org/" :
                           "https://www.nypl.org/";
                case "creative":
                    return index == 1 ? "https://www.moma.org/" :
                           index == 2 ? "https://www.guggenheim.org/" :
                           "https://www.metmuseum.org/";
                case "sports":
                    return index == 1 ? "https://www.nycmarathon.org/" :
                           index == 2 ? "https://www.nycfc.com/" :
                           "https://www.nets.com/";
                case "music":
                    return index == 1 ? "https://www.juilliard.edu/" :
                           index == 2 ? "https://www.carnegiehall.org/" :
                           "https://www.lincolncenter.org/";
                default:
                    return "https://www.nycgo.com/";
            }
        } else if (city.contains("paris")) {
            switch (type) {
                case "indoor":
                    return index == 1 ? "https://www.louvre.fr/en" :
                           index == 2 ? "https://www.musee-orsay.fr/" :
                           "https://www.centrepompidou.fr/";
                case "outdoor":
                    return index == 1 ? "https://www.paris.fr/pages/paris-parks-and-gardens-1850" :
                           index == 2 ? "https://www.parc-zoologique-paris.fr/" :
                           "https://www.jardinduacclimatation.fr/";
                case "educational":
                    return index == 1 ? "https://www.cite-sciences.fr/en/" :
                           index == 2 ? "https://www.museedesenfants.asso.fr/" :
                           "https://www.palais-decouverte.fr/";
                case "creative":
                    return index == 1 ? "https://www.louvre.fr/en" :
                           index == 2 ? "https://www.musee-orsay.fr/" :
                           "https://www.centrepompidou.z/";
                case "sports":
                    return index == 1 ? "https://www.paris2024.org/" :
                           index == 2 ? "https://www.ffnatation.fr/" :
                           "https://www.ffgym.fr/";
                case "music":
                    return index == 1 ? "https://www.conservatoiredeparis.fr/" :
                           index == 2 ? "https://www.opera-de-paris.fr/" :
                           "https://www.philharmoniedeparis.fr/";
                default:
                    return "https://www.parisinfo.com/";
            }
        } else if (city.contains("sydney")) {
            switch (type) {
                case "indoor":
                    return index == 1 ? "https://www.sealife.com.au/sydney/" :
                           index == 2 ? "https://www.powerhousemuseum.com/" :
                           "https://www.artgallery.nsw.gov.au/";
                case "outdoor":
                    return index == 1 ? "https://www.tarzoo.com.au/" :
                           index == 2 ? "https://www.botanicgardens.gov.au/" :
                           "https://www.sydneyharbournationalpark.com.au/";
                case "educational":
                    return index == 1 ? "https://www.powerhousemuseum.com/" :
                           index == 2 ? "https://www.australianmuseum.net.au/" :
                           "https://www.sydney.edu.au/";
                case "creative":
                    return index == 1 ? "https://www.artgallery.nsw.gov.au/" :
                           index == 2 ? "https://www.mca.com.au/" :
                           "https://www.sydneyoperahouse.com/";
                case "sports":
                    return index == 1 ? "https://www.swimming.org.au/" :
                           index == 2 ? "https://www.gymnastics.org.au/" :
                           "https://www.sydneyfc.com/";
                case "music":
                    return index == 1 ? "https://www.sydneyconservatorium.edu.au/" :
                           index == 2 ? "https://www.sydneyoperahouse.com/" :
                           "https://www.acma.gov.au/";
                default:
                    return "https://www.sydney.com/";
            }
        } else if (city.contains("berlin")) {
            switch (type) {
                case "indoor":
                    return index == 1 ? "https://www.museumsinsel-berlin.de/" :
                           index == 2 ? "https://www.deutsches-museum.de/" :
                           "https://www.jewishmuseum.de/";
                case "outdoor":
                    return index == 1 ? "https://www.tierpark-berlin.de/" :
                           index == 2 ? "https://www.botanischer-garten-berlin.de/" :
                           "https://www.gruen-berlin.de/";
                case "educational":
                    return index == 1 ? "https://www.deutsches-museum.de/" :
                           index == 2 ? "https://www.museumsinsel-berlin.de/" :
                           "https://www.humboldt-university.de/";
                case "creative":
                    return index == 1 ? "https://www.hamburger-bahnhof.de/" :
                           index == 2 ? "https://www.gropiusbau.de/" :
                           "https://www.berliner-philharmoniker.de/";
                case "sports":
                    return index == 1 ? "https://www.dsv.org/" :
                           index == 2 ? "https://www.berlin.de/sport/" :
                           "https://www.herthabsc.com/";
                case "music":
                    return index == 1 ? "https://www.berliner-philharmoniker.de/" :
                           index == 2 ? "https://www.deutscheoperberlin.de/" :
                           "https://www.udk-berlin.de/";
                default:
                    return "https://www.visitberlin.de/";
            }
        } else {
            // Generic fallback for other cities
            switch (type) {
                case "indoor":
                    return "https://www.tripadvisor.com/Attractions-g" + location.hashCode() + "-Activities.html";
                case "outdoor":
                    return "https://www.parks.gov/";
                case "educational":
                    return "https://www.education.gov/";
                case "creative":
                    return "https://www.arts.gov/";
                case "sports":
                    return "https://www.sports.gov/";
                case "music":
                    return "https://www.music.gov/";
                default:
                    return "https://www.visit" + location.replaceAll("\\s+", "") + ".com/";
            }
        }
    }
    
    /**
     * Create hardcoded Singapore activities for plan_001
     */
    private List<Map<String, Object>> createSingaporeActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        Map<String, Object> activity1 = new HashMap<>();
        activity1.put("name", "KidZania Singapore");
        activity1.put("type", "Indoor Playground");
        activity1.put("ageRange", "3-14 years");
        activity1.put("description", "Educational role-playing theme park where children can explore different careers");
        activity1.put("location", "Sentosa Island, Singapore");
        activity1.put("price", "S$58-68");
        activity1.put("duration", "4-6 hours");
        activity1.put("rating", "4.5/5");
        activity1.put("safety", "Highly supervised, child-safe environment");
        activity1.put("educationalValue", "High - career exploration and social skills");
        activity1.put("url", "https://singapore.kidzania.com");
        activities.add(activity1);
        
        Map<String, Object> activity2 = new HashMap<>();
        activity2.put("name", "ArtScience Museum");
        activity2.put("type", "Children's Museum");
        activity2.put("ageRange", "All ages");
        activity2.put("description", "Interactive exhibits combining art, science, and technology");
        activity2.put("location", "Marina Bay Sands, Singapore");
        activity2.put("price", "S$21-32");
        activity2.put("duration", "2-3 hours");
        activity2.put("rating", "4.3/5");
        activity2.put("safety", "Child-friendly with staff supervision");
        activity2.put("educationalValue", "High - STEAM learning");
        activity2.put("url", "https://www.marinabaysands.com/museum");
        activities.add(activity2);
        
        Map<String, Object> activity3 = new HashMap<>();
        activity3.put("name", "The Artground");
        activity3.put("type", "Art Classes");
        activity3.put("ageRange", "2-12 years");
        activity3.put("description", "Creative arts and crafts workshops for children");
        activity3.put("location", "Goodman Arts Centre, Singapore");
        activity3.put("price", "S$25-35 per session");
        activity3.put("duration", "1-2 hours");
        activity3.put("rating", "4.6/5");
        activity3.put("safety", "Small class sizes, parent-friendly");
        activity3.put("educationalValue", "High - creativity and fine motor skills");
        activity3.put("url", "https://www.theartground.com.sg");
        activities.add(activity3);
        
        return activities;
    }
    
    /**
     * Create hardcoded Tokyo activities for plan_002
     */
    private List<Map<String, Object>> createTokyoActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        Map<String, Object> activity1 = new HashMap<>();
        activity1.put("name", "National Museum of Emerging Science and Innovation (Miraikan)");
        activity1.put("type", "Science Museum");
        activity1.put("ageRange", "4-12 years");
        activity1.put("description", "Interactive science exhibits and demonstrations perfect for curious minds");
        activity1.put("location", "Odaiba, Tokyo");
        activity1.put("price", "¥630");
        activity1.put("duration", "3-4 hours");
        activity1.put("rating", "4.4/5");
        activity1.put("safety", "Family-friendly with English support");
        activity1.put("educationalValue", "Excellent - hands-on science learning");
        activity1.put("url", "https://www.miraikan.jst.go.jp/en/");
        activities.add(activity1);
        
        Map<String, Object> activity2 = new HashMap<>();
        activity2.put("name", "Tokyo Metropolitan Children's Hall");
        activity2.put("type", "Children's Center");
        activity2.put("ageRange", "3-12 years");
        activity2.put("description", "Multi-purpose facility with play areas, workshops, and educational programs");
        activity2.put("location", "Shibuya, Tokyo");
        activity2.put("price", "Free admission");
        activity2.put("duration", "2-3 hours");
        activity2.put("rating", "4.2/5");
        activity2.put("safety", "Supervised activities and safe play areas");
        activity2.put("educationalValue", "High - diverse learning experiences");
        activity2.put("url", "https://www.children.metro.tokyo.lg.jp/");
        activities.add(activity2);
        
        Map<String, Object> activity3 = new HashMap<>();
        activity3.put("name", "KidZania Tokyo");
        activity3.put("type", "Educational Theme Park");
        activity3.put("ageRange", "3-15 years");
        activity3.put("description", "Role-playing activities where children can experience different professions");
        activity3.put("location", "Lalaport Toyosu, Tokyo");
        activity3.put("price", "¥3,500-4,000");
        activity3.put("duration", "4-6 hours");
        activity3.put("rating", "4.6/5");
        activity3.put("safety", "Highly supervised, child-safe environment");
        activity3.put("educationalValue", "Excellent - career exploration and life skills");
        activity3.put("url", "https://tokyo.kidzania.com/en");
        activities.add(activity3);
        
        return activities;
    }
    
    /**
     * Create hardcoded London activities for plan_003
     */
    private List<Map<String, Object>> createLondonActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        Map<String, Object> activity1 = new HashMap<>();
        activity1.put("name", "Hyde Park Playground");
        activity1.put("type", "Outdoor Playground");
        activity1.put("ageRange", "1-12 years");
        activity1.put("description", "Large playground with age-appropriate equipment and open green spaces");
        activity1.put("location", "Hyde Park, London");
        activity1.put("price", "Free admission");
        activity1.put("duration", "1-2 hours");
        activity1.put("rating", "4.3/5");
        activity1.put("safety", "Well-maintained equipment with safety surfaces");
        activity1.put("educationalValue", "Good - physical development and social interaction");
        activity1.put("url", "https://www.royalparks.org.uk/parks/hyde-park");
        activities.add(activity1);
        
        Map<String, Object> activity2 = new HashMap<>();
        activity2.put("name", "Kensington Gardens Playground");
        activity2.put("type", "Adventure Playground");
        activity2.put("ageRange", "2-12 years");
        activity2.put("description", "Natural adventure playground with climbing structures and water play");
        activity2.put("location", "Kensington Gardens, London");
        activity2.put("price", "Free admission");
        activity2.put("duration", "1-3 hours");
        activity2.put("rating", "4.5/5");
        activity2.put("safety", "Supervised areas with natural play elements");
        activity2.put("educationalValue", "High - nature exploration and creativity");
        activity2.put("url", "https://www.royalparks.org.uk/parks/kensington-gardens");
        activities.add(activity2);
        
        Map<String, Object> activity3 = new HashMap<>();
        activity3.put("name", "London Zoo");
        activity3.put("type", "Outdoor Zoo");
        activity3.put("ageRange", "All ages");
        activity3.put("description", "World-famous zoo with interactive exhibits and outdoor animal encounters");
        activity3.put("location", "Regent's Park, London");
        activity3.put("price", "£25-35");
        activity3.put("duration", "3-4 hours");
        activity3.put("rating", "4.2/5");
        activity3.put("safety", "Family-friendly with stroller access");
        activity3.put("educationalValue", "Excellent - wildlife education and conservation awareness");
        activity3.put("url", "https://www.londonzoo.org/");
        activities.add(activity3);
        
        return activities;
    }
}
