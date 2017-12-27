package killrvideo.events;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class YouTubeVideoAdded {
	private Date addedDate;
	private String name;
    private String description;
    private String location;
    private String previewImageLocation;
    private Set<String> tags = new HashSet<>();
    private Date timestamp;
	private String videoId;
	private String userId;
	
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

	
    
	 public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
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
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getPreviewImageLocation() {
		return previewImageLocation;
	}
	public void setPreviewImageLocation(String previewImageLocation) {
		this.previewImageLocation = previewImageLocation;
	}
	public Set<String> getTags() {
		return tags;
	}
	public void setTags(Set<String> tags) {
		this.tags = tags;
	}
	public Date getAddedDate() {
		return addedDate;
	}
	public void setAddedDate(Date addedDate) {
		this.addedDate = addedDate;
	}
	public void addAllTags(HashSet<String> newHashSet) {
		tags.addAll(newHashSet);
	}
	
     
     
}
