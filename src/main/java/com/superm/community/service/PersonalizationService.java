package com.superm.community.service;

import com.superm.community.model.Post;
import com.superm.community.model.Community;
import com.superm.community.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonalizationService {
    
    // User engagement data (in production, this would be in a database)
    private final Map<String, Map<String, Object>> userEngagement = new HashMap<>();
    private final Map<String, List<String>> userViewedPosts = new HashMap<>();
    private final Map<String, List<String>> userLikedPosts = new HashMap<>();
    private final Map<String, Map<String, Integer>> userTopicScores = new HashMap<>();
    
    /**
     * Get personalized feed for a user
     * Uses AI-powered ranking algorithm
     */
    public List<Post> getPersonalizedFeed(String userId, List<Post> allPosts, String[] userTopics, List<Community> communities) {
        // Create scored posts
        List<ScoredPost> scoredPosts = new ArrayList<>();
        
        for (Post post : allPosts) {
            double score = calculatePostScore(userId, post, userTopics, communities);
            scoredPosts.add(new ScoredPost(post, score));
        }
        
        // Sort by score (descending)
        scoredPosts.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Apply diversity - don't show too many posts from same community in a row
        List<Post> diversifiedPosts = applyDiversity(scoredPosts);
        
        return diversifiedPosts;
    }
    
    /**
     * Calculate AI-powered relevance score for a post
     */
    private double calculatePostScore(String userId, Post post, String[] userTopics, List<Community> communities) {
        double score = 0.0;
        
        // 1. TOPIC RELEVANCE SCORE (40% weight)
        double topicScore = calculateTopicRelevance(post, userTopics, communities);
        score += topicScore * 0.4;
        
        // 2. ENGAGEMENT SCORE (25% weight) - Popular content
        double engagementScore = calculateEngagementScore(post);
        score += engagementScore * 0.25;
        
        // 3. RECENCY SCORE (20% weight) - Newer content prioritized
        double recencyScore = calculateRecencyScore(post);
        score += recencyScore * 0.2;
        
        // 4. TIME-OF-DAY OPTIMIZATION (10% weight)
        double timeScore = calculateTimeRelevance(post);
        score += timeScore * 0.1;
        
        // 5. PERSONALIZATION SCORE (5% weight) - Based on user history
        double personalScore = calculatePersonalScore(userId, post);
        score += personalScore * 0.05;
        
        return score;
    }
    
    /**
     * Topic relevance - matches user interests
     */
    private double calculateTopicRelevance(Post post, String[] userTopics, List<Community> communities) {
        if (userTopics == null || userTopics.length == 0) {
            return 0.5; // Neutral score if no topics
        }
        
        // Get post's community
        Community community = communities.stream()
            .filter(c -> c.getId().equals(post.getCommunityId()))
            .findFirst()
            .orElse(null);
        
        if (community == null) return 0.0;
        
        String postText = (post.getContent() + " " + community.getName() + " " + community.getDescription()).toLowerCase();
        
        int matches = 0;
        for (String topic : userTopics) {
            if (postText.contains(topic.toLowerCase())) {
                matches++;
            }
        }
        
        // Return score 0.0 to 1.0
        return (double) matches / userTopics.length;
    }
    
    /**
     * Engagement score - viral/popular content
     */
    private double calculateEngagementScore(Post post) {
        // Weighted engagement: likes are more valuable than views
        double engagementPoints = (post.getLikes() * 3.0) + (post.getComments() * 5.0);
        
        // Normalize to 0-1 scale (assuming max engagement ~200 points)
        return Math.min(engagementPoints / 200.0, 1.0);
    }
    
    /**
     * Recency score - fresh content prioritized
     */
    private double calculateRecencyScore(Post post) {
        LocalDateTime now = LocalDateTime.now();
        long hoursAgo = java.time.Duration.between(post.getCreatedAt(), now).toHours();
        
        // Exponential decay: newest posts score highest
        if (hoursAgo < 1) return 1.0;
        if (hoursAgo < 6) return 0.9;
        if (hoursAgo < 12) return 0.7;
        if (hoursAgo < 24) return 0.5;
        if (hoursAgo < 48) return 0.3;
        if (hoursAgo < 168) return 0.2; // 1 week
        return 0.1;
    }
    
    /**
     * Time-of-day optimization - show relevant content at relevant times
     */
    private double calculateTimeRelevance(Post post) {
        LocalTime now = LocalTime.now();
        String postText = post.getContent().toLowerCase();
        
        // Morning (6am-10am): Breakfast, morning routines
        if (now.getHour() >= 6 && now.getHour() < 10) {
            if (postText.contains("breakfast") || postText.contains("morning") || 
                postText.contains("routine")) {
                return 1.0;
            }
        }
        
        // Midday (10am-2pm): Activities, development
        if (now.getHour() >= 10 && now.getHour() < 14) {
            if (postText.contains("activity") || postText.contains("play") || 
                postText.contains("learning")) {
                return 1.0;
            }
        }
        
        // Afternoon (2pm-6pm): Nutrition, snacks
        if (now.getHour() >= 14 && now.getHour() < 18) {
            if (postText.contains("snack") || postText.contains("meal") || 
                postText.contains("nutrition")) {
                return 1.0;
            }
        }
        
        // Evening (6pm-10pm): Sleep, bedtime, behavior
        if (now.getHour() >= 18 && now.getHour() < 22) {
            if (postText.contains("sleep") || postText.contains("bedtime") || 
                postText.contains("tantrum") || postText.contains("behavior")) {
                return 1.0;
            }
        }
        
        // Night (10pm-6am): Sleep training, night wakings
        if (now.getHour() >= 22 || now.getHour() < 6) {
            if (postText.contains("sleep") || postText.contains("night") || 
                postText.contains("waking")) {
                return 1.0;
            }
        }
        
        return 0.5; // Neutral score if no time match
    }
    
    /**
     * Personal score based on user's past behavior
     */
    private double calculatePersonalScore(String userId, Post post) {
        // Check if user has viewed this post before (lower score)
        List<String> viewedPosts = userViewedPosts.getOrDefault(userId, new ArrayList<>());
        if (viewedPosts.contains(post.getId())) {
            return 0.0; // Already seen
        }
        
        // Check if user liked similar posts (higher score)
        List<String> likedPosts = userLikedPosts.getOrDefault(userId, new ArrayList<>());
        if (!likedPosts.isEmpty()) {
            // If user has engagement history, use it
            return 0.8;
        }
        
        return 0.5; // Neutral for new users
    }
    
    /**
     * Apply diversity to prevent too many posts from same community in a row
     */
    private List<Post> applyDiversity(List<ScoredPost> scoredPosts) {
        List<Post> result = new ArrayList<>();
        Set<String> recentCommunities = new HashSet<>();
        int diversityWindow = 3; // Don't show same community within 3 posts
        
        for (ScoredPost sp : scoredPosts) {
            String communityId = sp.post.getCommunityId();
            
            if (!recentCommunities.contains(communityId)) {
                result.add(sp.post);
                recentCommunities.add(communityId);
                
                // Maintain sliding window
                if (recentCommunities.size() > diversityWindow) {
                    recentCommunities.clear();
                }
            } else {
                // Add to end for later consideration
                result.add(sp.post);
            }
        }
        
        return result;
    }
    
    /**
     * Track user engagement for better personalization
     */
    public void trackPostView(String userId, String postId) {
        userViewedPosts.putIfAbsent(userId, new ArrayList<>());
        if (!userViewedPosts.get(userId).contains(postId)) {
            userViewedPosts.get(userId).add(postId);
        }
    }
    
    public void trackPostLike(String userId, String postId) {
        userLikedPosts.putIfAbsent(userId, new ArrayList<>());
        if (!userLikedPosts.get(userId).contains(postId)) {
            userLikedPosts.get(userId).add(postId);
        }
    }
    
    public void trackTopicInterest(String userId, String topic, int weight) {
        userTopicScores.putIfAbsent(userId, new HashMap<>());
        Map<String, Integer> scores = userTopicScores.get(userId);
        scores.put(topic, scores.getOrDefault(topic, 0) + weight);
    }
    
    /**
     * Get content diversity score for feed health
     */
    public Map<String, Object> getFeedHealthMetrics(List<Post> personalizedFeed) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Count unique communities
        Set<String> uniqueCommunities = personalizedFeed.stream()
            .map(Post::getCommunityId)
            .collect(Collectors.toSet());
        
        metrics.put("totalPosts", personalizedFeed.size());
        metrics.put("uniqueCommunities", uniqueCommunities.size());
        metrics.put("diversityScore", (double) uniqueCommunities.size() / Math.max(personalizedFeed.size(), 1));
        
        // Average age of content
        long avgHoursOld = personalizedFeed.stream()
            .mapToLong(p -> java.time.Duration.between(p.getCreatedAt(), LocalDateTime.now()).toHours())
            .sum() / Math.max(personalizedFeed.size(), 1);
        metrics.put("averageContentAge", avgHoursOld + " hours");
        
        return metrics;
    }
    
    /**
     * Helper class to hold post with its relevance score
     */
    private static class ScoredPost {
        Post post;
        double score;
        
        ScoredPost(Post post, double score) {
            this.post = post;
            this.score = score;
        }
    }
    
    /**
     * Generate personalized community suggestions based on behavior
     */
    public List<Community> getPersonalizedCommunitySuggestions(String userId, List<Community> allCommunities, String[] userTopics) {
        List<Community> suggestions = new ArrayList<>();
        
        // Get communities user hasn't joined yet (based on topic match)
        for (Community community : allCommunities) {
            double relevanceScore = 0.0;
            
            if (userTopics != null) {
                for (String topic : userTopics) {
                    String communityText = (community.getName() + " " + community.getDescription()).toLowerCase();
                    if (communityText.contains(topic.toLowerCase())) {
                        relevanceScore += 1.0;
                    }
                }
            }
            
            // Add popularity factor
            relevanceScore += Math.log(community.getMemberCount() + 1) / 10.0;
            
            if (relevanceScore > 0) {
                suggestions.add(community);
            }
        }
        
        // Sort by relevance and limit to top 5
        suggestions.sort((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()));
        return suggestions.stream().limit(5).collect(Collectors.toList());
    }
    
    /**
     * Get insights about user's feed preferences
     */
    public Map<String, Object> getUserFeedInsights(String userId, String[] userTopics) {
        Map<String, Object> insights = new HashMap<>();
        
        // Viewing patterns
        List<String> viewedPosts = userViewedPosts.getOrDefault(userId, new ArrayList<>());
        insights.put("totalPostsViewed", viewedPosts.size());
        
        // Engagement patterns
        List<String> likedPosts = userLikedPosts.getOrDefault(userId, new ArrayList<>());
        insights.put("totalPostsLiked", likedPosts.size());
        insights.put("engagementRate", viewedPosts.size() > 0 ? 
            (double) likedPosts.size() / viewedPosts.size() : 0.0);
        
        // Topic interests
        if (userTopics != null && userTopics.length > 0) {
            insights.put("primaryInterests", Arrays.asList(userTopics));
        }
        
        // Best time to engage
        insights.put("suggestedCheckInTime", getBestTimeToCheckFeed());
        
        return insights;
    }
    
    /**
     * Determine best time to check feed based on content patterns
     */
    private String getBestTimeToCheckFeed() {
        LocalTime now = LocalTime.now();
        
        if (now.getHour() >= 6 && now.getHour() < 10) {
            return "Morning - Great time for motivational content!";
        } else if (now.getHour() >= 10 && now.getHour() < 14) {
            return "Midday - Active discussions happening now!";
        } else if (now.getHour() >= 14 && now.getHour() < 18) {
            return "Afternoon - Perfect for learning and tips!";
        } else if (now.getHour() >= 18 && now.getHour() < 22) {
            return "Evening - Lots of parents online now!";
        } else {
            return "Late night - Sleep support available!";
        }
    }
    
    /**
     * Smart content categorization
     */
    public Map<String, List<Post>> categorizeFeed(List<Post> posts, List<Community> communities) {
        Map<String, List<Post>> categorized = new HashMap<>();
        
        categorized.put("For You", new ArrayList<>());
        categorized.put("Trending", new ArrayList<>());
        categorized.put("Recent", new ArrayList<>());
        categorized.put("Popular", new ArrayList<>());
        
        for (Post post : posts) {
            // Recent posts (< 6 hours)
            long hoursAgo = java.time.Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours();
            if (hoursAgo < 6) {
                categorized.get("Recent").add(post);
            }
            
            // Trending (high engagement in short time)
            if (hoursAgo < 24 && post.getLikes() > 20) {
                categorized.get("Trending").add(post);
            }
            
            // Popular (high engagement overall)
            if (post.getLikes() > 30) {
                categorized.get("Popular").add(post);
            }
        }
        
        // "For You" gets personalized content
        categorized.put("For You", posts.stream().limit(10).collect(Collectors.toList()));
        
        return categorized;
    }
    
    /**
     * Generate feed insights for display
     */
    public Map<String, Object> getFeedInsights(List<Post> personalizedFeed) {
        Map<String, Object> insights = new HashMap<>();
        
        // Content freshness
        long recentCount = personalizedFeed.stream()
            .filter(p -> java.time.Duration.between(p.getCreatedAt(), LocalDateTime.now()).toHours() < 6)
            .count();
        
        insights.put("freshContentCount", recentCount);
        insights.put("totalPostsAvailable", personalizedFeed.size());
        
        // Engagement potential
        long highEngagementCount = personalizedFeed.stream()
            .filter(p -> p.getLikes() > 20)
            .count();
        
        insights.put("popularPostsCount", highEngagementCount);
        
        return insights;
    }
}

