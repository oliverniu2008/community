package com.superm.community.controller;

import com.superm.community.service.StaticDataService;
import com.superm.community.service.ActivityPlannerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class AdminController {
	private final StaticDataService data;
	private final ActivityPlannerService activityPlannerService;
	
	public AdminController(StaticDataService data, ActivityPlannerService activityPlannerService) { 
		this.data = data;
		this.activityPlannerService = activityPlannerService;
	}

	@GetMapping("/admin")
	public String dashboard(Model model) {
		model.addAttribute("totalUsers", data.getAllUsers().size());
		model.addAttribute("totalCommunities", data.getAllCommunities().size());
		model.addAttribute("dailyActive", 123);
		return "admin";
	}

	@GetMapping("/admin/users")
	public String users(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "status", required = false) String status,
			Model model) {
		var filteredUsers = data.filterUsers(q, status);
		java.util.Map<String, String> komTags = new java.util.HashMap<>();
		for (var user : filteredUsers) {
			komTags.put(user.getId(), data.getKomTagForUser(user.getId()));
		}
		
		model.addAttribute("users", filteredUsers);
		model.addAttribute("komTags", komTags);
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("status", status == null ? "" : status);
		return "admin-users";
	}

	@PostMapping("/admin/users/{id}/suspend")
	public String suspendUser(@PathVariable("id") String id) {
		data.suspendUser(id);
		return "redirect:/admin/users";
	}

	@PostMapping("/admin/users/{id}/activate")
	public String activateUser(@PathVariable("id") String id) {
		data.activateUser(id);
		return "redirect:/admin/users";
	}

	@GetMapping("/admin/communities")
	public String communities(Model model) {
		model.addAttribute("communities", data.getAllCommunities());
		return "admin-communities";
	}

	@GetMapping("/admin/communities/{id}/view")
	public String viewCommunity(@PathVariable("id") String id, Model model) {
		var community = data.findCommunityById(id);
		if (community == null) {
			return "redirect:/admin/communities";
		}
		
		var articles = data.getArticlesByCommunity(id);
		model.addAttribute("community", community);
		model.addAttribute("articles", articles);
		return "admin-community-view";
	}

	@PostMapping("/admin/communities/{id}/archive")
	public String archiveCommunity(@PathVariable("id") String id) {
		data.archiveCommunity(id);
		return "redirect:/admin/communities";
	}

	@GetMapping("/admin/leaders")
public String leaders(Model model) {
    var leaders = data.getAllUsers().stream().filter(u -> u.isLeader()).toList();
    java.util.Map<String, String> assigned = new java.util.HashMap<>();
    java.util.Map<String, Integer> performance = new java.util.HashMap<>();
    var all = data.getAllCommunities();
    for (var u : leaders) {
        var led = all.stream().filter(c -> c.getLeaderName().equals(u.getName())).toList();
        String names = led.stream().map(c -> c.getName()).reduce((a,b) -> a + ", " + b).orElse("");
        if (names.isEmpty() && !all.isEmpty()) {
            int count = 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(Math.min(2, Math.max(1, all.size())));
            java.util.Set<Integer> picks = new java.util.HashSet<>();
            while (picks.size() < count) { picks.add(java.util.concurrent.ThreadLocalRandom.current().nextInt(all.size())); }
            names = picks.stream().map(idx -> all.get(idx).getName()).reduce((a,b) -> a + ", " + b).orElse("");
        }
        assigned.put(u.getName(), names);
        int posts = java.util.concurrent.ThreadLocalRandom.current().nextInt(11);
        performance.put(u.getName(), posts);
    }
    model.addAttribute("leaders", leaders);
    model.addAttribute("assigned", assigned);
    model.addAttribute("performance", performance);
    return "admin-leaders";
}

	@PostMapping("/admin/rewards/payouts")
