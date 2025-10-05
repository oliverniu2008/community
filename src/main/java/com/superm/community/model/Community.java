package com.superm.community.model;

import java.time.LocalDate;

public class Community {
	private String id;
	private String name;
	private String description;
	private String leaderName;
	private int memberCount;
	private LocalDate created;

	public Community(String id, String name, String description, String leaderName, int memberCount, LocalDate created) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.leaderName = leaderName;
		this.memberCount = memberCount;
		this.created = created;
	}

	public String getId() { return id; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public String getLeaderName() { return leaderName; }
	public int getMemberCount() { return memberCount; }
	public LocalDate getCreated() { return created; }
}
