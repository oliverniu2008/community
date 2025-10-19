package com.superm.community.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

@Service
public class OpenAIService {
    
    @Value("${openai.api.key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OpenAIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate content using ChatGPT API
     */
    public Map<String, Object> generateContent(String contentType, String topic, String communityId, String aiTool) {
        Map<String, Object> content = new HashMap<>();
        content.put("id", "content_" + System.currentTimeMillis());
        content.put("type", contentType);
        content.put("topic", topic);
        content.put("communityId", communityId);
        content.put("aiTool", aiTool);
        content.put("generatedAt", java.time.LocalDateTime.now().toString());
        
        try {
            // Check if API key is available
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                content.put("status", "Error");
                content.put("error", "OpenAI API key not configured");
                return content;
            }
            
            // Generate content based on type
            String generatedContent = generateContentWithChatGPT(contentType, topic);
            
            if (generatedContent != null && !generatedContent.trim().isEmpty()) {
                content.put("status", "Generated");
                
                // Parse and structure the generated content
                parseGeneratedContent(content, contentType, generatedContent);
                
            } else {
                content.put("status", "Error");
                content.put("error", "Failed to generate content");
            }
            
        } catch (Exception e) {
            content.put("status", "Error");
            content.put("error", "API call failed: " + e.getMessage());
        }
        
        return content;
    }
    
