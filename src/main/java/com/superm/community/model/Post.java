package com.superm.community.model;

import java.time.LocalDateTime;

public class Post {
	private String id;
	private String communityId;
	private String authorName;
	private String content;
	private String mediaUrl; // image/video optional
	private LocalDateTime createdAt;
	private int likes;
	private int comments;

	public Post(String id, String communityId, String authorName, String content, String mediaUrl, LocalDateTime createdAt, int likes, int comments) {
		this.id = id;
		this.communityId = communityId;
		this.authorName = authorName;
		this.content = content;
		this.mediaUrl = mediaUrl;
		this.createdAt = createdAt;
		this.likes = likes;
		this.comments = comments;
	}

	public String getId() { return id; }
	public String getCommunityId() { return communityId; }
	public String getAuthorName() { return authorName; }
	public String getContent() { return content; }
	public String getMediaUrl() { return mediaUrl; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public int getLikes() { return likes; }
	public int getComments() { return comments; }
}
