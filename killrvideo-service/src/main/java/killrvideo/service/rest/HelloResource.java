package killrvideo.service.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;

@Component	
@Path("/api/hello")
public class HelloResource {
	
	@GET
	public Response sayHello() {
		return Response.ok()
				.entity("Hello World")
				.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE, OPTIONS")
				.header("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token").build();
	}
}
