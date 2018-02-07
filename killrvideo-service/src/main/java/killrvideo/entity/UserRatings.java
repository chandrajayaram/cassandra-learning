package killrvideo.entity;

public class UserRatings {
    private String videoid;
    private String userid;
    private int rating;
    
    public UserRatings(String videoid,String userid,int rating) {
    	this.videoid= videoid;
    	this.userid = userid;
    	this.rating = rating;
    }
    public UserRatings() {}
    
    public String getVideoid() {
		return videoid;
	}
	public String getUserid() {
		return userid;
	}
	public int getRating() {
		return rating;
	}
	public void setVideoid(String videoid) {
		this.videoid = videoid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public void setRating(int rating) {
		this.rating = rating;
	}
    
}
