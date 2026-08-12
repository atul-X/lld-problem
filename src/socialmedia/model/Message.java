package socialmedia.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Message {
	private static final AtomicInteger idGenerator = new AtomicInteger(1);

	private final int id;
	private final int senderId;
	private final int receiverId;
	private final String content;
	private final LocalDateTime sentAt;

	public Message(int senderId, int receiverId, String content) {
		this.id = idGenerator.getAndIncrement();
		this.senderId = senderId;
		this.receiverId = receiverId;
		this.content = content;
		this.sentAt = LocalDateTime.now();
	}

	public int getId() {
		return id;
	}

	public int getSenderId() {
		return senderId;
	}

	public int getReceiverId() {
		return receiverId;
	}

	public String getContent() {
		return content;
	}

	public LocalDateTime getSentAt() {
		return sentAt;
	}
}
