package killrvideo.service.rest;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.springframework.stereotype.Component;

import killrvideo.entity.User;
import killrvideo.entity.UserCredentials;
import killrvideo.service.UserManagementService;

@Component	
@Path("/api/users")
public class UserResource {
	@Inject
	private UserManagementService userMgmtService;
	
	@GET
	public String authenticate(String userName, String password) {
		return "Hello";
	}
	
	@Path("/{userId}")
	@GET
	public User getProfile(String userId) {
		try {
			return userMgmtService.getUser(userId);
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;	
	}
}
