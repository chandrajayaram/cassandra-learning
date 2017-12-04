package killrvideo.service.rest;

import java.util.Base64;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import killrvideo.entity.User;
import killrvideo.service.UserManagementService;

@Component	
@Path("/api/users")
public class UserResource {
	@Inject
	private UserManagementService userMgmtService;
	
	@GET
	public String authenticate(@HeaderParam(value = "Authorization") String authenticationHeader ) {
		System.out.println(authenticationHeader);
		String credentials = authenticationHeader.split(" ")[1];
		String decodedCredentials = new String(Base64.getDecoder().decode(credentials));
		String uNamePassword[] = decodedCredentials.split(":");
		
		return userMgmtService.verifyCredentials(uNamePassword[0], uNamePassword[1]);
	}
	
	@Path("/{userId}")
	@Produces("application/json")
	@GET
	public User getProfile(@PathParam(value = "userId") String userId) {
		return userMgmtService.getUser(userId);
	}
	
	@Path("/{userId}")
	@POST
	public void updateProfile(User user ) {
		userMgmtService.updateUser(user);
	}
	
}
