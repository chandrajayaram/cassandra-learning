package killrvideo.service;

import static killrvideo.utils.ExceptionUtils.mergeStackTrace;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.dse.DseSession;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.protobuf.ProtocolStringList;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.entity.Schema;
import killrvideo.entity.VideoLocationType;
import killrvideo.entity.YouTubeVideo;
import killrvideo.events.CassandraMutationError;
import killrvideo.utils.FutureUtils;
import killrvideo.utils.TypeConverter;
import killrvideo.validation.KillrVideoInputValidator;

@Service
public class VideoCatalogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoCatalogService.class);
	
    @Inject
	EventBus eventBus;

	
	@Inject
	DseSession dseSession;

	@Inject
	KillrVideoInputValidator validator;
	
	public static final int MAX_DAYS_IN_PAST_FOR_LATEST_VIDEOS = 7;
    public static final int LATEST_VIDEOS_TTL_SECONDS = MAX_DAYS_IN_PAST_FOR_LATEST_VIDEOS * 24 * 3600;
    
	private String videosTableName;
    private String latestVideosTableName;
    private String userVideosTableName;
    private PreparedStatement latestVideoPreview_startingPointPrepared;
    private PreparedStatement latestVideoPreview_noStartingPointPrepared;
    private PreparedStatement userVideoPreview_startingPointPrepared;
    private PreparedStatement userVideoPreview_noStartingPointPrepared;
    private PreparedStatement submitYouTubeVideo_insertVideo;
    private PreparedStatement submitYouTubeVideo_insertUserVideo;
    private PreparedStatement submitYouTubeVideo_insertLatestVideo;
    
    @PostConstruct
	public void init() {
        /**
         * Set the following up in PostConstruct because 1) we have to
         * wait until after dependency injection for these to work,
         * and 2) we only want to load the prepared statements once at
         * the start of the service.  From here the prepared statements should
         * be cached on our Cassandra nodes.
         *
         * Note I am not using QueryBuilder with bindmarker() for these
         * statements.  This is not a value judgement, just a different way of doing it.
         * Take a look at some of the other services to see QueryBuilder.bindmarker() examples.
         */

        videosTableName = "videos";
        latestVideosTableName = "latest_videos";
        userVideosTableName = "user_videos";

        // Prepared statements for getLatestVideoPreviews()
        latestVideoPreview_startingPointPrepared = dseSession.prepare(
                "" +
                        "SELECT * " +
                        "FROM " + Schema.KEYSPACE + "." + latestVideosTableName + " " +
                        "WHERE yyyymmdd = :ymd " +
                        "AND (added_date, videoid) <= (:ad, :vid)"
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        latestVideoPreview_noStartingPointPrepared = dseSession.prepare(
                "" +
                        "SELECT * " +
                        "FROM " + Schema.KEYSPACE + "." + latestVideosTableName + " " +
                        "WHERE yyyymmdd = :ymd "
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        // Prepared statements for getUserVideoPreviews()
        userVideoPreview_startingPointPrepared = dseSession.prepare(
                "" +
                        "SELECT * " +
                        "FROM " + Schema.KEYSPACE + "." + userVideosTableName + " " +
                        "WHERE userid = :uid " +
                        "AND (added_date, videoid) <= (:ad, :vid)"
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        userVideoPreview_noStartingPointPrepared = dseSession.prepare(
                "" +
                        "SELECT * " +
                        "FROM " + Schema.KEYSPACE + "." + userVideosTableName + " " +
                        "WHERE userid = :uid "
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);


        // Prepared statements for submitYouTubeVideo()
        submitYouTubeVideo_insertVideo = dseSession.prepare(
                QueryBuilder
                        .insertInto(Schema.KEYSPACE, videosTableName)
                        .value("videoId", QueryBuilder.bindMarker())
                        .value("userId", QueryBuilder.bindMarker())
                        .value("name", QueryBuilder.bindMarker())
                        .value("description", QueryBuilder.bindMarker())
                        .value("location", QueryBuilder.bindMarker())
                        .value("location_type", QueryBuilder.bindMarker())
                        .value("preview_image_location", QueryBuilder.bindMarker())
                        .value("tags", QueryBuilder.bindMarker())
                        .value("added_date", QueryBuilder.bindMarker())
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        submitYouTubeVideo_insertUserVideo = dseSession.prepare(
                QueryBuilder
                        .insertInto(Schema.KEYSPACE, userVideosTableName)
                        .value("userid", QueryBuilder.bindMarker())
                        .value("videoid", QueryBuilder.bindMarker())
                        .value("name", QueryBuilder.bindMarker())
                        .value("preview_image_location", QueryBuilder.bindMarker())
                        .value("added_date", QueryBuilder.bindMarker())
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        submitYouTubeVideo_insertLatestVideo = dseSession.prepare(
                QueryBuilder
                        .insertInto(Schema.KEYSPACE, latestVideosTableName)
                        .value("yyyymmdd", QueryBuilder.bindMarker())
                        .value("userId", QueryBuilder.bindMarker())
                        .value("videoid", QueryBuilder.bindMarker())
                        .value("name", QueryBuilder.bindMarker())
                        .value("preview_image_location", QueryBuilder.bindMarker())
                        .value("added_date", QueryBuilder.bindMarker())
                        .using(QueryBuilder.ttl(LATEST_VIDEOS_TTL_SECONDS))
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    @Override
    public void submitYouTubeVideo(YouTubeVideo request) {

        LOGGER.debug("-----Start submitting youtube video-----");

        final Date now = new Date();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final String yyyyMMdd = dateFormat.format(now);
        final String location = request.getYouTubeVideoId();
        final String name = request.getName();
        final String description = request.getDescription();
        final List<String> tagsList = request.getTagsList();
        final String previewImageLocation = "//img.youtube.com/vi/"+ location + "/hqdefault.jpg";
        final UUID videoId = UUID.fromString(request.getVideoId());
        final UUID userId = UUID.fromString(request.getUserId());

        final BoundStatement insertVideo = submitYouTubeVideo_insertVideo.bind()
                .setUUID("videoid", videoId)
                .setUUID("userid", userId)
                .setString("name", name)
                .setString("description", description)
                .setString("location", location)
                .setInt("location_type", VideoLocationType.YOUTUBE.ordinal())
                .setString("preview_image_location", previewImageLocation)
                .setSet("tags", Sets.newHashSet(tagsList.iterator()))
                .setTimestamp("added_date", now);

        final BoundStatement insertUserVideo = submitYouTubeVideo_insertUserVideo.bind()
                .setUUID("userid", userId)
                .setUUID("videoid", videoId)
                .setString("name", name)
                .setString("preview_image_location", previewImageLocation)
                .setTimestamp("added_date", now);

        final BoundStatement insertLatestVideo = submitYouTubeVideo_insertLatestVideo.bind()
                .setString("yyyymmdd", yyyyMMdd)
                .setUUID("userid", userId)
                .setUUID("videoid", videoId)
                .setString("name", name)
                .setString("preview_image_location", previewImageLocation)
                .setTimestamp("added_date", now);

        /**
         * Logged batch insert for automatic retry
         */
        final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.LOGGED);
        batchStatement.add(insertVideo);
        batchStatement.add(insertUserVideo);
        batchStatement.add(insertLatestVideo);
        batchStatement.setDefaultTimestamp(now.getTime());

        CompletableFuture<ResultSet> insertUserFuture = FutureUtils.buildCompletableFuture(dseSession.executeAsync(batchStatement))
                .handle((rs, ex) -> {
                    if (rs != null) {
                        /**
                         * See class {@link VideoAddedHandlers} for the impl
                         */
                        final YouTubeVideoAdded.Builder youTubeVideoAdded = YouTubeVideoAdded.newBuilder()
                                .setAddedDate(TypeConverter.dateToTimestamp(now))
                                .setDescription(description)
                                .setLocation(location)
                                .setName(name)
                                .setPreviewImageLocation(previewImageLocation)
                                .setTimestamp(TypeConverter.dateToTimestamp(now))
                                .setUserId(request.getUserId())
                                .setVideoId(request.getVideoId());

                        youTubeVideoAdded.addAllTags(Sets.newHashSet(tagsList));

                        /**
                         * eventbus.post() for youTubeVideoAdded below is located both in the
                         * VideoAddedhandlers and SuggestedVideos Service classes within the handle() method.
                         * The YouTubeVideoAdded type triggers the handler.  The call in SuggestedVideos is
                         * responsible for adding data into our graph recommendation engine.
                         */
                        eventBus.post(youTubeVideoAdded.build());

                        responseObserver.onNext(SubmitYouTubeVideoResponse.newBuilder().build());
                        responseObserver.onCompleted();

                        LOGGER.debug("End submitting youtube video");

                    } else if (ex != null) {
                        LOGGER.error("Exception submitting youtube video : " + mergeStackTrace(ex));

                        eventBus.post(new CassandraMutationError(request, ex));
                        responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());

                    }
                    return rs;
                });
    }

}
