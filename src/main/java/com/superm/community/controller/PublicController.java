package com.superm.community.controller;

import com.superm.community.model.Community;
import com.superm.community.model.User;
import com.superm.community.model.Post;
import com.superm.community.service.StaticDataService;
import com.superm.community.service.AIAssistantService;
import com.superm.community.service.PersonalizationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
public class PublicController {
	private final StaticDataService data;
	private final AIAssistantService aiService;
	private final PersonalizationService personalizationService;

	public PublicController(StaticDataService data, AIAssistantService aiService, PersonalizationService personalizationService) {
		this.data = data;
		this.aiService = aiService;
		this.personalizationService = personalizationService;
	}

	@GetMapping("/")
	public String home(@RequestParam(value = "q", required = false) String q, Model model) {
		List<Community> featured = data.getAllCommunities();
		List<java.util.Map<String, Object>> searchResults = null;
		
		if (q != null && !q.isBlank()) {
			String ql = q.toLowerCase();
			// Search communities
			featured = featured.stream().filter(c ->
				c.getName().toLowerCase().contains(ql) || c.getDescription().toLowerCase().contains(ql)
			).collect(Collectors.toList());
			
			// Search articles across all communities
			searchResults = data.searchArticles(ql);
		}
		
		model.addAttribute("featured", featured);
		model.addAttribute("searchResults", searchResults);
		model.addAttribute("searchQuery", q);
		return "home";
	}

	@GetMapping("/onboarding")
	public String onboarding(@RequestParam(value = "track", required = false) String referrerId, 
	                        HttpSession session, 
	                        Model model) {
		// Store referrer ID in session for tracking
		if (referrerId != null && !referrerId.isBlank()) {
			session.setAttribute("referrerId", referrerId);
			
			// Get referrer user info for display
			User referrer = data.findUserById(referrerId);
			if (referrer != null) {
				model.addAttribute("referrerName", referrer.getName());
				model.addAttribute("hasReferrer", true);
			}
		}
		return "onboarding";
	}

	@GetMapping("/signup")
	public String signupPage(@RequestParam(value = "track", required = false) String referrerId, 
	                        HttpSession session, 
	                        Model model) {
		// Store referrer ID in session for tracking
		if (referrerId != null && !referrerId.isBlank()) {
			session.setAttribute("referrerId", referrerId);
			
			// Get referrer user info for display
			User referrer = data.findUserById(referrerId);
			if (referrer != null) {
				model.addAttribute("referrerName", referrer.getName());
				model.addAttribute("hasReferrer", true);
			}
		}
		return "signup";
	}
	
	@PostMapping("/signup")
	public String signup(@RequestParam("email") String email,
	                    @RequestParam("password") String password,
	                    @RequestParam("fullName") String fullName,
	                    @RequestParam(value = "topics", required = false) String[] topics,
	                    HttpSession session,
	                    Model model) {
		// Validate topics
		if (topics == null || topics.length != 3) {
			model.addAttribute("error", "Please select exactly 3 topics");
			return "signup";
		}
		
		// Check if email already exists
		if (data.emailExists(email)) {
			model.addAttribute("error", "Email already registered. Please login instead.");
			return "signup";
		}
		
		// Get referrer ID from session
		String referrerId = (String) session.getAttribute("referrerId");
		
		// Create new user
		User newUser = data.createUser(email, password, fullName, topics, referrerId);
		
		// Auto-login the user
		session.setAttribute("userId", newUser.getId());
		session.setAttribute("userName", newUser.getName());
		session.setAttribute("userEmail", newUser.getEmail());
		session.setAttribute("userTopics", topics);
		
		return "redirect:/feed";
	}

