package killrvideo.entity;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class Video extends AbstractVideo{

    private UUID videoId;
    private UUID userId;
    private String description;
    private String location;
    private VideoLocationType locationType;
    private Set<String> tags;
    private Date addedDate;

    public Video() {
    }

    public Video(UUID videoid, UUID userid, String name, String description, VideoLocationType locationType, Set<String> tags, Date addedDate) {
        this.videoId = videoid;
        this.userId = userid;
        this.name = name;
        this.description = description;
        this.locationType = locationType;
        this.tags = tags;
        this.addedDate = addedDate;
    }

    public Video(UUID videoid, UUID userid, String name, String description, String location, VideoLocationType locationType, String previewImageLocation, Set<String> tags, Date addedDate) {
        this.videoId = videoid;
        this.userId = userid;
        this.name = name;
        this.description = description;
        this.location = location;
        this.locationType = locationType;
        this.previewImageLocation = previewImageLocation;
        this.tags = tags;
        this.addedDate = addedDate;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoid) {
        this.videoId = videoid;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userid) {
        this.userId = userid;
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

    public VideoLocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(VideoLocationType locationType) {
        this.locationType = locationType;
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

    public VideoPreview toVideoPreview() {
        return VideoPreview.newBuilder()
                .setAddedDate(addedDate)
                .setName(name)
                .setPreviewImageLocation(previewImageLocation)
                .setUserId(userId.toString())
                .setVideoId(videoId.toString())
                .build();
    }
}
