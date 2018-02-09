package killrvideo.entity;

import java.util.Date;

public class VideoComment {
    private String videoId;
	private String commentId;
    private String userId;
    private String comment;
    private Date dateOfComment;

    public String getVideoId() {
		return videoId;
	}
	public String getCommentId() {
		return commentId;
	}
	public String getUserId() {
		return userId;
	}
	public String getComment() {
		return comment;
	}
	public Date getDateOfComment() {
		return dateOfComment;
	}
	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}
	public void setCommentId(String commentId) {
		this.commentId = commentId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public void setDateOfComment(Date dateOfComment) {
		this.dateOfComment = dateOfComment;
	}
	private static final VideoComment DEFAULT_INSTANCE;

	static {
		DEFAULT_INSTANCE = new VideoComment();
	}

	public static VideoComment getDefaultInstance() {
		return DEFAULT_INSTANCE;
	}

	public static final class Builder {
		private String userId;
		private String commentId;
		private String videoId;
		private String comment;
		private Date dateOfComment;
		
		public Builder mergeFrom(VideoComment prototype) {
			if (prototype == VideoComment.getDefaultInstance())
				return this;
			Builder newBuilder = new Builder();
			newBuilder.userId = prototype.userId;
			newBuilder.commentId = prototype.commentId;
			newBuilder.videoId = prototype.videoId;
			newBuilder.comment = prototype.comment;
			newBuilder.dateOfComment = prototype.dateOfComment;
			return newBuilder;
		}
		
		
		public Builder setUserId(String userId) {
			this.userId = userId;
			return this;
		}
		public Builder setCommentId(String commentId) {
			this.commentId = commentId;
			return this;
		}
		
		public Builder setVideoId(String videoId) {
			this.videoId = videoId;
			return this;
		}
		
		public Builder setComment(String comment) {
			this.comment = comment;
			return this;
		}
		
		public Builder setCommentTimestamp(Date dateOfComment) {
			 this.dateOfComment = dateOfComment;
			return this;
		}

		public VideoComment build() {
			VideoComment newInstance = new VideoComment();
			newInstance.userId = this.userId;
			newInstance.commentId = this.commentId;
			newInstance.videoId = this.videoId;
			newInstance.comment = this.comment;
			newInstance.dateOfComment = this.dateOfComment;
			return newInstance;
		}
	}

	public Builder newBuilderForType() {
		return newBuilder();
	}

	public static Builder newBuilder() {
		return DEFAULT_INSTANCE.toBuilder();
	}

	public static Builder newBuilder(VideoComment prototype) {
		return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
	}

	public Builder toBuilder() {
		return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
	}

    
}
