package socialmedia.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Post {
	private static final AtomicInteger idGenerator = new AtomicInteger(1);
	private int userId;
	private final int id;
	private String content;
	private PostType postType;
	private LocalDateTime createdAt;
	private Metadata metadata;

	public Post(int userId, PostType postType,Metadata metadata,String content) {
		this.userId = userId;
		this.id = idGenerator.getAndIncrement();
		this.postType = postType;
		this.createdAt = LocalDateTime.now();
		this.metadata = metadata;
		this.content=content;
	}

	public int getUserId() {
		return userId;
	}


	public int getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public PostType getPostType() {
		return postType;
	}

	public void setPostType(PostType postType) {
		this.postType = postType;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
