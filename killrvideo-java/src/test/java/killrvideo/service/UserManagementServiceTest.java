
package killrvideo.service;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import killrvideo.configuration.DSEConfiguration;
import killrvideo.configuration.KillrVideoConfiguration;
import killrvideo.configuration.MapperConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={KillrVideoConfiguration.class,DSEConfiguration.class,MapperConfiguration.class})
@ComponentScan("killrvideo")
@EnableAutoConfiguration
public class UserManagementServiceTest {
	@Inject
	UserManagementService userManagementService;
	
	@Test
	public void testContextInitialization(){
		String userId = userManagementService.createUser("chandra","jayaram","chandra.jayara.c@gmail.com","testPassword");
		System.out.println(userId);
	}
}
