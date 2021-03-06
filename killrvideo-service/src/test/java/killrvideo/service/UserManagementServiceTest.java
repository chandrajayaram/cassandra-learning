
package killrvideo.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import killrvideo.configuration.KillrvideoConfigurationTest;
import killrvideo.entity.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { KillrvideoConfigurationTest.class })
public class UserManagementServiceTest {
	@Inject
	UserManagementService userManagementService;

	String userId;

	@Before
	public void setUp() throws InterruptedException, ExecutionException {
		User profile = new User();
		profile.setEmail("chandraoops@testmail.com");
		profile.setFirstName("chandra");
		profile.setLastName("jayaram");
		profile.setPassword("testPassword");
		this.userId = userManagementService.createUser("chandraoops@testmail1.com","testPassword");
		profile.setUserId(userId);
		userManagementService.updateUser(profile);
	}
	
	@After
	public void tearDown() throws InterruptedException, ExecutionException {
		boolean wasDeleted = userManagementService.deleteUser(userId,"chandraoops@testmail.com");
		assertEquals(wasDeleted, true);
	}

	@Test
	public void testGetUser() throws InterruptedException, ExecutionException {
		User createdUser = userManagementService.getUser(userId);
		assertNotNull(createdUser);
	}

	@Test
	public void testVerifyCredentials() throws InterruptedException, ExecutionException {
		String successUserId = userManagementService.verifyCredentials("chandraoops@testmail.com", "testPassword");
		assertEquals(successUserId, userId);
	}
}
