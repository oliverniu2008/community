package com.superm.community.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class AIAssistantService {
    
    // OpenAI API configuration
    // In production, these should be in application.properties or environment variables
    private static final String OPENAI_API_KEY = "your-api-key-here"; // Replace with actual key
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    /**
     * Get AI response for a parenting question
     * This is a SIMULATED response for demo purposes
     * In production, you would call the actual OpenAI API
     */
    public String getAIResponse(String userMessage, String userName, List<Map<String, String>> history) {
        // SIMULATION: Generate contextual response based on keywords
        String response = generateSimulatedResponse(userMessage, userName);
        
        /* 
         * PRODUCTION CODE (uncomment when you have an API key):
         * 
         * try {
         *     String systemPrompt = buildSystemPrompt(userName);
         *     List<Map<String, String>> messages = new ArrayList<>();
         *     
         *     // Add system message
         *     messages.add(Map.of("role", "system", "content", systemPrompt));
         *     
         *     // Add conversation history
         *     if (history != null) {
         *         messages.addAll(history);
         *     }
         *     
         *     // Add current user message
         *     messages.add(Map.of("role", "user", "content", userMessage));
         *     
         *     // Call OpenAI API
         *     String requestBody = new ObjectMapper().writeValueAsString(Map.of(
         *         "model", "gpt-4",
         *         "messages", messages,
         *         "temperature", 0.7,
         *         "max_tokens", 800
         *     ));
         *     
         *     HttpClient client = HttpClient.newHttpClient();
         *     HttpRequest request = HttpRequest.newBuilder()
         *         .uri(URI.create(OPENAI_API_URL))
         *         .header("Content-Type", "application/json")
         *         .header("Authorization", "Bearer " + OPENAI_API_KEY)
         *         .POST(HttpRequest.BodyPublishers.ofString(requestBody))
         *         .build();
         *     
         *     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         *     
         *     // Parse response
         *     JsonNode jsonResponse = new ObjectMapper().readTree(response.body());
         *     return jsonResponse.get("choices").get(0).get("message").get("content").asText();
         *     
         * } catch (Exception e) {
         *     return "I'm having trouble connecting right now. Please try again in a moment.";
         * }
         */
        
        return response;
    }
    
    /**
     * Build system prompt for AI with parenting context
     */
    private String buildSystemPrompt(String userName) {
        return String.format(
            "You are an expert AI Parenting Assistant helping %s. " +
            "Provide evidence-based, compassionate advice on parenting topics. " +
            "Your expertise includes: child development, sleep training, nutrition, behavior management, " +
            "health and safety, educational activities, and emotional support for parents. " +
            "\n\nGuidelines:\n" +
            "- Be warm, empathetic, and supportive\n" +
            "- Provide practical, actionable advice\n" +
            "- Cite age-appropriate recommendations when relevant\n" +
            "- Acknowledge when professional medical advice is needed\n" +
            "- Keep responses concise but comprehensive (200-400 words)\n" +
            "- Use bullet points or numbered lists for clarity\n" +
            "- Never claim to replace professional medical or psychological advice\n" +
            "- Respect diverse parenting styles and cultural approaches",
            userName
        );
    }
    
    /**
     * Generate simulated AI response based on keywords (for demo)
     */
    private String generateSimulatedResponse(String userMessage, String userName) {
        String message = userMessage.toLowerCase();
        
        // Sleep-related
        if (message.contains("sleep") || message.contains("bedtime") || message.contains("nap")) {
            return String.format(
                "Hi %s! Sleep challenges are very common. Here are some evidence-based strategies:\n\n" +
                "**Establish a Consistent Routine:**\n" +
                "• Same bedtime every night (±30 minutes)\n" +
                "• Calming activities: bath, books, lullabies\n" +
                "• Dim lights 1-2 hours before bed\n\n" +
                "**Create a Sleep-Friendly Environment:**\n" +
                "• Dark, cool room (65-70°F)\n" +
                "• White noise machine (optional)\n" +
                "• Safe sleep space (AAP guidelines)\n\n" +
                "**Age-Appropriate Expectations:**\n" +
                "• Newborns: Wake every 2-3 hours (normal!)\n" +
                "• 6-12 months: May sleep 6-8 hour stretches\n" +
                "• Toddlers: Need 11-14 hours total\n\n" +
                "Remember: Every child is different. If concerns persist, consult your pediatrician. You're doing great! 💙",
                userName
            );
        }
        
        // Nutrition-related
        if (message.contains("food") || message.contains("eat") || message.contains("nutrition") || message.contains("meal")) {
            return String.format(
                "Great question, %s! Nutrition is so important. Here's helpful guidance:\n\n" +
                "**Healthy Eating Principles:**\n" +
                "• Offer variety: fruits, veggies, proteins, whole grains\n" +
                "• You decide WHAT and WHEN, child decides HOW MUCH\n" +
                "• Make mealtimes pressure-free and positive\n\n" +
                "**Age-Appropriate Foods:**\n" +
                "• 6-12 months: Soft, mashable foods (avoid honey, choking hazards)\n" +
                "• 1-3 years: Small portions, finger foods, family meals\n" +
                "• Preschool: Encourage trying new foods, model healthy eating\n\n" +
                "**Common Challenges:**\n" +
                "• Picky eating is NORMAL and usually temporary\n" +
                "• Offer new foods 10-15 times before giving up\n" +
                "• Don't force or bribe - creates negative associations\n\n" +
                "For specific dietary concerns or allergies, consult a pediatrician or dietitian. 🥗",
                userName
            );
        }
        
        // Behavior-related
        if (message.contains("tantrum") || message.contains("behavior") || message.contains("discipline") || message.contains("misbehav")) {
            return String.format(
                "%s, behavior challenges are part of normal development! Here's how to navigate them:\n\n" +
                "**Understanding Tantrums:**\n" +
                "• Ages 1-4: Peak tantrum years (limited communication skills)\n" +
                "• Triggers: Tired, hungry, overwhelmed, frustrated\n" +
                "• They're learning emotional regulation - it takes time!\n\n" +
                "**Positive Discipline Strategies:**\n" +
                "1. **Prevent**: Maintain routines, offer choices, avoid triggers\n" +
                "2. **Stay Calm**: Your regulation helps them regulate\n" +
                "3. **Connect First**: Get down to their level, empathize\n" +
                "4. **Set Limits**: Clear, consistent boundaries with love\n" +
                "5. **Natural Consequences**: Let them learn safely\n\n" +
                "**During a Tantrum:**\n" +
                "• Ensure safety first\n" +
                "• Stay nearby but don't engage/negotiate\n" +
                "• Offer comfort when they're ready\n" +
                "• Discuss what happened later (when calm)\n\n" +
                "You're teaching life skills! It gets easier. 💪",
                userName
            );
        }
        
        // Development/milestones
        if (message.contains("milestone") || message.contains("development") || message.contains("crawl") || 
            message.contains("walk") || message.contains("talk") || message.contains("ready")) {
            return String.format(
                "Hi %s! Developmental milestones are exciting to track. Here's what to know:\n\n" +
                "**Key Developmental Areas:**\n" +
                "• **Motor Skills**: Rolling, sitting, crawling, walking\n" +
                "• **Language**: Cooing, babbling, first words, sentences\n" +
                "• **Social-Emotional**: Smiling, attachment, empathy\n" +
                "• **Cognitive**: Problem-solving, object permanence, play\n\n" +
                "**General Timeline (every child is unique!):**\n" +
                "• 6 months: Sits with support, babbles\n" +
                "• 12 months: May take first steps, says 1-2 words\n" +
                "• 18 months: Walks independently, 10-20 words\n" +
                "• 2 years: Runs, 2-word phrases, imaginative play\n" +
                "• 3 years: Pedals tricycle, speaks in sentences\n\n" +
                "**Remember:**\n" +
                "• Wide range of \"normal\" exists\n" +
                "• Boys/girls may develop at different paces\n" +
                "• Trust your instincts - discuss concerns with pediatrician\n\n" +
                "Every child blooms in their own time! 🌱",
                userName
            );
        }
        
        // Potty training
        if (message.contains("potty") || message.contains("toilet") || message.contains("diaper")) {
            return String.format(
                "Potty training can be challenging, %s! Here's a supportive approach:\n\n" +
                "**Signs of Readiness (typically 18-30 months):**\n" +
                "• Stays dry for 2+ hours\n" +
                "• Shows interest in bathroom habits\n" +
                "• Can follow simple instructions\n" +
                "• Expresses discomfort with dirty diapers\n" +
                "• Can pull pants up/down\n\n" +
                "**Gentle Potty Training Steps:**\n" +
                "1. **Prepare**: Read books, let them pick underwear\n" +
                "2. **Schedule**: Regular bathroom visits (every 2 hours)\n" +
                "3. **Celebrate**: Praise efforts, not just success\n" +
                "4. **Stay Positive**: Accidents are normal learning\n" +
                "5. **Be Patient**: Average training time is 3-6 months\n\n" +
                "**Pro Tips:**\n" +
                "• Start when life is stable (avoid major changes)\n" +
                "• Nighttime dryness comes later (totally normal)\n" +
                "• Never punish accidents - they're learning!\n\n" +
                "You've got this! 🎉",
                userName
            );
        }
        
        // Mental health/self-care
        if (message.contains("mental health") || message.contains("postpartum") || message.contains("depression") ||
            message.contains("anxiety") || message.contains("overwhelm") || message.contains("stress")) {
            return String.format(
                "%s, thank you for prioritizing your mental health. That's so important! ❤️\n\n" +
                "**Parenting is Hard - You're Not Alone:**\n" +
                "• 1 in 7 mothers experience postpartum depression\n" +
                "• Parental burnout is real and valid\n" +
                "• Seeking help is a sign of STRENGTH\n\n" +
                "**Self-Care Isn't Selfish:**\n" +
                "• Sleep when possible (ask for help!)\n" +
                "• Eat nutritious meals regularly\n" +
                "• Move your body (even 10-minute walks)\n" +
                "• Connect with supportive friends/family\n" +
                "• Set boundaries - it's okay to say no\n\n" +
                "**When to Seek Professional Help:**\n" +
                "• Persistent sadness or hopelessness\n" +
                "• Difficulty bonding with baby\n" +
                "• Intrusive thoughts\n" +
                "• Can't function in daily activities\n\n" +
                "**Resources:**\n" +
                "• Postpartum Support International: 1-800-944-4773\n" +
                "• National Maternal Mental Health Hotline: 1-833-943-5746\n" +
                "• Talk to your doctor - treatment works!\n\n" +
                "Your well-being matters. A healthy parent = healthy child. 💙",
                userName
            );
        }
        
        // Default response
        return String.format(
            "Thanks for your question, %s! I'm here to help with parenting challenges.\n\n" +
            "I can provide guidance on:\n" +
            "• **Sleep Training**: Bedtime routines, night wakings, naps\n" +
            "• **Nutrition**: Feeding, picky eating, meal planning\n" +
            "• **Behavior**: Tantrums, discipline, positive parenting\n" +
            "• **Development**: Milestones, activities, learning\n" +
            "• **Health & Safety**: Common concerns, childproofing\n" +
            "• **Self-Care**: Parental mental health, stress management\n\n" +
            "Could you tell me more about what you're experiencing? The more details you share, " +
            "the better I can help! For example:\n" +
            "• Child's age\n" +
            "• Specific situation or challenge\n" +
            "• What you've already tried\n\n" +
            "I'm here to support you! 🌟",
            userName
        );
    }
}

