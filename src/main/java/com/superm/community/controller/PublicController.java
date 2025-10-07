package com.superm.community.controller;

import com.superm.community.model.Community;
import com.superm.community.service.StaticDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PublicController {
	private final StaticDataService data;

	public PublicController(StaticDataService data) {
		this.data = data;
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
	public String onboarding() {
		return "onboarding";
	}

	@GetMapping("/feed")
	public String feed(Model model) {
		model.addAttribute("communities", data.getAllCommunities());
		model.addAttribute("posts", data.getAllPosts());
		return "feed";
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
}
