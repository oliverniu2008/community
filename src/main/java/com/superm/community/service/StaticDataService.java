package com.superm.community.service;

import com.superm.community.model.Community;
import com.superm.community.model.Post;
import com.superm.community.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service("staticDataService")
public class StaticDataService {
	private final List<User> users;
	private final List<Community> communities;
	private final List<Post> posts;
	private final List<String> rewardLog;
	private final List<java.util.Map<String, Object>> recentGeneratedContent;

	public StaticDataService() {
		Random random = new Random(42); // Fixed seed for consistent results
		this.users = new ArrayList<>(Arrays.asList(
			new User("u1", "parent1@example.com", "Avery Kim", "Avery Kim", false, "Active", generateRandomJoinDate(random)),
			new User("u2", "leader1@example.com", "Jordan Lee", "Jordan Lee", true, "Active", generateRandomJoinDate(random)),
			new User("u3", "parent2@example.com", "Sam Patel", "Sam Patel", false, "Active", generateRandomJoinDate(random)),
			new User("u4", "leader2@example.com", "Morgan Rivera", "Morgan Rivera", true, "Active", generateRandomJoinDate(random)),
			new User("u5", "leader3@example.com", "Riley Chen", "Riley Chen", true, "Active", generateRandomJoinDate(random)),
			new User("u6", "leader4@example.com", "Taylor Singh", "Taylor Singh", true, "Active", generateRandomJoinDate(random)),
			new User("u7", "leader5@example.com", "Jamie Brooks", "Jamie Brooks", true, "Active", generateRandomJoinDate(random)),
			new User("u8", "leader6@example.com", "Casey Nguyen", "Casey Nguyen", true, "Active", generateRandomJoinDate(random)),
			new User("u9", "leader7@example.com", "Alex Martinez", "Alex Martinez", true, "Active", generateRandomJoinDate(random)),
			new User("u10", "leader8@example.com", "Charlie Ahmed", "Charlie Ahmed", true, "Active", generateRandomJoinDate(random)),
			new User("u11", "leader9@example.com", "Parker Rossi", "Parker Rossi", true, "Active", generateRandomJoinDate(random)),
			new User("u12", "leader10@example.com", "Drew Nakamura", "Drew Nakamura", true, "Active", generateRandomJoinDate(random)),
			new User("u13", "leader11@example.com", "Jordan Scott", "Jordan Scott", true, "Active", generateRandomJoinDate(random))));

		this.communities = new ArrayList<>(Arrays.asList(
			new Community("c1", "Pregnancy Nutrition", "Tips and support for healthy pregnancy nutrition.", "Jordan Lee", 128, LocalDate.now().minusDays(42)),
			new Community("c2", "ADHD Toddlers", "Strategies, stories, and support for caregivers of ADHD toddlers.", "Avery Kim", 256, LocalDate.now().minusDays(10)),
			new Community("c3", "Sleep Training 0-6m", "Gentle sleep training practices for newborns.", "Jordan Lee", 312, LocalDate.now().minusDays(5)),
			new Community("c4", "Postpartum Health", "Recovery and wellness for new parents.", "Jordan Lee", 189, LocalDate.now().minusDays(20)),
			new Community("c5", "Special Needs Support", "Resources and peer support for special needs caregivers.", "Avery Kim", 142, LocalDate.now().minusDays(65)),
			new Community("c6", "Parent Fitness", "Quick routines and tips to stay active.", "Sam Patel", 98, LocalDate.now().minusDays(8))
		));

		List<Post> seed = new ArrayList<>();
		seed.add(new Post("p1", "c1", "Jordan Lee", "What are your go-to iron-rich snacks?", null, LocalDateTime.now().minusHours(12), 24, 5));
		seed.add(new Post("p2", "c2", "Avery Kim", "Sharing a routine that helped our evenings.", null, LocalDateTime.now().minusHours(6), 42, 12));
		seed.add(new Post("p3", "c3", "Sam Patel", "Nights are hard—what worked for you?", null, LocalDateTime.now().minusHours(2), 13, 7));
		this.posts = new ArrayList<>(seed);

		this.rewardLog = new ArrayList<>();
		this.recentGeneratedContent = new ArrayList<>();
	}

	public List<Community> getAllCommunities() { return communities; }
	public List<Post> getAllPosts() { return posts; }
	public List<User> getAllUsers() { return users; }

	public Community findCommunityById(String id) {
		return communities.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
	}

	public List<Post> getPostsForCommunity(String communityId) {
		return posts.stream().filter(p -> p.getCommunityId().equals(communityId)).collect(Collectors.toList());
	}

	public Map<String, Long> getEngagementByCommunity() {
		return posts.stream().collect(Collectors.groupingBy(Post::getCommunityId, Collectors.counting()));
	}

	public List<User> filterUsers(String query, String status) {
		return users.stream().filter(u -> {
			boolean matchesQuery = (query == null || query.isBlank()) ||
				u.getEmail().toLowerCase().contains(query.toLowerCase()) ||
				u.getName().toLowerCase().contains(query.toLowerCase());
			boolean matchesStatus = (status == null || status.isBlank()) || u.getStatus().equalsIgnoreCase(status);
			return matchesQuery && matchesStatus;
		}).collect(Collectors.toList());
	}

	private LocalDate generateRandomJoinDate(Random random) {
		// Generate random date between 2012-01-01 and 2024-12-31
		int year = 2012 + random.nextInt(13); // 2012 to 2024
		int month = 1 + random.nextInt(12); // 1 to 12
		int day = 1 + random.nextInt(28); // 1 to 28 (safe for all months)
		return LocalDate.of(year, month, day);
	}

