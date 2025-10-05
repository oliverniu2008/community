package com.superm.community.controller;

import com.superm.community.service.StaticDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminController {
	private final StaticDataService data;
	public AdminController(StaticDataService data) { this.data = data; }

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
        Model model) {
    var leaders = data.getAllUsers().stream().filter(u -> u.isLeader()).toList();
    java.util.Map<String, String> assigned = new java.util.HashMap<>();
    java.util.Map<String, Integer> performance = new java.util.HashMap<>();
    for (var u : leaders) {
        var led = data.getAllCommunities().stream().filter(c -> c.getLeaderName().equals(u.getName())).toList();
        String names = led.stream().map(c -> c.getName()).reduce((a,b) -> a + ", " + b).orElse("");
        assigned.put(u.getName(), names);
        var ids = led.stream().map(c -> c.getId()).toList();
        int posts = (int) data.getAllPosts().stream().filter(p -> ids.contains(p.getCommunityId())).count();
        performance.put(u.getName(), posts);
    }
    model.addAttribute("leaders", leaders);
    model.addAttribute("assigned", assigned);
    model.addAttribute("performance", performance);
    model.addAttribute("rewards", data.processPayouts(dollars, tokens));
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
}