public String processPayouts(@RequestParam("dollars") int dollars,
        @RequestParam("tokens") int tokens,
        @RequestParam(value = "selectedLeaders", required = false) String selectedLeadersStr,
        Model model) {
    
    // Get all leaders for display
    var allLeaders = data.getAllUsers().stream().filter(u -> u.isLeader()).toList();
    java.util.Map<String, String> assigned = new java.util.HashMap<>();
    java.util.Map<String, Integer> performance = new java.util.HashMap<>();
    
    for (var u : allLeaders) {
        var led = data.getAllCommunities().stream().filter(c -> c.getLeaderName().equals(u.getName())).toList();
        String names = led.stream().map(c -> c.getName()).reduce((a,b) -> a + ", " + b).orElse("");
        assigned.put(u.getName(), names);
        var ids = led.stream().map(c -> c.getId()).toList();
        int posts = (int) data.getAllPosts().stream().filter(p -> ids.contains(p.getCommunityId())).count();
        performance.put(u.getName(), posts);
    }
    
    // Process payouts for selected leaders only
    java.util.List<String> processedRewards = new java.util.ArrayList<>();
    
    if (selectedLeadersStr != null && !selectedLeadersStr.trim().isEmpty()) {
        String[] selectedLeaderIds = selectedLeadersStr.split(",");
        int selectedCount = 0;
        int totalDollars = 0;
        int totalTokens = 0;
        
        for (String leaderIdStr : selectedLeaderIds) {
            final String leaderId = leaderIdStr.trim();
            if (!leaderId.isEmpty()) {
                var leader = allLeaders.stream().filter(u -> u.getId().equals(leaderId)).findFirst().orElse(null);
                if (leader != null) {
                    selectedCount++;
                    int leaderPosts = performance.getOrDefault(leader.getName(), 0);
                    int leaderDollars = leaderPosts * dollars;
                    int leaderTokens = leaderPosts * tokens;
                    totalDollars += leaderDollars;
                    totalTokens += leaderTokens;
                    
                    processedRewards.add("Processed payout for " + leader.getName() + 
                        ": $" + leaderDollars + " + " + leaderTokens + " tokens (" + leaderPosts + " posts)");
                }
            }
        }
        
        if (selectedCount > 0) {
            processedRewards.add(0, "BULK PAYOUT: Processed $" + totalDollars + " and " + totalTokens + " tokens for " + selectedCount + " selected leaders.");
            data.processPayouts(dollars, tokens); // Add to reward log
        }
    } else {
        // Fallback to original behavior if no leaders selected
        processedRewards.addAll(data.processPayouts(dollars, tokens));
    }
    
    model.addAttribute("leaders", allLeaders);
    model.addAttribute("assigned", assigned);
    model.addAttribute("performance", performance);
    model.addAttribute("rewards", processedRewards);
    return "admin-leaders";
}

	@GetMapping("/admin/moderation")
	public String moderation(Model model) {
		model.addAttribute("reports", java.util.List.of("Reported post p2", "Reported comment by u3"));
		return "admin-moderation";
	}

	@PostMapping("/admin/moderation/delete")
	public String deleteContent(@RequestParam("postId") String postId) {
		data.deleteContent(postId);
		return "redirect:/admin/moderation";
	}

	@PostMapping("/admin/moderation/warn")
	public String warnUser(@RequestParam("userId") String userId) {
		data.warnUser(userId);
		return "redirect:/admin/moderation";
	}

	@GetMapping("/admin/analytics")
	public String analytics(Model model) {
		model.addAttribute("overview", data.getAnalyticsOverview());
		model.addAttribute("userEvents", data.getUserEvents());
		model.addAttribute("locationData", data.getLocationData());
		model.addAttribute("deviceData", data.getDeviceData());
		model.addAttribute("eventTypes", data.getEventTypes());
		model.addAttribute("topPages", data.getTopPages());
		model.addAttribute("trafficSources", data.getTrafficSources());
		model.addAttribute("hourlyTraffic", data.getHourlyTraffic());
		model.addAttribute("ageGroups", data.getAgeGroups());
		model.addAttribute("genderDistribution", data.getGenderDistribution());
		model.addAttribute("topReferrers", data.getTopReferrers());
		model.addAttribute("sessionDuration", data.getSessionDuration());
		model.addAttribute("browserData", data.getBrowserData());
		model.addAttribute("operatingSystems", data.getOperatingSystems());
		return "admin-analytics";
	}

	@GetMapping("/admin/content-generator")
	public String contentGenerator(Model model) {
		model.addAttribute("communities", data.getAllCommunities());
		model.addAttribute("socialPlatforms", data.getSocialMediaPlatforms());
		model.addAttribute("contentTemplates", data.getContentTemplates());
		model.addAttribute("recentContent", data.getRecentGeneratedContent());
		return "admin-content-generator";
	}

	@PostMapping("/admin/content-generator/generate")
	public String generateContent(@RequestParam("contentType") String contentType,
								 @RequestParam("topic") String topic,
								 @RequestParam("communityId") String communityId,
								 @RequestParam(value = "aiTool", defaultValue = "chatgpt") String aiTool,
								 Model model) {
		var generatedContent = data.generateContent(contentType, topic, communityId, aiTool);
		model.addAttribute("generatedContent", generatedContent);
		model.addAttribute("communities", data.getAllCommunities());
		model.addAttribute("socialPlatforms", data.getSocialMediaPlatforms());
		model.addAttribute("contentTemplates", data.getContentTemplates());
		model.addAttribute("recentContent", data.getRecentGeneratedContent());
		return "admin-content-generator";
	}

	@PostMapping("/admin/content-generator/post")
	public String postToSocialMedia(@RequestParam("contentId") String contentId,
								   @RequestParam(value = "platforms", required = false) String[] platforms,
								   Model model) {
		// Simulate posting to social media platforms
		if (platforms == null || platforms.length == 0) {
			model.addAttribute("error", "No platforms selected");
		} else {
			model.addAttribute("postedPlatforms", platforms);
		}
		model.addAttribute("contentId", contentId);
		model.addAttribute("communities", data.getAllCommunities());
		model.addAttribute("socialPlatforms", data.getSocialMediaPlatforms());
		model.addAttribute("contentTemplates", data.getContentTemplates());
		model.addAttribute("recentContent", data.getRecentGeneratedContent());
		return "admin-content-generator";
	}

	@GetMapping("/admin/content-generator/view/{id}")
	public String viewGeneratedContent(@PathVariable("id") String id, Model model) {
		var generatedContent = data.getGeneratedContentById(id);
		if (generatedContent == null) {
			return "redirect:/admin/content-generator";
		}
		model.addAttribute("generatedContent", generatedContent);
		model.addAttribute("communities", data.getAllCommunities());
		model.addAttribute("socialPlatforms", data.getSocialMediaPlatforms());
		model.addAttribute("contentTemplates", data.getContentTemplates());
		model.addAttribute("recentContent", data.getRecentGeneratedContent());
		return "admin-content-generator";
	}
	
	// Activity Planner AI Agent endpoints
	@GetMapping("/admin/activity-planner")
	public String activityPlanner(Model model) {
		Map<String, Object> serviceStatus = activityPlannerService.getServiceStatus();
		Map<String, Object> documentation = activityPlannerService.getServiceDocumentation();
		
		model.addAttribute("serviceStatus", serviceStatus);
		model.addAttribute("documentation", documentation);
		return "admin-activity-planner";
	}
	
	@PostMapping("/admin/activity-planner/start")
	@ResponseBody
	public Map<String, Object> startActivityPlanner() {
		Map<String, Object> response = new HashMap<>();
		try {
			String serviceUrl = activityPlannerService.startActivityPlannerService();
			response.put("success", true);
			response.put("message", "Activity Planner service started successfully");
			response.put("serviceUrl", serviceUrl);
		} catch (Exception e) {
			response.put("success", false);
			response.put("error", e.getMessage());
		}
		return response;
	}
	
	@PostMapping("/admin/activity-planner/stop")
	@ResponseBody
	public Map<String, Object> stopActivityPlanner() {
		Map<String, Object> response = new HashMap<>();
		try {
			String result = activityPlannerService.stopActivityPlannerService();
			response.put("success", true);
			response.put("message", result);
		} catch (Exception e) {
			response.put("success", false);
			response.put("error", e.getMessage());
		}
		return response;
	}
	
	@GetMapping("/admin/activity-planner/status")
	@ResponseBody
	public Map<String, Object> getActivityPlannerStatus() {
		return activityPlannerService.getServiceStatus();
	}
}