	public boolean suspendUser(String userId) {
		for (int i = 0; i < users.size(); i++) {
			User u = users.get(i);
			if (u.getId().equals(userId)) {
				users.set(i, new User(u.getId(), u.getEmail(), u.getName(), u.getFullName(), u.isLeader(), "Suspended", u.getJoinDate()));
				return true;
			}
		}
		return false;
	}

	public boolean activateUser(String userId) {
		for (int i = 0; i < users.size(); i++) {
			User u = users.get(i);
			if (u.getId().equals(userId)) {
				users.set(i, new User(u.getId(), u.getEmail(), u.getName(), u.getFullName(), u.isLeader(), "Active", u.getJoinDate()));
				return true;
			}
		}
		return false;
	}

	public boolean archiveCommunity(String communityId) {
		Iterator<Community> it = communities.iterator();
		while (it.hasNext()) {
			Community c = it.next();
			if (c.getId().equals(communityId)) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	public boolean deleteContent(String postId) {
		return posts.removeIf(p -> p.getId().equals(postId));
	}

	public boolean warnUser(String userId) {
		// Prototype: pretend to warn
		return users.stream().anyMatch(u -> u.getId().equals(userId));
	}

	public List<String> processPayouts(int dollarsPerNewMember, int tokensPer100Engagements) {
		rewardLog.add("Processed payouts: $" + dollarsPerNewMember + ", " + tokensPer100Engagements + " tokens/100 engagements");
		return new ArrayList<>(rewardLog);
	}

	public String getKomTagForUser(String userId) {
		User user = users.stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
		if (user == null) return "Unknown";
		
		// Find communities led by this user
		List<Community> userCommunities = communities.stream()
			.filter(c -> c.getLeaderName().equals(user.getName()))
			.collect(Collectors.toList());
		
		if (!userCommunities.isEmpty()) {
			// Leaders get their first community's name as KOM tag
			return userCommunities.get(0).getName();
		}
		
		// For non-leaders, assign KOM tags based on user ID for consistency
		// This simulates community membership/interest areas
		switch (userId) {
			case "u1": return "ADHD Toddlers"; // Avery Kim - matches their led community
			case "u3": return "Sleep Training 0-6m"; // Sam Patel - matches their led community
			case "u4": return "N.A"; // Morgan Rivera - assigned N.A
			case "u5": return "N.A"; // Riley Chen - assigned N.A
			default: 
				// Assign random community types for other non-leaders
				String[] communityTypes = {
					"Pregnancy Nutrition", "ADHD Toddlers", "Sleep Training 0-6m", 
					"Postpartum Health", "Special Needs Support", "Parent Fitness"
				};
				int index = Math.abs(userId.hashCode()) % communityTypes.length;
				return communityTypes[index];
		}
	}

	public java.util.List<String> getArticleTitlesByCommunity(String communityId) {
		java.util.List<String> titles = new java.util.ArrayList<>();
		
		switch (communityId) {
			case "c1": // Pregnancy Nutrition
				titles.add("Iron-rich foods during pregnancy");
				titles.add("Hydration tips for expecting parents");
				titles.add("Managing morning sickness with diet");
				titles.add("Prenatal vitamins: What you need to know");
				titles.add("Healthy weight gain during pregnancy");
				break;
			case "c2": // ADHD Toddlers
				titles.add("5 calming activities for ADHD toddlers");
				titles.add("Structuring your day: routines that help");
				titles.add("Sensory-friendly play ideas");
				titles.add("Understanding ADHD in toddlers");
				titles.add("Communication strategies for ADHD toddlers");
				break;
			case "c3": // Sleep Training 0-6m
				titles.add("Newborn sleep windows explained");
				titles.add("Creating a gentle bedtime routine");
				titles.add("Swaddling do's and don'ts");
				titles.add("Sleep training methods compared");
				titles.add("Night feeding schedules for newborns");
				break;
			case "c4": // Postpartum Health
				titles.add("Postpartum recovery essentials");
				titles.add("Pelvic floor basics");
				titles.add("Nutrition for healing");
				titles.add("Postpartum depression: Signs and support");
				titles.add("Returning to exercise postpartum");
				break;
			case "c5": // Special Needs Support
				titles.add("IEP: where to start");
				titles.add("Building your support team");
				titles.add("Everyday sensory strategies");
				titles.add("Understanding different special needs");
				titles.add("Financial resources for special needs families");
				break;
			case "c6": // Parent Fitness
				titles.add("10-minute workouts for busy parents");
				titles.add("Stroller-friendly cardio");
				titles.add("Home stretching routine");
				titles.add("Nutrition for active parents");
				titles.add("Finding time for fitness as a parent");
				break;
			default: 
				titles.add("Welcome to our community");
		}
		
		return titles;
	}

	public java.util.List<java.util.Map<String, Object>> getArticlesByCommunity(String communityId) {
		java.util.List<java.util.Map<String, Object>> articles = new java.util.ArrayList<>();
		
		switch (communityId) {
			case "c1": // Pregnancy Nutrition
				articles.add(createArticle("Iron-rich foods during pregnancy", "Jordan Lee", "This comprehensive guide covers everything you need to know about iron-rich foods during pregnancy. Learn about the best sources, absorption tips, and meal planning strategies.", "2 days ago", 1200, 45, 12));
				articles.add(createArticle("Hydration tips for expecting parents", "Jordan Lee", "Stay properly hydrated during pregnancy with these expert tips. Learn about water intake, signs of dehydration, and healthy beverage alternatives.", "5 days ago", 890, 32, 8));
				articles.add(createArticle("Managing morning sickness with diet", "Jordan Lee", "Discover effective dietary strategies to manage morning sickness. From ginger remedies to meal timing, find what works for you.", "1 week ago", 1560, 67, 23));
				articles.add(createArticle("Prenatal vitamins: What you need to know", "Admin", "A complete guide to prenatal vitamins, including timing, dosage, and what to look for when choosing supplements.", "2 weeks ago", 2100, 89, 34));
				articles.add(createArticle("Healthy weight gain during pregnancy", "Admin", "Learn about healthy weight gain patterns during pregnancy and how to maintain proper nutrition while gaining weight appropriately.", "3 weeks ago", 1800, 76, 28));
				break;
			case "c2": // ADHD Toddlers
				articles.add(createArticle("5 calming activities for ADHD toddlers", "Avery Kim", "Discover effective calming activities specifically designed for toddlers with ADHD. These strategies help manage energy and improve focus.", "1 day ago", 980, 42, 15));
				articles.add(createArticle("Structuring your day: routines that help", "Avery Kim", "Learn how to create effective daily routines that support your ADHD toddler's needs and reduce stress for the whole family.", "4 days ago", 1150, 38, 11));
				articles.add(createArticle("Sensory-friendly play ideas", "Avery Kim", "Explore sensory-friendly play activities that engage ADHD toddlers while providing the stimulation they need in a controlled way.", "1 week ago", 1340, 55, 19));
				articles.add(createArticle("Understanding ADHD in toddlers", "Admin", "A comprehensive overview of ADHD symptoms in toddlers, early signs to watch for, and when to seek professional help.", "2 weeks ago", 1890, 73, 31));
				articles.add(createArticle("Communication strategies for ADHD toddlers", "Admin", "Effective communication techniques that work well with toddlers who have ADHD, including visual aids and positive reinforcement.", "3 weeks ago", 1620, 61, 25));
				break;
			case "c3": // Sleep Training 0-6m
				articles.add(createArticle("Newborn sleep windows explained", "Sam Patel", "Understanding newborn sleep patterns and optimal wake windows for better sleep training success with your baby.", "2 days ago", 1100, 48, 16));
				articles.add(createArticle("Creating a gentle bedtime routine", "Sam Patel", "Step-by-step guide to establishing a calming bedtime routine that helps your baby transition to sleep more easily.", "6 days ago", 1250, 41, 13));
				articles.add(createArticle("Swaddling do's and don'ts", "Sam Patel", "Learn the proper techniques for safe swaddling and when to transition away from swaddling as your baby grows.", "1 week ago", 1450, 59, 21));
				articles.add(createArticle("Sleep training methods compared", "Admin", "A detailed comparison of different sleep training methods, their pros and cons, and which might work best for your family.", "2 weeks ago", 1980, 82, 35));
				articles.add(createArticle("Night feeding schedules for newborns", "Admin", "Guidelines for night feeding schedules that support healthy sleep patterns while ensuring your baby gets adequate nutrition.", "3 weeks ago", 1720, 68, 29));
				break;
			case "c4": // Postpartum Health
				articles.add(createArticle("Postpartum recovery essentials", "Jordan Lee", "Essential items and strategies for a smooth postpartum recovery, including physical healing and emotional well-being.", "3 days ago", 1320, 56, 18));
				articles.add(createArticle("Pelvic floor basics", "Jordan Lee", "Understanding pelvic floor health after childbirth and exercises to support recovery and prevent future issues.", "1 week ago", 1680, 71, 26));
				articles.add(createArticle("Nutrition for healing", "Jordan Lee", "Postpartum nutrition guidelines to support healing, energy levels, and breastfeeding if applicable.", "2 weeks ago", 1890, 78, 32));
				articles.add(createArticle("Postpartum depression: Signs and support", "Admin", "Recognizing signs of postpartum depression and available resources for support and treatment.", "3 weeks ago", 2100, 95, 41));
				articles.add(createArticle("Returning to exercise postpartum", "Admin", "Safe guidelines for returning to exercise after childbirth, including timeline and appropriate activities.", "4 weeks ago", 1950, 83, 37));
				break;
			case "c5": // Special Needs Support
				articles.add(createArticle("IEP: where to start", "Avery Kim", "A beginner's guide to Individualized Education Programs (IEPs) and how to advocate for your child's needs.", "2 days ago", 1450, 62, 22));
				articles.add(createArticle("Building your support team", "Avery Kim", "How to build a comprehensive support team for your special needs child, including professionals and community resources.", "5 days ago", 1200, 49, 17));
				articles.add(createArticle("Everyday sensory strategies", "Avery Kim", "Practical sensory strategies you can implement at home to support your child's sensory needs and reduce overwhelm.", "1 week ago", 1580, 67, 24));
				articles.add(createArticle("Understanding different special needs", "Admin", "An overview of various special needs conditions and how they may affect your child's development and daily life.", "2 weeks ago", 2200, 91, 38));
				articles.add(createArticle("Financial resources for special needs families", "Admin", "Information about financial assistance, insurance coverage, and other resources available to special needs families.", "3 weeks ago", 1850, 74, 31));
				break;
			case "c6": // Parent Fitness
				articles.add(createArticle("10-minute workouts for busy parents", "Sam Patel", "Quick, effective workouts that fit into busy parent schedules without requiring special equipment or gym membership.", "1 day ago", 980, 39, 12));
				articles.add(createArticle("Stroller-friendly cardio", "Sam Patel", "Cardio exercises you can do with your stroller, perfect for getting exercise while spending time with your baby.", "4 days ago", 1120, 44, 15));
				articles.add(createArticle("Home stretching routine", "Sam Patel", "Essential stretching exercises for parents to relieve tension and maintain flexibility despite busy schedules.", "1 week ago", 1350, 52, 19));
				articles.add(createArticle("Nutrition for active parents", "Admin", "Nutrition guidelines for parents who want to maintain an active lifestyle while managing family responsibilities.", "2 weeks ago", 1780, 76, 28));
				articles.add(createArticle("Finding time for fitness as a parent", "Admin", "Practical strategies for incorporating fitness into your daily routine as a busy parent.", "3 weeks ago", 1650, 69, 25));
				break;
			default: 
				articles.add(createArticle("Welcome to our community", "Admin", "Welcome to our community! This is a place where parents can share experiences, ask questions, and support each other.", "1 week ago", 500, 25, 8));
		}
		
		return articles;
	}
	
	private java.util.Map<String, Object> createArticle(String title, String author, String excerpt, String publishedDate, int views, int likes, int comments) {
		java.util.Map<String, Object> article = new java.util.HashMap<>();
		article.put("title", title);
		article.put("author", author);
		article.put("excerpt", excerpt);
		article.put("publishedDate", publishedDate);
		article.put("views", views);
		article.put("likes", likes);
		article.put("comments", comments);
		return article;
	}
	
	// Analytics Data Methods
	public java.util.Map<String, Object> getAnalyticsOverview() {
		java.util.Map<String, Object> overview = new java.util.HashMap<>();
		
		// User Metrics
		overview.put("totalUsers", users.size());
		overview.put("activeUsers", users.stream().filter(u -> "Active".equals(u.getStatus())).count());
		overview.put("dau", 156); // Daily Active Users
		overview.put("mau", 1247); // Monthly Active Users
		overview.put("wau", 892); // Weekly Active Users
		
		// Community Metrics
		overview.put("totalCommunities", communities.size());
		overview.put("totalPosts", posts.size());
		overview.put("totalComments", posts.stream().mapToInt(Post::getComments).sum());
		
		// Engagement Metrics
		overview.put("avgSessionDuration", "4m 32s");
		overview.put("bounceRate", "23.4%");
		overview.put("pagesPerSession", 3.2);
		
		// Growth Metrics
		overview.put("userGrowthRate", "+12.3%");
		overview.put("communityGrowthRate", "+8.7%");
		overview.put("postGrowthRate", "+15.2%");
		
		return overview;
	}
	
	public java.util.List<java.util.Map<String, Object>> getUserEvents() {
		java.util.List<java.util.Map<String, Object>> events = new java.util.ArrayList<>();
		
		// Sample user events with Singapore locations - using user IDs for privacy
		events.add(createEvent("User Signup", "New Account Created", "u1", "Marina Bay Community", "2024-01-15 14:30:25", "Mobile"));
		events.add(createEvent("User Login", "Successful Login", "u2", "Orchard Road Parents", "2024-01-15 14:25:12", "Desktop"));
		events.add(createEvent("Post Created", "Sleep Training Tips", "u3", "Sentosa Island Families", "2024-01-15 14:20:45", "Tablet"));
		events.add(createEvent("Comment Added", "ADHD Support Group", "u4", "Chinatown Parenting", "2024-01-15 14:15:33", "Mobile"));
		events.add(createEvent("Community Joined", "Pregnancy Nutrition", "u5", "Little India Community", "2024-01-15 14:10:18", "Desktop"));
		events.add(createEvent("Article Viewed", "Iron-rich foods", "u6", "Clarke Quay Families", "2024-01-15 14:05:52", "Mobile"));
		events.add(createEvent("Profile Updated", "User Profile", "u7", "Bugis Junction Parents", "2024-01-15 14:00:27", "Desktop"));
		events.add(createEvent("Password Changed", "Security Update", "u8", "Tampines Community", "2024-01-15 13:55:41", "Desktop"));
		events.add(createEvent("Article Shared", "Postpartum Health", "u9", "Jurong East Families", "2024-01-15 13:50:15", "Mobile"));
		events.add(createEvent("Community Left", "Parent Fitness", "u10", "Woodlands Parents", "2024-01-15 13:45:38", "Tablet"));
		events.add(createEvent("Search Performed", "sleep training", "u11", "Ang Mo Kio Community", "2024-01-15 13:40:22", "Mobile"));
		events.add(createEvent("Email Verified", "Account Verification", "u12", "Bedok Families", "2024-01-15 13:35:18", "Desktop"));
		events.add(createEvent("Notification Read", "Message Center", "u13", "Clementi Parents", "2024-01-15 13:30:45", "Mobile"));
		events.add(createEvent("Settings Updated", "Privacy Settings", "u1", "Pasir Ris Community", "2024-01-15 13:25:33", "Desktop"));
		events.add(createEvent("Logout", "Session Ended", "u2", "Sengkang Families", "2024-01-15 13:20:12", "Mobile"));
		
		return events;
	}
	
	public java.util.Map<String, Integer> getLocationData() {
		java.util.Map<String, Integer> locations = new java.util.HashMap<>();
		locations.put("Marina Bay Community", 234);
		locations.put("Orchard Road Parents", 189);
		locations.put("Sentosa Island Families", 167);
		locations.put("Chinatown Parenting", 145);
		locations.put("Little India Community", 123);
		locations.put("Clarke Quay Families", 98);
		locations.put("Bugis Junction Parents", 87);
		locations.put("Tampines Community", 76);
		locations.put("Jurong East Families", 65);
		locations.put("Woodlands Parents", 54);
		locations.put("Ang Mo Kio Community", 43);
		locations.put("Bedok Families", 38);
		locations.put("Clementi Parents", 32);
		locations.put("Pasir Ris Community", 28);
		locations.put("Sengkang Families", 25);
		return locations;
	}
	
	public java.util.Map<String, Integer> getDeviceData() {
		java.util.Map<String, Integer> devices = new java.util.HashMap<>();
		devices.put("Mobile", 1247);
		devices.put("Desktop", 892);
		devices.put("Tablet", 456);
		return devices;
	}
	
	public java.util.Map<String, Integer> getEventTypes() {
		java.util.Map<String, Integer> eventTypes = new java.util.HashMap<>();
		eventTypes.put("Page View", 3456);
		eventTypes.put("Post Created", 234);
		eventTypes.put("Comment Added", 567);
		eventTypes.put("Community Joined", 189);
		eventTypes.put("Article Viewed", 1234);
		eventTypes.put("Search Performed", 456);
		eventTypes.put("Profile Updated", 123);
		eventTypes.put("Article Shared", 78);
		eventTypes.put("Community Left", 45);
		return eventTypes;
	}
	
	public java.util.List<java.util.Map<String, Object>> getTopPages() {
		java.util.List<java.util.Map<String, Object>> pages = new java.util.ArrayList<>();
		pages.add(createPageData("Pregnancy Nutrition Community", "/community/c1", 1234, 4.2));
		pages.add(createPageData("ADHD Toddlers Community", "/community/c2", 987, 3.8));
		pages.add(createPageData("Sleep Training Community", "/community/c3", 876, 4.5));
		pages.add(createPageData("Home Page", "/", 2345, 2.1));
		pages.add(createPageData("Postpartum Health Community", "/community/c4", 654, 4.0));
		pages.add(createPageData("Special Needs Support", "/community/c5", 543, 3.9));
		pages.add(createPageData("Parent Fitness Community", "/community/c6", 432, 3.7));
		pages.add(createPageData("Feed Page", "/feed", 765, 2.8));
		return pages;
	}
	
	private java.util.Map<String, Object> createEvent(String eventType, String target, String user, String location, String timestamp, String device) {
		java.util.Map<String, Object> event = new java.util.HashMap<>();
		event.put("eventType", eventType);
		event.put("target", target);
		event.put("user", user);
		event.put("location", location);
		event.put("timestamp", timestamp);
		event.put("device", device);
		return event;
	}
	
	private java.util.Map<String, Object> createPageData(String pageName, String url, int views, double avgTime) {
		java.util.Map<String, Object> page = new java.util.HashMap<>();
		page.put("pageName", pageName);
		page.put("url", url);
		page.put("views", views);
		page.put("avgTime", avgTime);
		return page;
	}
	
	// Additional Rich Analytics Components
	public java.util.Map<String, Object> getTrafficSources() {
		java.util.Map<String, Object> sources = new java.util.HashMap<>();
		sources.put("Direct", 45.2);
		sources.put("Social Media", 28.7);
		sources.put("Search Engines", 18.3);
		sources.put("Referrals", 7.8);
		return sources;
	}
	
	public java.util.Map<String, Integer> getHourlyTraffic() {
		java.util.Map<String, Integer> hourly = new java.util.HashMap<>();
		hourly.put("00:00", 12);
		hourly.put("01:00", 8);
		hourly.put("02:00", 5);
		hourly.put("03:00", 3);
		hourly.put("04:00", 2);
		hourly.put("05:00", 4);
		hourly.put("06:00", 15);
		hourly.put("07:00", 45);
		hourly.put("08:00", 78);
		hourly.put("09:00", 95);
		hourly.put("10:00", 112);
		hourly.put("11:00", 98);
		hourly.put("12:00", 87);
		hourly.put("13:00", 92);
		hourly.put("14:00", 105);
		hourly.put("15:00", 118);
		hourly.put("16:00", 125);
		hourly.put("17:00", 98);
		hourly.put("18:00", 76);
		hourly.put("19:00", 89);
		hourly.put("20:00", 95);
		hourly.put("21:00", 67);
		hourly.put("22:00", 34);
		hourly.put("23:00", 18);
		return hourly;
	}
	
	public java.util.Map<String, Integer> getAgeGroups() {
		java.util.Map<String, Integer> ages = new java.util.HashMap<>();
		ages.put("18-24", 156);
		ages.put("25-34", 423);
		ages.put("35-44", 389);
		ages.put("45-54", 234);
		ages.put("55-64", 98);
		ages.put("65+", 45);
		return ages;
	}
	
	public java.util.Map<String, Integer> getGenderDistribution() {
		java.util.Map<String, Integer> gender = new java.util.HashMap<>();
		gender.put("Female", 892);
		gender.put("Male", 567);
		return gender;
	}
	
	public java.util.List<java.util.Map<String, Object>> getTopReferrers() {
		java.util.List<java.util.Map<String, Object>> referrers = new java.util.ArrayList<>();
		referrers.add(createReferrerData("Facebook", "facebook.com", 234, 12.3));
		referrers.add(createReferrerData("Instagram", "instagram.com", 189, 9.8));
		referrers.add(createReferrerData("Google", "google.com", 167, 8.7));
		referrers.add(createReferrerData("WhatsApp", "whatsapp.com", 145, 7.6));
		referrers.add(createReferrerData("Telegram", "telegram.org", 123, 6.4));
		referrers.add(createReferrerData("TikTok", "tiktok.com", 98, 5.1));
		referrers.add(createReferrerData("LinkedIn", "linkedin.com", 87, 4.5));
		referrers.add(createReferrerData("Twitter", "twitter.com", 76, 4.0));
		return referrers;
	}
	
	public java.util.Map<String, Integer> getSessionDuration() {
		java.util.Map<String, Integer> duration = new java.util.HashMap<>();
		duration.put("0-10 seconds", 234);
		duration.put("11-30 seconds", 456);
		duration.put("31-60 seconds", 567);
		duration.put("1-3 minutes", 789);
		duration.put("3-10 minutes", 892);
		duration.put("10-30 minutes", 456);
		duration.put("30+ minutes", 123);
		return duration;
	}
	
	public java.util.Map<String, Integer> getBrowserData() {
		java.util.Map<String, Integer> browsers = new java.util.HashMap<>();
		browsers.put("Chrome", 1247);
		browsers.put("Safari", 892);
		browsers.put("Firefox", 456);
		browsers.put("Edge", 234);
		browsers.put("Other", 123);
		return browsers;
	}
	
	public java.util.Map<String, Integer> getOperatingSystems() {
		java.util.Map<String, Integer> os = new java.util.HashMap<>();
		os.put("Android", 1247);
		os.put("iOS", 892);
		os.put("Windows", 456);
		os.put("macOS", 234);
		os.put("Other", 123);
		return os;
	}
	
	private java.util.Map<String, Object> createReferrerData(String name, String domain, int visits, double percentage) {
		java.util.Map<String, Object> referrer = new java.util.HashMap<>();
		referrer.put("name", name);
		referrer.put("domain", domain);
		referrer.put("visits", visits);
		referrer.put("percentage", percentage);
		return referrer;
	}
	
	// Content Generation and Social Media Methods
	public java.util.Map<String, Object> generateContent(String contentType, String topic, String communityId, String aiTool) {
		java.util.Map<String, Object> content = new java.util.HashMap<>();
		content.put("id", "content_" + System.currentTimeMillis());
		content.put("type", contentType);
		content.put("topic", topic);
		content.put("communityId", communityId);
		content.put("aiTool", aiTool);
		content.put("generatedAt", java.time.LocalDateTime.now().toString());
		content.put("status", "Generated");
		
		// Simulate AI-generated content based on type and tool
		switch (contentType.toLowerCase()) {
			case "article":
				content.put("title", generateArticleTitle(topic, aiTool));
				content.put("content", generateArticleContent(topic, aiTool));
				content.put("wordCount", getWordCountForTool(aiTool));
				content.put("readingTime", getReadingTimeForTool(aiTool));
				break;
			case "video":
				content.put("title", generateVideoTitle(topic, aiTool));
				content.put("script", generateVideoScript(topic, aiTool));
				content.put("duration", getDurationForTool(aiTool));
				content.put("hashtags", generateHashtags(topic));
				break;
			case "photo":
				content.put("title", generatePhotoTitle(topic, aiTool));
				content.put("caption", generatePhotoCaption(topic, aiTool));
				content.put("hashtags", generateHashtags(topic));
				content.put("altText", generateAltText(topic));
				break;
		}
		
		// Add to recent generated content list
		java.util.Map<String, Object> recentItem = createContentData(
			content.get("id").toString(),
			contentType,
			content.get("title").toString(),
			getCommunityNameById(communityId),
			"Generated",
			"Just now"
		);
		recentGeneratedContent.add(0, recentItem); // Add to beginning of list
		
		// Keep only the last 10 items
		if (recentGeneratedContent.size() > 10) {
			recentGeneratedContent.remove(recentGeneratedContent.size() - 1);
		}
		
		return content;
	}
	
	public java.util.List<java.util.Map<String, Object>> getSocialMediaPlatforms() {
		java.util.List<java.util.Map<String, Object>> platforms = new java.util.ArrayList<>();
		platforms.add(createPlatformData("TikTok", "tiktok.com", "Short-form videos", "1.2B", "#1B5E20", "🎵"));
		platforms.add(createPlatformData("Facebook", "facebook.com", "Social networking", "3.0B", "#1877F2", "📘"));
		platforms.add(createPlatformData("Instagram", "instagram.com", "Photo & video sharing", "2.0B", "#E4405F", "📷"));
		platforms.add(createPlatformData("X", "x.com", "Microblogging", "450M", "#000000", "𝕏"));
		platforms.add(createPlatformData("LinkedIn", "linkedin.com", "Professional networking", "900M", "#0077B5", "💼"));
		platforms.add(createPlatformData("YouTube", "youtube.com", "Video sharing", "2.7B", "#FF0000", "📺"));
		return platforms;
	}
	
	public java.util.List<java.util.Map<String, Object>> getContentTemplates() {
		java.util.List<java.util.Map<String, Object>> templates = new java.util.ArrayList<>();
		templates.add(createTemplateData("Parenting Tips", "Educational content for parents", "article"));
		templates.add(createTemplateData("Quick Tutorial", "Step-by-step guides", "video"));
		templates.add(createTemplateData("Motivational Quote", "Inspirational messages", "photo"));
		templates.add(createTemplateData("Community Highlight", "Showcasing community members", "article"));
		templates.add(createTemplateData("Behind the Scenes", "Day-in-the-life content", "video"));
		templates.add(createTemplateData("Success Story", "Member achievements", "photo"));
		return templates;
	}
	
	public java.util.List<java.util.Map<String, Object>> getRecentGeneratedContent() {
		// If no recent content, return some sample data
		if (recentGeneratedContent.isEmpty()) {
			java.util.List<java.util.Map<String, Object>> sampleContent = new java.util.ArrayList<>();
			sampleContent.add(createContentData("content_001", "article", "5 Tips for Better Sleep Training", "Sleep Training 0-6m", "Generated", "2 hours ago"));
			sampleContent.add(createContentData("content_002", "video", "Quick Morning Routine for Busy Parents", "Parent Fitness", "Posted to TikTok", "5 hours ago"));
			sampleContent.add(createContentData("content_003", "photo", "Motivational Monday Quote", "ADHD Toddlers", "Posted to Instagram", "1 day ago"));
			sampleContent.add(createContentData("content_004", "article", "Nutrition Guide for Expecting Mothers", "Pregnancy Nutrition", "Draft", "2 days ago"));
			sampleContent.add(createContentData("content_005", "video", "Community Success Stories", "Special Needs Support", "Posted to Facebook", "3 days ago"));
			return sampleContent;
		}
		return new java.util.ArrayList<>(recentGeneratedContent);
	}
	
	// AI Content Generation Methods
	private String generateArticleTitle(String topic) {
		String[] templates = {
			"5 Essential Tips for " + topic,
			"The Complete Guide to " + topic,
			"How to Master " + topic + " in 2024",
			"Everything You Need to Know About " + topic,
			"Expert Advice on " + topic
		};
		return templates[new java.util.Random().nextInt(templates.length)];
	}
	
	private String generateArticleContent(String topic) {
		return "Introduction to " + topic + "...\n\n" +
			   "Key Points:\n" +
			   "1. Understanding the basics\n" +
			   "2. Practical applications\n" +
			   "3. Common challenges and solutions\n" +
			   "4. Expert recommendations\n\n" +
			   "Conclusion: " + topic + " is an important aspect of parenting that requires patience and understanding.";
	}
	
	private String generateVideoTitle(String topic) {
		String[] templates = {
			"Quick Tips: " + topic,
			"How to Handle " + topic,
			"Parenting Hack: " + topic,
			"60-Second Guide to " + topic,
			"Real Talk About " + topic
		};
		return templates[new java.util.Random().nextInt(templates.length)];
	}
	
	private String generateVideoScript(String topic) {
		return "Hook: Did you know that " + topic + " can be easier than you think?\n\n" +
			   "Main Content:\n" +
			   "- Point 1: Start with the basics\n" +
			   "- Point 2: Common mistakes to avoid\n" +
			   "- Point 3: Pro tips from experts\n\n" +
			   "Call to Action: Follow for more parenting tips!";
	}
	
	private String generatePhotoTitle(String topic) {
		String[] templates = {
			"Inspirational Quote: " + topic,
			"Motivational Monday: " + topic,
			"Parenting Wisdom: " + topic,
			"Community Spotlight: " + topic,
			"Success Story: " + topic
		};
		return templates[new java.util.Random().nextInt(templates.length)];
	}
	
	private String generatePhotoCaption(String topic) {
		return "💡 " + topic + " reminder for all parents!\n\n" +
			   "Remember: Every parent's journey is unique. " +
			   "Share your experiences in the comments below! 👇\n\n" +
			   "#Parenting #Community #Support";
	}
	
	// AI Content Generation Methods
	private String generateArticleTitle(String topic, String aiTool) {
		String[] templates;
		switch (aiTool.toLowerCase()) {
			case "chatgpt":
				templates = new String[]{
					"5 Essential Tips for " + topic,
					"The Complete Guide to " + topic,
					"How to Master " + topic + " in 2024",
					"Everything You Need to Know About " + topic,
					"Expert Advice on " + topic
				};
				break;
			case "claude":
				templates = new String[]{
					"Understanding " + topic + ": A Comprehensive Approach",
					"The Science Behind " + topic,
					"Practical Strategies for " + topic,
					"Navigating " + topic + " Successfully",
					"Evidence-Based " + topic + " Methods"
				};
				break;
			case "nano-banana":
				templates = new String[]{
					"Quick " + topic + " Tips",
					"Simple " + topic + " Guide",
					"Easy " + topic + " Solutions",
					"Fast " + topic + " Methods",
					"Basic " + topic + " Advice"
				};
				break;
			case "sora":
				templates = new String[]{
					"Visual Guide to " + topic,
					"Step-by-Step " + topic + " Tutorial",
					"Video: " + topic + " Explained",
					"Watch and Learn: " + topic,
					"Visual " + topic + " Demonstration"
				};
				break;
			default:
				templates = new String[]{"Guide to " + topic};
		}
		return templates[new java.util.Random().nextInt(templates.length)];
	}
	
	private String generateArticleContent(String topic, String aiTool) {
		String baseContent = "Introduction to " + topic + "...\n\n";
		String keyPoints = "Key Points:\n1. Understanding the basics\n2. Practical applications\n3. Common challenges and solutions\n4. Expert recommendations\n\n";
		String conclusion = "Conclusion: " + topic + " is an important aspect of parenting that requires patience and understanding.";
		
		switch (aiTool.toLowerCase()) {
			case "chatgpt":
				return baseContent + keyPoints + conclusion + "\n\nGenerated by ChatGPT - OpenAI's conversational AI.";
			case "claude":
				return baseContent + keyPoints + conclusion + "\n\nGenerated by Claude - Anthropic's AI assistant with focus on helpfulness and safety.";
			case "nano-banana":
				return baseContent + "Quick Tips:\n- Start simple\n- Be consistent\n- Ask for help\n\n" + conclusion + "\n\nGenerated by Nano Banana - Lightweight AI for quick content.";
			case "sora":
				return baseContent + "Visual Elements:\n- Step-by-step images\n- Video demonstrations\n- Interactive guides\n\n" + conclusion + "\n\nGenerated by Sora - OpenAI's video generation AI.";
			default:
				return baseContent + keyPoints + conclusion;
		}
	}
	
	private String generateVideoTitle(String topic, String aiTool) {
		String[] templates;
		switch (aiTool.toLowerCase()) {
			case "chatgpt":
				templates = new String[]{"Quick Tips: " + topic, "How to Handle " + topic, "Parenting Hack: " + topic, "60-Second Guide to " + topic, "Real Talk About " + topic};
				break;
			case "claude":
				templates = new String[]{"Understanding " + topic, "The Science of " + topic, "Evidence-Based " + topic, "Comprehensive " + topic + " Guide", "Expert Insights on " + topic};
				break;
			case "nano-banana":
				templates = new String[]{"Simple " + topic, "Easy " + topic + " Tips", "Quick " + topic + " Fix", "Basic " + topic, "Fast " + topic + " Guide"};
				break;
			case "sora":
				templates = new String[]{"Visual " + topic + " Tutorial", "Watch: " + topic + " in Action", "Step-by-Step " + topic, "Video Guide: " + topic, "See " + topic + " Work"};
				break;
			default:
				templates = new String[]{"Guide to " + topic};
		}
		return templates[new java.util.Random().nextInt(templates.length)];
	}
	
	private String generateVideoScript(String topic, String aiTool) {
		String hook = "Hook: Did you know that " + topic + " can be easier than you think?\n\n";
		String mainContent = "Main Content:\n- Point 1: Start with the basics\n- Point 2: Common mistakes to avoid\n- Point 3: Pro tips from experts\n\n";
		String callToAction = "Call to Action: Follow for more parenting tips!";
		
		switch (aiTool.toLowerCase()) {
			case "chatgpt":
				return hook + mainContent + callToAction + "\n\n[Generated by ChatGPT]";
			case "claude":
				return hook + mainContent + callToAction + "\n\n[Generated by Claude - Focus on safety and helpfulness]";
			case "nano-banana":
				return hook + "Quick Points:\n- Keep it simple\n- Stay consistent\n- Ask questions\n\n" + callToAction + "\n\n[Generated by Nano Banana]";
			case "sora":
				return hook + mainContent + callToAction + "\n\n[Video generated by Sora - OpenAI's video AI]";
			default:
				return hook + mainContent + callToAction;
		}
	}
	
	private String generatePhotoTitle(String topic, String aiTool) {
		String[] templates;
		switch (aiTool.toLowerCase()) {
			case "chatgpt":
				templates = new String[]{"Inspirational Quote: " + topic, "Motivational Monday: " + topic, "Parenting Wisdom: " + topic, "Community Spotlight: " + topic, "Success Story: " + topic};
				break;
			case "claude":
				templates = new String[]{"Thoughtful Reflection: " + topic, "Mindful Parenting: " + topic, "Compassionate " + topic, "Understanding " + topic, "Gentle " + topic};
				break;
			case "nano-banana":
				templates = new String[]{"Simple " + topic, "Easy " + topic + " Reminder", "Quick " + topic + " Tip", "Basic " + topic, "Fast " + topic};
				break;
			case "sora":
				templates = new String[]{"Visual " + topic, "See " + topic + " in Action", "Watch " + topic, "Visual Guide: " + topic, "Step-by-Step " + topic};
				break;
			default:
				templates = new String[]{"Quote about " + topic};
		}
		return templates[new java.util.Random().nextInt(templates.length)];
	}
	
	private String generatePhotoCaption(String topic, String aiTool) {
		String baseCaption = "💡 " + topic + " reminder for all parents!\n\nRemember: Every parent's journey is unique. Share your experiences in the comments below! 👇\n\n";
		String hashtags = "#Parenting #Community #Support";
		
		switch (aiTool.toLowerCase()) {
			case "chatgpt":
				return baseCaption + hashtags + "\n\n🤖 Generated by ChatGPT";
			case "claude":
				return baseCaption + hashtags + "\n\n🧠 Generated by Claude";
			case "nano-banana":
				return baseCaption + hashtags + "\n\n🍌 Generated by Nano Banana";
			case "sora":
				return baseCaption + hashtags + "\n\n🎬 Generated by Sora";
			default:
				return baseCaption + hashtags;
		}
	}
	
	private int getWordCountForTool(String aiTool) {
		switch (aiTool.toLowerCase()) {
			case "chatgpt": return 800;
			case "claude": return 1000;
			case "nano-banana": return 400;
			case "sora": return 600;
			default: return 800;
		}
	}
	
	private String getReadingTimeForTool(String aiTool) {
		switch (aiTool.toLowerCase()) {
			case "chatgpt": return "4 min read";
			case "claude": return "5 min read";
			case "nano-banana": return "2 min read";
			case "sora": return "3 min read";
			default: return "4 min read";
		}
	}
	
	private String getDurationForTool(String aiTool) {
		switch (aiTool.toLowerCase()) {
			case "chatgpt": return "2:30";
			case "claude": return "3:00";
			case "nano-banana": return "1:30";
			case "sora": return "2:00";
			default: return "2:30";
		}
	}
	
	private String generateHashtags(String topic) {
		String[] hashtags = {
			"#Parenting", "#Community", "#Support", "#Tips", "#Family",
			"#MomLife", "#DadLife", "#ParentingTips", "#CommunitySupport", "#ParentingJourney"
		};
		java.util.List<String> selected = new java.util.ArrayList<>();
		java.util.Random random = new java.util.Random();
		for (int i = 0; i < 5; i++) {
			selected.add(hashtags[random.nextInt(hashtags.length)]);
		}
		return String.join(" ", selected);
	}
	
	private String generateAltText(String topic) {
		return "Inspirational quote about " + topic + " with beautiful typography and parenting theme";
	}
	
	private String getCommunityNameById(String communityId) {
		return communities.stream()
			.filter(c -> c.getId().equals(communityId))
			.findFirst()
			.map(c -> c.getName())
			.orElse("Unknown Community");
	}
	
	private java.util.Map<String, Object> createPlatformData(String name, String url, String description, String users, String color, String icon) {
		java.util.Map<String, Object> platform = new java.util.HashMap<>();
		platform.put("name", name);
		platform.put("url", url);
		platform.put("description", description);
		platform.put("users", users);
		platform.put("color", color);
		platform.put("icon", icon);
		return platform;
	}
	
	private java.util.Map<String, Object> createTemplateData(String name, String description, String type) {
		java.util.Map<String, Object> template = new java.util.HashMap<>();
		template.put("name", name);
		template.put("description", description);
		template.put("type", type);
		return template;
	}
	
	private java.util.Map<String, Object> createContentData(String id, String type, String title, String community, String status, String timeAgo) {
		java.util.Map<String, Object> content = new java.util.HashMap<>();
		content.put("id", id);
		content.put("type", type);
		content.put("title", title);
		content.put("community", community);
		content.put("status", status);
		content.put("timeAgo", timeAgo);
		return content;
	}
}
