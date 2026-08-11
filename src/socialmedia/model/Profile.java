package socialmedia.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Profile {
	private static final AtomicInteger idGenerator = new AtomicInteger(1);

	private final int id;
	private String username;
	private String name;
	private String dob;
	private String mobileno;
	private final LocalDateTime createdAt;

	public Profile( String username, String name, String dob, String mobileno) {
		this.id = idGenerator.getAndIncrement();
		this.username = username;
		this.name = name;
		this.dob = dob;
		this.mobileno = mobileno;
		this.createdAt = LocalDateTime.now();
	}

	public int getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public String getMobileno() {
		return mobileno;
	}

	public void setMobileno(String mobileno) {
		this.mobileno = mobileno;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
