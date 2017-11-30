package killrvideo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;


@SpringBootApplication
public class KillrVideoServer extends SpringBootServletInitializer {

    public static void main(String[] args) {
    	new KillrVideoServer()
		.configure(new SpringApplicationBuilder(KillrVideoServer.class))
		.run(args);
    	
    }
}