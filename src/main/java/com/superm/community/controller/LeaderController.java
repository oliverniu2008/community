package com.superm.community.controller;

import com.superm.community.service.StaticDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LeaderController {
	private final StaticDataService data;
	public LeaderController(StaticDataService data) { this.data = data; }

	@GetMapping({"/leader","/leaders","/leader/"})
	public String dashboard(Model model) {
		model.addAttribute("communities", data.getAllCommunities());
		model.addAttribute("engagement", data.getEngagementByCommunity());
		return "leader";
	}
}
