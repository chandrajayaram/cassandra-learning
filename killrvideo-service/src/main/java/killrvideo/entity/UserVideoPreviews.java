package killrvideo.entity;

import java.util.List;

public class UserVideoPreviews {
	private String userId;
	private List <VideoPreview> previewList;
	private String pagingState;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public List<VideoPreview> getPreviewList() {
		return previewList;
	}
	public void setPreviewList(List<VideoPreview> previewList) {
		this.previewList = previewList;
	}
	public String getPagingState() {
		return pagingState;
	}
	public void setPagingState(String pagingState) {
		this.pagingState = pagingState;
	}
}	
