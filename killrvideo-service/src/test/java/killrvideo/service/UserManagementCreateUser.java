
package killrvideo.service;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import killrvideo.configuration.KillrvideoConfigurationTest;
import killrvideo.entity.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { KillrvideoConfigurationTest.class })
public class UserManagementCreateUser {
	@Inject
	UserManagementService userManagementService;

	@Test
	public void createUser() throws InterruptedException, ExecutionException {
		
		User profile = new User();
		profile.setEmail("chandraoops@gmail.com");
		profile.setFirstName("chandra");
		profile.setLastName("jayaram");
		profile.setPassword("testPassword");
		String userId = userManagementService.createUser("chandraoops@gmail.com","testPassword");
		profile.setUserId(userId);
		userManagementService.updateUser(profile);
	}
	
	
}