    /**
     * Generate content using ChatGPT API
     */
    private String generateContentWithChatGPT(String contentType, String topic) throws Exception {
        // Build system prompt based on content type
        String systemPrompt = buildSystemPrompt(contentType);
        
        // Build user prompt
        String userPrompt = buildUserPrompt(contentType, topic);
        
        // Prepare API request
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", getMaxTokensForContentType(contentType));
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Make API call
        ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, request, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.get("choices").get(0).get("message").get("content").asText();
        } else {
            throw new RestClientException("API call failed with status: " + response.getStatusCode());
        }
    }
    
    /**
     * Build system prompt based on content type
     */
    private String buildSystemPrompt(String contentType) {
        switch (contentType.toLowerCase()) {
            case "article":
                return "You are an expert content writer specializing in parenting and family topics. " +
                       "Write engaging, informative articles that are helpful for parents. " +
                       "Use a warm, supportive tone and include practical advice. " +
                       "Structure your content with clear headings and actionable tips.";
                       
            case "video":
                return "You are an expert video script writer for parenting content. " +
                       "Create engaging video scripts that are educational and entertaining for parents. " +
                       "Include clear scene descriptions, dialogue, and visual cues. " +
                       "Keep the content concise and engaging for short-form video content.";
                       
            case "photo":
                return "You are an expert social media content creator specializing in parenting topics. " +
                       "Write compelling captions for photos that engage parents and families. " +
                       "Include relevant hashtags and call-to-actions. " +
                       "Keep captions concise but engaging, perfect for social media posts.";
                       
            default:
                return "You are an expert content creator specializing in parenting and family topics. " +
                       "Create engaging, helpful content that resonates with parents and families.";
        }
    }
    
    /**
     * Build user prompt based on content type and topic
     */
    private String buildUserPrompt(String contentType, String topic) {
        switch (contentType.toLowerCase()) {
            case "article":
                return "Write a comprehensive article about: " + topic + "\n\n" +
                       "Please include:\n" +
                       "- An engaging introduction\n" +
                       "- Clear sections with headings\n" +
                       "- Practical tips and advice\n" +
                       "- A helpful conclusion\n" +
                       "- Make it approximately 800-1200 words\n" +
                       "- Use a warm, supportive tone for parents";
                       
            case "video":
                return "Create a video script about: " + topic + "\n\n" +
                       "Please include:\n" +
                       "- A hook in the first 3 seconds\n" +
                       "- Clear scene descriptions\n" +
                       "- Engaging dialogue\n" +
                       "- Visual cues and actions\n" +
                       "- A strong call-to-action at the end\n" +
                       "- Make it suitable for a 60-90 second video";
                       
            case "photo":
                return "Write a social media caption for a photo about: " + topic + "\n\n" +
                       "Please include:\n" +
                       "- An engaging opening line\n" +
                       "- 2-3 sentences of helpful content\n" +
                       "- Relevant hashtags (5-8 hashtags)\n" +
                       "- A call-to-action\n" +
                       "- Keep it under 200 characters for the main text";
                       
            default:
                return "Create content about: " + topic + "\n\n" +
                       "Make it engaging, helpful, and relevant for parents and families.";
        }
    }
    
    /**
     * Get max tokens based on content type
     */
    private int getMaxTokensForContentType(String contentType) {
        switch (contentType.toLowerCase()) {
            case "article":
                return 2000; // Longer articles need more tokens
            case "video":
                return 1000; // Video scripts are medium length
            case "photo":
                return 300;  // Photo captions are short
            default:
                return 1000;
        }
    }
    
    /**
     * Parse generated content and structure it based on content type
     */
    private void parseGeneratedContent(Map<String, Object> content, String contentType, String generatedText) {
        switch (contentType.toLowerCase()) {
            case "article":
                parseArticleContent(content, generatedText);
                break;
            case "video":
                parseVideoContent(content, generatedText);
                break;
            case "photo":
                parsePhotoContent(content, generatedText);
                break;
            default:
                content.put("content", generatedText);
        }
    }
    
    /**
     * Parse article content
     */
    private void parseArticleContent(Map<String, Object> content, String generatedText) {
        content.put("content", generatedText);
        
        // Extract title (first line or first heading)
        String[] lines = generatedText.split("\n");
        String title = lines[0].replaceAll("^#+\\s*", "").trim();
        if (title.length() > 100) {
            title = title.substring(0, 100) + "...";
        }
        content.put("title", title);
        
        // Calculate word count
        int wordCount = generatedText.split("\\s+").length;
        content.put("wordCount", wordCount);
        
        // Estimate reading time (average 200 words per minute)
        int readingTime = Math.max(1, wordCount / 200);
        content.put("readingTime", readingTime + " min read");
    }
    
    /**
     * Parse video content
     */
    private void parseVideoContent(Map<String, Object> content, String generatedText) {
        content.put("script", generatedText);
        
        // Extract title
        String[] lines = generatedText.split("\n");
        String title = lines[0].replaceAll("^#+\\s*", "").trim();
        if (title.length() > 80) {
            title = title.substring(0, 80) + "...";
        }
        content.put("title", title);
        
        // Estimate duration (rough calculation)
        int wordCount = generatedText.split("\\s+").length;
        int estimatedDuration = Math.max(30, wordCount / 3); // Rough estimate
        content.put("duration", estimatedDuration + " seconds");
        
        // Extract hashtags
        List<String> hashtags = extractHashtags(generatedText);
        content.put("hashtags", hashtags);
    }
    
    /**
     * Parse photo content
     */
    private void parsePhotoContent(Map<String, Object> content, String generatedText) {
        content.put("caption", generatedText);
        
        // Extract title (first line)
        String[] lines = generatedText.split("\n");
        String title = lines[0].trim();
        if (title.length() > 60) {
            title = title.substring(0, 60) + "...";
        }
        content.put("title", title);
        
        // Extract hashtags
        List<String> hashtags = extractHashtags(generatedText);
        content.put("hashtags", hashtags);
        
        // Generate alt text
        String altText = "Photo related to " + content.get("topic") + " for parents and families";
        content.put("altText", altText);
    }
    
    /**
     * Extract hashtags from text
     */
    private List<String> extractHashtags(String text) {
        List<String> hashtags = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            if (word.startsWith("#") && word.length() > 1) {
                hashtags.add(word);
            }
        }
        
        // If no hashtags found, add some default ones
        if (hashtags.isEmpty()) {
            hashtags.add("#parenting");
            hashtags.add("#family");
            hashtags.add("#kids");
        }
        
        return hashtags;
    }
    
    /**
     * Check if OpenAI API is configured and available
     */
    public boolean isApiConfigured() {
        return openaiApiKey != null && !openaiApiKey.trim().isEmpty();
    }
    
    /**
     * Get API status information
     */
    public Map<String, Object> getApiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", isApiConfigured());
        status.put("apiUrl", OPENAI_API_URL);
        status.put("model", "gpt-4");
        
        if (isApiConfigured()) {
            status.put("status", "Ready");
            status.put("message", "OpenAI API is configured and ready to use");
        } else {
            status.put("status", "Not Configured");
            status.put("message", "OpenAI API key is not configured. Please set OPENAI_API_KEY environment variable.");
        }
        
        return status;
    }
}
