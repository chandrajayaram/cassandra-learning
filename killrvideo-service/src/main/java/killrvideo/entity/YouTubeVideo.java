package killrvideo.entity;

import java.util.List;

public class YouTubeVideo {
	private String youTubeVideoId;
	private String name;
	private String description;
	private String videoId;
	private String userId;
	private List<String> tagsList;
	
	public List<String> getTagsList() {
		return tagsList;
	}
	public void setTagsList(List<String> tagsList) {
		this.tagsList = tagsList;
	}
	public String getYouTubeVideoId() {
		return youTubeVideoId;
	}
	public void setYouTubeVideoId(String youTubeVideoId) {
		this.youTubeVideoId = youTubeVideoId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getVideoId() {
		return videoId;
	}
	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	
	
    
    


}
