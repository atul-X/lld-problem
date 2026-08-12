package socialmedia.model;

public class Metadata {
	private final String s3url;
	private final String fileName;
	private final String uploadedAt;

	public Metadata(String s3url, String fileName, String uploadedAt) {
		this.s3url = s3url;
		this.fileName = fileName;
		this.uploadedAt = uploadedAt;
	}

	public String getS3url() {
		return s3url;
	}

	public String getFileName() {
		return fileName;
	}

	public String getUploadedAt() {
		return uploadedAt;
	}
}
