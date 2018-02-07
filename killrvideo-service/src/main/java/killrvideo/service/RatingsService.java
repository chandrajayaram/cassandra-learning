package killrvideo.service;

import static killrvideo.utils.ExceptionUtils.mergeStackTrace;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.eventbus.EventBus;

import killrvideo.entity.Schema;
import killrvideo.entity.UserRatings;
import killrvideo.entity.VideoRating;
import killrvideo.entity.VideoRatingByUser;
import killrvideo.events.CassandraMutationError;
import killrvideo.exception.ApplicationException;
import killrvideo.utils.ExecutionStatus;
import killrvideo.utils.ExecutionStatus.STATUS;
import killrvideo.utils.FutureUtils;
import killrvideo.validation.KillrVideoInputValidator;

@Service
public class RatingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RatingsService.class);

    @Inject
    MappingManager manager;

    @Inject
    Mapper<VideoRating> videoRatingMapper;

    @Inject
    Mapper<VideoRatingByUser> videoRatingByUserMapper;

    @Inject
    DseSession dseSession;

    @Inject
    EventBus eventBus;

    @Inject
    KillrVideoInputValidator validator;

    private String videoRatingsTableName;
    private PreparedStatement rateVideo_updateRatingPrepared;


    @PostConstruct
    public void init(){
        videoRatingsTableName = videoRatingMapper.getTableMetadata().getName();

        rateVideo_updateRatingPrepared = dseSession.prepare(
                QueryBuilder
                        .update(Schema.KEYSPACE, videoRatingsTableName)
                        .with(QueryBuilder.incr("rating_counter"))
                        .and(QueryBuilder.incr("rating_total", QueryBuilder.bindMarker()))
                        .where(QueryBuilder.eq("videoid", QueryBuilder.bindMarker()))
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    public void rateVideo(String videoid,String userid, Integer rating) {

        LOGGER.debug("-----Start rate video request-----");

/*        if (!validator.isValid(request, responseObserver)) {
            return;
        }
*/
        final Instant time = Instant.now();

        final UUID videoId = UUID.fromString(videoid);
        final UUID userId = UUID.fromString(userid);
        

        /**
         * Increment rating_counter by 1
         * Increment rating_total by amount of rating
         */
        BoundStatement counterUpdateStatement = rateVideo_updateRatingPrepared.bind()
                .setLong("rating_total", rating)
                .setUUID("videoid", videoId);

        /**
         * Here, instead of using logged batch, we can insert both mutations asynchronously
         * In case of error, we log the request into the mutation error log for replay later
         * by another micro-service
         *
         * Something else to notice is I am using both a prepared statement with executeAsync()
         * and a call to the mapper's saveAsync() methods.  I could have kept things uniform
         * and stuck with both prepared/bind statements, but I wanted to illustrate the combination
         * and use the mapper for the second statement because it is a simple save operation with no
         * options, increments, etc...  A key point is in the case you see below both statements are actually
         * prepared, the first one I did manually in a more traditional sense and in the second one the
         * mapper will prepare the statement for you automagically.
         */
        CompletableFuture<ExecutionStatus> result = CompletableFuture
                .allOf(
                        FutureUtils.buildCompletableFuture(dseSession.executeAsync(counterUpdateStatement)),
                        FutureUtils.buildCompletableFuture(videoRatingByUserMapper
                                .saveAsync(new VideoRatingByUser(videoId, userId, rating)))
                )
                .handle((rs, ex) -> {
                	ExecutionStatus exStatus = new ExecutionStatus();
                    if (ex == null) {
                        /**
                         * This eventBus.post() call will make its way to the SuggestedVideoService
                         * class to handle adding data to our graph recommendation engine
                         */
                        eventBus.post(new killrvideo.events.UserRatedVideo(videoid, userid, rating, time));
                        exStatus.setStatus(STATUS.SUCCESS);
                        
                        LOGGER.debug("End rate video request");

                    } else {
                        LOGGER.error("Exception rating video : " + mergeStackTrace(ex));
                        exStatus.setStatus(STATUS.FAIL);
                        exStatus.setException(ex);
                        eventBus.post(new CassandraMutationError(videoId, ex));
                    }
                    return exStatus;
                });
        try {
			ExecutionStatus status = result.get();
			if(status.getStatus() == STATUS.FAIL) {
				throw new ApplicationException(status.getException().getMessage());
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new ApplicationException(e.getMessage());
		}
    }

    public VideoRating getRating(String videoid) {

        LOGGER.debug("-----Start get video rating request-----");

        /*if (!validator.isValid(request, responseObserver)) {
            return;
        }*/

        final UUID videoId = UUID.fromString(videoid);

        // videoId matches the partition key set in the VideoRating class
        CompletableFuture<ExecutionStatus> result = FutureUtils.buildCompletableFuture(videoRatingMapper.getAsync(videoId))
                .handle((ratings, ex) -> {
                	ExecutionStatus exStatus = new ExecutionStatus();
                    if (ex != null) {
                        LOGGER.error("Exception when getting video rating : " + mergeStackTrace(ex));
                        exStatus.setStatus(STATUS.FAIL);
                        exStatus.setException(ex);
                    } else {
                        if (ratings != null) {
                            exStatus.setData(ratings.toRatingResponse());
                        }
                        /**
                         * If no row is returned (entity == null), we should
                         * still build a response with 0 as rating value
                         */
                        else {
                        	VideoRating videoRating = new VideoRating();
                        	videoRating.setVideoid(UUID.fromString(videoid));
                        	videoRating.setRatingCounter(0l);
                        	videoRating.setRatingTotal(0l);
                        	exStatus.setData(videoRating);
                        }
                        LOGGER.debug("End get video rating request");
                    }
                    return exStatus;
                });
        try {
			ExecutionStatus status = result.get();
			if(status.getStatus() == STATUS.FAIL) {
				throw new ApplicationException(status.getException().getMessage());
			}
			return (VideoRating) status.getData();
		} catch (InterruptedException | ExecutionException e) {
			throw new ApplicationException(e.getMessage());
		}
    }

    
    public UserRatings getUserRating(String videoid, String userid) {

        LOGGER.debug("-----Start get user rating request-----");
/*
        if (!validator.isValid(request, responseObserver)) {
            return;
        }*/

        final UUID videoId = UUID.fromString(videoid);
        final UUID userId = UUID.fromString(userid);

        CompletableFuture<ExecutionStatus> result = FutureUtils.buildCompletableFuture(videoRatingByUserMapper.getAsync(videoId, userId))
                .handle((videoRating, ex) -> {
                	ExecutionStatus exStatus = new ExecutionStatus();
                    if (ex != null) {
                        LOGGER.error("Exception when getting user rating : " + mergeStackTrace(ex));
                        exStatus.setStatus(STATUS.FAIL);
                        exStatus.setException(ex);

                    } else {
                        if (videoRating != null) {
                        	exStatus.setData(videoRating.toUserRatings());
                            
                        }
                        /**
                         * If no row is returned (entity == null), we should
                         * still build a response with 0 as rating value
                         */
                        else {
                        	exStatus.setData(new UserRatings(videoid, userid, 0));
                        }
                        LOGGER.debug("End get user rating request");
                    }
                    return exStatus;
                });
        try {
			ExecutionStatus status = result.get();
			if(status.getStatus() == STATUS.FAIL) {
				throw new ApplicationException(status.getException().getMessage());
			}
			return (UserRatings) status.getData();
		} catch (InterruptedException | ExecutionException e) {
			throw new ApplicationException(e.getMessage());
		}
    }

}
