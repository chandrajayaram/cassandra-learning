package killrvideo;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;


@ComponentScan
@EnableAutoConfiguration
public class KillrVideoServer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(KillrVideoServer.class);
        app.setWebEnvironment(false);
        app.setBannerMode(Mode.CONSOLE);

        app.run(args);
    }
}