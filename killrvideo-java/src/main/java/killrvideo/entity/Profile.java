package killrvideo.entity;

import java.util.Date;
import java.util.UUID;

public class Profile {
	private UUID userid;

	private String firstname;

	private String lastname;

	private String email;

	private Date createdAt;

	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Profile() {
	}

	public Profile(UUID userid, String firstname, String lastname, String email, Date createdAt) {
		this.userid = userid;
		this.firstname = firstname;
		this.lastname = lastname;
		this.email = email;
		this.createdAt = createdAt;
	}

	public UUID getUserid() {
		return userid;
	}

	public void setUserid(UUID userid) {
		this.userid = userid;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

}
