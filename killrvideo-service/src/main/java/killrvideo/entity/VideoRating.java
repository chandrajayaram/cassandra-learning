package killrvideo.entity;

import static killrvideo.entity.Schema.KEYSPACE;

import java.util.Optional;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = KEYSPACE, name = "video_ratings")
public class VideoRating {

    @PartitionKey
    private UUID videoid;

    @Column(name = "rating_counter")
    private Long ratingCounter;

    @Column(name = "rating_total")
    private Long ratingTotal;

    public UUID getVideoid() {
        return videoid;
    }

    public void setVideoid(UUID videoid) {
        this.videoid = videoid;
    }

    public Long getRatingCounter() {
        return ratingCounter;
    }

    public void setRatingCounter(Long ratingCounter) {
        this.ratingCounter = ratingCounter;
    }

    public Long getRatingTotal() {
        return ratingTotal;
    }

    public void setRatingTotal(Long ratingTotal) {
        this.ratingTotal = ratingTotal;
    }

    public VideoRating toRatingResponse() {
    	VideoRating videoRating = new VideoRating();
    	videoRating.setVideoid(videoid);
    	videoRating.setRatingCounter(Optional.ofNullable(ratingCounter).orElse(0L));
    	videoRating.setRatingTotal(Optional.ofNullable(ratingTotal).orElse(0L));
    	return videoRating;
    }

}
