
package killrvideo.service;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import killrvideo.configuration.KillrvideoConfigurationTest;
import killrvideo.entity.Profile;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={KillrvideoConfigurationTest.class})
public class UserManagementServiceTest {
	@Inject
	UserManagementService userManagementService;
	
	@Inject
	UserMgmtService ugmtService;
	
	
	@Test
	public void testContextInitialization(){
		Profile profile = new Profile();
		profile.setEmail("chandraoops@rediffmail.com");
		profile.setFirstname("chandra");
		profile.setLastname("jayaram");
		profile.setPassword("testPassword");
		String userId = ugmtService.createUser(profile);
		System.out.println(userId);
	}
}
