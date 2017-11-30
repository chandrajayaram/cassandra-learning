package killrvideo.service.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.springframework.stereotype.Component;

@Component	
@Path("/api/users")
public class UserResource {
	@GET
	public String message() {
		return "Hello";
	}
}
