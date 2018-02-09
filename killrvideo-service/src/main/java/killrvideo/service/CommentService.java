package killrvideo.service;

import static java.util.UUID.fromString;
import static killrvideo.utils.ExceptionUtils.mergeStackTrace;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.eventbus.EventBus;

import killrvideo.entity.CommentsByUser;
import killrvideo.entity.CommentsByVideo;
import killrvideo.entity.Schema;
import killrvideo.entity.UserComment;
import killrvideo.entity.VideoComment;
import killrvideo.events.CassandraMutationError;
import killrvideo.exception.ApplicationException;
import killrvideo.utils.FutureUtils;
import killrvideo.utils.PaginatedResponse;
import killrvideo.validation.KillrVideoInputValidator;

@Service
public class CommentService  {

    private static Logger LOGGER = LoggerFactory.getLogger(CommentService.class);

    @Inject
    Mapper<CommentsByUser> commentsByUserMapper;

    @Inject
    Mapper<CommentsByVideo> commentsByVideoMapper;

    @Inject
    MappingManager manager;

    @Inject
    EventBus eventBus;

    @Inject
    KillrVideoInputValidator validator;

    @Inject
    DseSession dseSession;

    private String commentsByUserTableName;
    private String commentsByVideoTableName;
    private PreparedStatement commentsByUserPrepared;
    private PreparedStatement commentsByVideoPrepared;
    private PreparedStatement getUserComments_startingPointPrepared;
    private PreparedStatement getUserComments_noStartingPointPrepared;
    private PreparedStatement getVideoComments_startingPointPrepared;
    private PreparedStatement getVideoComments_noStartingPointPrepared;

