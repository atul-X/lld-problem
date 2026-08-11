package socialmedia.model;

import java.time.LocalDateTime;

public class ProfileRequest {
    private String username;
    private String name;
    private String dob;
    private String mobileno;

    public ProfileRequest(String username, String name, String dob, String mobileno) {
        this.username = username;
        this.name = name;
        this.dob = dob;
        this.mobileno = mobileno;
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
}
