package killrvideo.entity;

import javax.validation.constraints.NotNull;

import com.datastax.driver.mapping.annotations.Column;

import killrvideo.NotEmpty;


public class UserCredentials {

    private String email;

    @NotEmpty
    @Column(name = "password")
    private String password;

    @NotNull
    private String userid;

    public UserCredentials() {
    }

    public UserCredentials(String email, String password, String userid) {
        this.email = email;
        this.password = password;
        this.userid = userid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
