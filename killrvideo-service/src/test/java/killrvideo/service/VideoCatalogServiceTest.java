
package killrvideo.service;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import killrvideo.configuration.KillrvideoConfigurationTest;
import killrvideo.entity.UserVideoPreviews;
import killrvideo.entity.VideoPreview;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { KillrvideoConfigurationTest.class })
public class VideoCatalogServiceTest {
	@Inject
	VideoCatalogService  videoCatalogService ;

	@Test
	public void testGetUserVideoPreviews() throws InterruptedException, ExecutionException {

		String userId= "9761d3d7-7fbd-4269-9988-6cfd4e188678";
		String startingVideoId = null;
		Date startingAddedDate = null;
		String pagingState = null;
		int pageSize = 5;
		UserVideoPreviews userVideoPreviews = videoCatalogService.getUserVideoPreviews(userId, startingVideoId, startingAddedDate, pagingState, pageSize);
		print(userVideoPreviews);
		VideoPreview userVideo = userVideoPreviews.getPreviewList().get(userVideoPreviews.getPreviewList().size()-1);
		userVideoPreviews = videoCatalogService.getUserVideoPreviews(userId, startingVideoId, startingAddedDate, userVideoPreviews.getPagingState(), pageSize);
		print(userVideoPreviews);
		
		userVideo = userVideoPreviews.getPreviewList().get(userVideoPreviews.getPreviewList().size()-1);
		userVideoPreviews = videoCatalogService.getUserVideoPreviews(userId, startingVideoId, startingAddedDate, userVideoPreviews.getPagingState(), pageSize);
		print(userVideoPreviews);

		

	}

	private void print(UserVideoPreviews userVideoPreviews) {
		for( VideoPreview videoPreview : userVideoPreviews.getPreviewList()) {
			System.out.println(videoPreview.getAddedDate() + " " +videoPreview.getVideoId());
		}
		System.out.println();
	}


}
