package killrvideo.events;

import java.time.Instant;

public class UserRatedVideo {
	 private String videoid;
	    private String userid;
	    private int rating;
	    private Instant ratingTimestamp;
	    public UserRatedVideo(String videoid,String userid,int rating, Instant ratingTimestamp) {
	    	this.videoid= videoid;
	    	this.userid = userid;
	    	this.rating = rating;
	    	this.ratingTimestamp = ratingTimestamp;
	    }
	    public UserRatedVideo() {}
	    
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
		public Instant getRatingTimestamp() {
			return ratingTimestamp;
		}
		public void setRatingTimestamp(Instant ratingTimestamp) {
			this.ratingTimestamp = ratingTimestamp;
		}
}