    @PostConstruct
    public void init(){
        /**
         * Set the following up in PostConstruct because 1) we have to
         * wait until after dependency injection for these to work,
         * and 2) we only want to load the prepared statements once at
         * the start of the service.  From here the prepared statements should
         * be cached on our Cassandra nodes.
         *
         * Alrighty, here is a case where I provide prepared statements
         * both with and without using QueryBuilder. The end result is essentially
         * the same and the one you choose largely comes down to style.
         */

        commentsByUserTableName = commentsByUserMapper.getTableMetadata().getName();
        commentsByVideoTableName = commentsByVideoMapper.getTableMetadata().getName();

        // Prepared statements for commentOnVideo()
        commentsByUserPrepared = dseSession.prepare(
                "INSERT INTO " + Schema.KEYSPACE + "." + commentsByUserTableName + " " +
                        "(userid, commentid, comment, videoid) " +
                        "VALUES (?, ?, ?, ?)"
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        commentsByVideoPrepared = dseSession.prepare(
                "INSERT INTO " + Schema.KEYSPACE + "." + commentsByVideoTableName + " " +
                        "(videoid, commentid, comment, userid) " +
                        "VALUES (?, ?, ?, ?)"
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        // Prepared statements for getUserComments()
        /**
         * Notice below I execute fcall() to pull the timestamp out of the
         * commentid timeuuid field, yet I am using the @Computed annotation
         * to do the same thing within the CommentsByUser entity for the dateOfComment
         * field.  I do this because I am using QueryBuilder for the query below.
         * @Computed is only supported when using the mapper stated per
         * http://docs.datastax.com/en/drivers/java/3.2/com/datastax/driver/mapping/annotations/Computed.html.
         * So, I essentially have 2 ways to get the timestamp out of my timeUUID column
         * depending on the type of query I am executing.
         */
        getUserComments_noStartingPointPrepared = dseSession.prepare(
                QueryBuilder
                        .select()
                        .column("userid")
                        .column("commentid")
                        .column("videoid")
                        .column("comment")
                        .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                        .from(Schema.KEYSPACE, commentsByUserTableName)
                        .where(QueryBuilder.eq("userid", QueryBuilder.bindMarker()))
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        getUserComments_startingPointPrepared = dseSession.prepare(
                QueryBuilder
                        .select()
                        .column("userid")
                        .column("commentid")
                        .column("videoid")
                        .column("comment")
                        .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                        .from(Schema.KEYSPACE, commentsByUserTableName)
                        .where(QueryBuilder.eq("userid", QueryBuilder.bindMarker()))
                        .and(QueryBuilder.lte("commentid", QueryBuilder.bindMarker()))
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        // Prepared statements for getVideoComments()
        getVideoComments_noStartingPointPrepared = dseSession.prepare(
                QueryBuilder
                    .select()
                    .column("videoid")
                    .column("commentid")
                    .column("userid")
                    .column("comment")
                    .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                    .from(Schema.KEYSPACE, commentsByVideoTableName)
                    .where(QueryBuilder.eq("videoid", QueryBuilder.bindMarker()))
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

        getVideoComments_startingPointPrepared = dseSession.prepare(
                QueryBuilder
                        .select()
                        .column("videoid")
                        .column("commentid")
                        .column("userid")
                        .column("comment")
                        .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                        .from(Schema.KEYSPACE, commentsByVideoTableName)
                        .where(QueryBuilder.eq("videoid", QueryBuilder.bindMarker()))
                        .and(QueryBuilder.lte("commentid", QueryBuilder.bindMarker()))
        ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }

    public void commentOnVideo(VideoComment request) {

        LOGGER.debug("-----Start comment on video request-----");

        /*if (!validator.isValid(request, responseObserver)) {
            return;
        }*/

        final Date now = new Date();
        final UUID userId = UUID.fromString(request.getUserId());
        final UUID videoId = UUID.fromString(request.getVideoId());
        final UUID commentId = UUID.fromString(request.getCommentId());
        final String comment = request.getComment();

        //:TODO Potential future work to use the mapper with saveAsync()
        BoundStatement bs1 = commentsByUserPrepared.bind(
                userId, commentId, comment, videoId
        );

        BoundStatement bs2 = commentsByVideoPrepared.bind(
                videoId, commentId, comment, userId
        );

        /**
         * We need to insert into comments_by_user and comments_by_video
         * simultaneously, thus using logged batch for automatic retries
         * in case of error
         */
        final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.LOGGED);
        batchStatement.add(bs1);
        batchStatement.add(bs2);
        batchStatement.setDefaultTimestamp(now.getTime());

        FutureUtils.buildCompletableFuture(dseSession.executeAsync(batchStatement))
            .handle((rs, ex) -> {
                if(rs != null) {
                    eventBus.post(VideoComment.newBuilder()
                            .setCommentId(request.getCommentId())
                            .setVideoId(request.getVideoId())
                            .setUserId(request.getUserId())
                            .setCommentTimestamp(now)
                            .build());
                    
                    LOGGER.debug("End comment on video request");

                } else if (ex != null) {
                	String exceptionMessage ="Exception commenting on video : " + mergeStackTrace(ex);
                    LOGGER.error(exceptionMessage);
                    eventBus.post(new CassandraMutationError(request, ex));
                    throw new ApplicationException(exceptionMessage);
                    
                }
                return rs;
            });
    }

    public PaginatedResponse<UserComment> getUserComments(final String startingCommentId, final String userId, final String pagingState, final int pageSize) {

        LOGGER.debug("Start get user comments request");

        /*if (!validator.isValid(request, responseObserver)) {
            return;
        }*/

        
        final Optional<String> _pagingState = Optional
                .ofNullable(pagingState)
                .filter(StringUtils::isNotBlank);

        BoundStatement statement;

        /**
         * Query without startingCommentId to get a reference point
         * Normally, the requested fetch size/page size is 1 to get
         * the first user comment as reference point
         */
        if (startingCommentId == null || isBlank(startingCommentId)) {
            LOGGER.debug("Query without startingCommentId");
            statement = getUserComments_noStartingPointPrepared.bind()
                    .setUUID("userid", fromString(userId));
        }

        /**
         * Subsequent requests always provide startingCommentId to load page
         * of user comments. Fetch size/page size is expected to be > 1
         */
        else {
            LOGGER.debug("Query WITH startingCommentId");
            statement = getUserComments_startingPointPrepared.bind()
                    .setUUID("userid", fromString(userId))
                    .setUUID("commentid", fromString(startingCommentId));
        }

        statement.setFetchSize(pageSize);

        _pagingState.ifPresent( x -> statement.setPagingState(PagingState.fromString(x)));
         
        CompletableFuture<PaginatedResponse<UserComment>> result =FutureUtils.buildCompletableFuture(dseSession.executeAsync(statement))
                .handle((commentResult, ex) -> {
                	PaginatedResponse<UserComment> respone = new PaginatedResponse<>();
                	try {
                        if (commentResult != null) {
                            
                        	List<UserComment> commentsByUserList = new ArrayList<>();

                            int remaining = commentResult.getAvailableWithoutFetching();
                            for (Row row : commentResult) {
                                CommentsByUser commentByUser = new CommentsByUser(
                                        row.getUUID("userid"), row.getUUID("commentid"),
                                        row.getUUID("videoid"), row.getString("comment")
                                );
                                
                                commentByUser.setDateOfComment(row.getTimestamp("comment_timestamp"));
                                
                                commentsByUserList.add(commentByUser.toUserComment());

                                if (--remaining == 0) {
                                    break;
                                }
                            }
                            respone.setData(commentsByUserList);

                            Optional.ofNullable(commentResult.getExecutionInfo().getPagingState())
                                    .map(PagingState::toString)
                                    .ifPresent(respone::setPagingState);
         
                            LOGGER.debug("End get user comments request");

                        } else if (ex != null) {
                            LOGGER.error("Exception getting user comments : " + mergeStackTrace(ex));
                            respone.setExeception(ex);
                        }

                    } catch (Exception exception) {
                        LOGGER.error("CATCH Exception getting user comments : " + mergeStackTrace(ex));
                        respone.setExeception(ex);
                    }
                    return respone;

                });
        try {
			return result.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new ApplicationException(e.getMessage());
		}

    }

    
    public PaginatedResponse<VideoComment> getVideoComments(final String startingCommentId, final String videoId, String pagingState, int pageSize) {

        LOGGER.debug("Start get video comments request");

    /*    if (!validator.isValid(request, responseObserver)) {
            return;
        }
*/
        
        
        final Optional<String> _pagingState = Optional
                .ofNullable(pagingState)
                .filter(StringUtils::isNotBlank);

        BoundStatement statement;

        /**
         * Query without startingCommentId to get a reference point
         * Normally, the requested fetch size/page size is 1 to get
         * the first video comment as reference point
         */
        if (startingCommentId == null || isBlank(startingCommentId)) {
            LOGGER.debug("Query without startingCommentId");
            statement = getVideoComments_noStartingPointPrepared.bind()
                    .setUUID("videoid", fromString(videoId));
        }
        /**
         * Subsequent requests always provide startingCommentId to load page
         * of video comments. Fetch size/page size is expected to be > 1
         */
        else {
            LOGGER.debug("Query WITH startingCommentId");
            statement = getVideoComments_startingPointPrepared.bind()
                    .setUUID("videoid", fromString(videoId))
                    .setUUID("commentid", fromString(startingCommentId));
        }

        statement.setFetchSize(pageSize);

        _pagingState.ifPresent( x -> statement.setPagingState(PagingState.fromString(x)));

        CompletableFuture<PaginatedResponse<VideoComment>> result = FutureUtils.buildCompletableFuture(dseSession.executeAsync(statement))
                .handle((commentResult, ex) -> {
                	PaginatedResponse<VideoComment> response = new PaginatedResponse<>();
                    try {
                        if (commentResult != null) {
                        	List<VideoComment> videoComments = new ArrayList<>();
                            
                            int remaining = commentResult.getAvailableWithoutFetching();
                            for (Row row : commentResult) {
                                CommentsByVideo commentByVideo = new CommentsByVideo(
                                        row.getUUID("videoid"), row.getUUID("commentid"),
                                        row.getUUID("userid"), row.getString("comment")
                                );

                                /**
                                 * Explicitly set dateOfComment because I cannot use the @Computed
                                 * annotation set on the dateOfComment field when using QueryBuilder.
                                 * This gives us the "proper" return object expected for the response to the front-end
                                 * UI.  It does not function if this value is null or not the correct type.
                                 */
                                commentByVideo.setDateOfComment(row.getTimestamp("comment_timestamp"));
                                videoComments.add(commentByVideo.toVideoComment());

                                if (--remaining == 0) {
                                    break;
                                }
                            }
                            response.setData(videoComments);
                            
                            Optional.ofNullable(commentResult.getExecutionInfo().getPagingState())
                                    .map(PagingState::toString)
                                    .ifPresent(response::setPagingState);
                

                            LOGGER.debug("End get video comments request");

                        } else if (ex != null) {
                            LOGGER.error("Exception getting video comments : " + mergeStackTrace(ex));
                            response.setExeception(ex);
                            
                        }

                    } catch (Exception exception) {
                        LOGGER.error("CATCH Exception getting video comments : " + mergeStackTrace(exception));
                        response.setExeception(ex);

                    }
                    return response;
                });
        try {
			return result.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new ApplicationException(e.getMessage());
		}
    }
}