	@GetMapping("/login")
	public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
		if (error != null) {
			model.addAttribute("error", "Invalid username or password");
		}
		return "login";
	}
	
	@PostMapping("/login")
	public String login(@RequestParam("username") String username,
	                   @RequestParam("password") String password,
	                   HttpSession session,
	                   Model model) {
		User user = data.authenticateUser(username, password);
		if (user != null) {
			// Store user info in session
			session.setAttribute("userId", user.getId());
			session.setAttribute("userName", user.getName());
			session.setAttribute("userEmail", user.getEmail());
			return "redirect:/feed";
		} else {
			model.addAttribute("error", "Invalid username or password");
			return "login";
		}
	}
	
	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/";
	}
	
	@GetMapping("/profile")
	public String profile(HttpSession session, Model model) {
		// Check if user is logged in
		String userId = (String) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/login";
		}
		
		// Get user details
		User user = data.findUserById(userId);
		if (user == null) {
			return "redirect:/login";
		}
		
		// Generate invite link with tracking
		String inviteLink = "http://localhost:8080/onboarding?track=" + userId;
		
		model.addAttribute("user", user);
		model.addAttribute("inviteLink", inviteLink);
		return "profile";
	}

	@GetMapping("/feed")
	public String feed(HttpSession session, Model model) {
		// Check if user is logged in
		String userId = (String) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/login";
		}
		
		// Get user topics from session
		String[] userTopics = (String[]) session.getAttribute("userTopics");
		
		// Get recommended communities based on topics
		List<Community> recommendedCommunities = data.getRecommendedCommunities(userTopics);
		List<Community> allCommunities = data.getAllCommunities();
		
		// Get AI-personalized feed
		List<Post> allPosts = data.getAllPosts();
		List<Post> personalizedPosts = personalizationService.getPersonalizedFeed(
			userId, allPosts, userTopics, allCommunities
		);
		
		// Get categorized feed
		Map<String, List<Post>> categorizedFeed = personalizationService.categorizeFeed(personalizedPosts, allCommunities);
		
		// Get feed insights
		Map<String, Object> feedInsights = personalizationService.getFeedInsights(personalizedPosts);
		Map<String, Object> userInsights = personalizationService.getUserFeedInsights(userId, userTopics);
		
		model.addAttribute("userName", session.getAttribute("userName"));
		model.addAttribute("communities", recommendedCommunities);
		model.addAttribute("allCommunities", allCommunities);
		model.addAttribute("posts", personalizedPosts);
		model.addAttribute("categorizedFeed", categorizedFeed);
		model.addAttribute("feedInsights", feedInsights);
		model.addAttribute("userInsights", userInsights);
		model.addAttribute("hasRecommendations", userTopics != null && userTopics.length > 0);
		return "feed";
	}
	
	// Track user engagement
	@PostMapping("/feed/track-view")
	@ResponseBody
	public Map<String, Object> trackView(@RequestBody Map<String, String> request, HttpSession session) {
		String userId = (String) session.getAttribute("userId");
		String postId = request.get("postId");
		
		if (userId != null && postId != null) {
			personalizationService.trackPostView(userId, postId);
		}
		
		return Map.of("success", true);
	}
	
	@PostMapping("/feed/track-like")
	@ResponseBody
	public Map<String, Object> trackLike(@RequestBody Map<String, String> request, HttpSession session) {
		String userId = (String) session.getAttribute("userId");
		String postId = request.get("postId");
		
		if (userId != null && postId != null) {
			personalizationService.trackPostLike(userId, postId);
		}
		
		return Map.of("success", true);
	}

	@GetMapping("/community/{id}")
	public String community(@PathVariable String id, Model model) {
		Community community = data.findCommunityById(id);
		if (community == null) {
			return "redirect:/";
		}
		model.addAttribute("community", community);
		model.addAttribute("posts", data.getPostsForCommunity(id));
		model.addAttribute("articles", data.getArticlesByCommunity(id));
		return "community";
	}
	
	// AI Assistant endpoints
	@GetMapping("/ai-assistant")
	public String aiAssistant(HttpSession session, Model model) {
		// Check if user is logged in
		String userId = (String) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/login";
		}
		
		model.addAttribute("userName", session.getAttribute("userName"));
		return "ai-assistant";
	}
	
	@PostMapping("/ai-assistant/chat")
	@ResponseBody
	public Map<String, Object> aiChat(@RequestBody Map<String, Object> request, HttpSession session) {
		Map<String, Object> response = new HashMap<>();
		
		try {
			// Check if user is logged in
			String userName = (String) session.getAttribute("userName");
			if (userName == null) {
				response.put("success", false);
				response.put("error", "Not authenticated");
				return response;
			}
			
			String message = (String) request.get("message");
			@SuppressWarnings("unchecked")
			List<Map<String, String>> history = (List<Map<String, String>>) request.get("history");
			
			// Get AI response
			String aiResponse = aiService.getAIResponse(message, userName, history);
			
			response.put("success", true);
			response.put("response", aiResponse);
			response.put("canShare", true);
			
		} catch (Exception e) {
			response.put("success", false);
			response.put("error", e.getMessage());
		}
		
		return response;
	}
}
