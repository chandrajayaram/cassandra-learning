package killrvideo.entity;

import java.util.Date;

public class UserComment {
	private String userId;
	private String commentId;
	private String videoId;
	private String comment;
	private Date dateOfComment;
	
	
	public Date getDateOfComment() {
		return dateOfComment;
	}
	public String getUserId() {
		return userId;
	}
	public String getCommentId() {
		return commentId;
	}
	public String getVideoId() {
		return videoId;
	}
	public String getComment() {
		return comment;
	}
	
	private static final UserComment DEFAULT_INSTANCE;

	static {
		DEFAULT_INSTANCE = new UserComment();
	}

	public static UserComment getDefaultInstance() {
		return DEFAULT_INSTANCE;
	}

	public static final class Builder {
		private String userId;
		private String commentId;
		private String videoId;
		private String comment;
		private Date dateOfComment;
		
		public Builder mergeFrom(UserComment prototype) {
			if (prototype == UserComment.getDefaultInstance())
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

		public UserComment build() {
			UserComment newInstance = new UserComment();
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

	public static Builder newBuilder(UserComment prototype) {
		return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
	}

	public Builder toBuilder() {
		return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
	}

	

}
