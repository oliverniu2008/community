package com.superm.community.controller;

import com.superm.community.model.Community;
import com.superm.community.model.User;
import com.superm.community.service.StaticDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpSession;

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

	@GetMapping("/feed")
	public String feed(HttpSession session, Model model) {
		// Check if user is logged in
		String userId = (String) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/login";
		}
		
		model.addAttribute("userName", session.getAttribute("userName"));
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
