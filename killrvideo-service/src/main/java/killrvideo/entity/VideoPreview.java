package killrvideo.entity;

import java.util.Date;

public class VideoPreview {

	private String name;
	private String videoid;
	private String userid;
	private Date addedDate;
	private String previewImageLocation;

	public Date getAddedDate() {
		return addedDate;
	}

	public String getName() {
		return name;
	}

	public String getPreviewImageLocation() {
		return previewImageLocation;
	}

	public String getUserId() {
		return userid;
	}

	public String getVideoId() {
		return videoid;

	}

	private static final VideoPreview DEFAULT_INSTANCE;

	static {
		DEFAULT_INSTANCE = new VideoPreview();
	}

	public static VideoPreview getDefaultInstance() {
		return DEFAULT_INSTANCE;
	}

	public static final class Builder {
		private String name;
		private String videoid;
		private String userid;
		private Date addedDate;
		private String previewImageLocation;
		
		public Builder mergeFrom(VideoPreview prototype) {
			if (prototype == VideoPreview.getDefaultInstance())
				return this;
			Builder newBuilder = new Builder();
			newBuilder.name = prototype.name;
			newBuilder.videoid = prototype.videoid;
			newBuilder.userid = prototype.userid;
			newBuilder.addedDate = prototype.addedDate;
			newBuilder.previewImageLocation = prototype.previewImageLocation;
			return newBuilder;
		}

		public Builder setAddedDate(Date addedDate) {
			this.addedDate = addedDate;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setPreviewImageLocation(String previewImageLocation) {
			this.previewImageLocation = previewImageLocation;
			return this;
		}

		public Builder setUserId(String userId) {
			this.userid = userId;
			return this;
		}

		public Builder setVideoId(String videoid) {
			this.videoid = videoid;
			return this;
		}

		public VideoPreview build() {
			VideoPreview newInstance = new VideoPreview();
			newInstance.name = this.name;
			newInstance.videoid = this.videoid;
			newInstance.userid = this.userid;
			newInstance.addedDate = this.addedDate;
			newInstance.previewImageLocation = this.previewImageLocation;
			return newInstance;
		}

	}

	public Builder newBuilderForType() {
		return newBuilder();
	}

	public static Builder newBuilder() {
		return DEFAULT_INSTANCE.toBuilder();
	}

	public static Builder newBuilder(VideoPreview prototype) {
		return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
	}

	public Builder toBuilder() {
		return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
	}

}
