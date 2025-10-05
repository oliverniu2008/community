package com.superm.community.model;

import java.time.LocalDate;

public class User {
	private String id;
	private String email;
	private String name;
	private String fullName;
	private boolean leader;
	private String status; // Active, Suspended
	private LocalDate joinDate;

	public User(String id, String email, String name, String fullName, boolean leader, String status, LocalDate joinDate) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.fullName = fullName;
		this.leader = leader;
		this.status = status;
		this.joinDate = joinDate;
	}

	public String getId() { return id; }
	public String getEmail() { return email; }
	public String getName() { return name; }
	public String getFullName() { return fullName; }
	public boolean isLeader() { return leader; }
	public String getStatus() { return status; }
	public LocalDate getJoinDate() { return joinDate; }
}
