
package killrvideo.service;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import killrvideo.configuration.KillrvideoConfigurationTest;
import killrvideo.entity.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={KillrvideoConfigurationTest.class})
public class UserManagementServiceTest {
	@Inject
	UserManagementService userManagementService;
	
	
	@Test
	public void testContextInitialization(){
		User profile = new User();
		profile.setEmail("chandraoops@rediffmail.com");
		profile.setFirstName("chandra");
		profile.setLastName("jayaram");
		profile.setPassword("testPassword");
		String userId = userManagementService.createUser(profile);
		System.out.println(userId);
	}
}
