package killrvideo.service;

import static java.util.stream.Collectors.toList;
import static killrvideo.utils.ExceptionUtils.mergeStackTrace;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.mapping.Result;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.common.CommonTypes.Uuid;
import killrvideo.entity.LatestVideos;
import killrvideo.entity.Schema;
import killrvideo.entity.User;
import killrvideo.entity.UserVideos;
import killrvideo.entity.Video;
import killrvideo.entity.VideoLocationType;
import killrvideo.entity.YouTubeVideo;
import killrvideo.events.CassandraMutationError;
import killrvideo.events.YouTubeVideoAdded;
import killrvideo.exception.ApplicationException;
import killrvideo.service.VideoCatalogService.CustomPagingState;
import killrvideo.utils.FutureUtils;
import killrvideo.validation.KillrVideoInputValidator;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetLatestVideoPreviewsRequest;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetLatestVideoPreviewsResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetUserVideoPreviewsRequest;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetUserVideoPreviewsResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetVideoPreviewsRequest;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetVideoPreviewsResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetVideoRequest;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetVideoResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.VideoPreview;

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
	private PreparedStatement select_video;

	@PostConstruct
	public void init() {
		/**
		 * Set the following up in PostConstruct because 1) we have to wait
		 * until after dependency injection for these to work, and 2) we only
		 * want to load the prepared statements once at the start of the
		 * service. From here the prepared statements should be cached on our
		 * Cassandra nodes.
		 *
		 * Note I am not using QueryBuilder with bindmarker() for these
		 * statements. This is not a value judgement, just a different way of
		 * doing it. Take a look at some of the other services to see
		 * QueryBuilder.bindmarker() examples.
		 */

		videosTableName = "videos";
		latestVideosTableName = "latest_videos";
		userVideosTableName = "user_videos";

		// Prepared statements for getLatestVideoPreviews()
		latestVideoPreview_startingPointPrepared = dseSession
				.prepare("" + "SELECT * " + "FROM " + Schema.KEYSPACE + "." + latestVideosTableName + " "
						+ "WHERE yyyymmdd = :ymd " + "AND (added_date, videoid) <= (:ad, :vid)")
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		latestVideoPreview_noStartingPointPrepared = dseSession.prepare("" + "SELECT * " + "FROM " + Schema.KEYSPACE
				+ "." + latestVideosTableName + " " + "WHERE yyyymmdd = :ymd ")
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		// Prepared statements for getUserVideoPreviews()
		userVideoPreview_startingPointPrepared = dseSession
				.prepare("" + "SELECT * " + "FROM " + Schema.KEYSPACE + "." + userVideosTableName + " "
						+ "WHERE userid = :uid " + "AND (added_date, videoid) <= (:ad, :vid)")
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		userVideoPreview_noStartingPointPrepared = dseSession.prepare(
				"" + "SELECT * " + "FROM " + Schema.KEYSPACE + "." + userVideosTableName + " " + "WHERE userid = :uid ")
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		// Prepared statements for submitYouTubeVideo()
		submitYouTubeVideo_insertVideo = dseSession
				.prepare(QueryBuilder.insertInto(Schema.KEYSPACE, videosTableName)
						.value("videoId", QueryBuilder.bindMarker()).value("userId", QueryBuilder.bindMarker())
						.value("name", QueryBuilder.bindMarker()).value("description", QueryBuilder.bindMarker())
						.value("location", QueryBuilder.bindMarker()).value("location_type", QueryBuilder.bindMarker())
						.value("preview_image_location", QueryBuilder.bindMarker())
						.value("tags", QueryBuilder.bindMarker()).value("added_date", QueryBuilder.bindMarker()))
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		submitYouTubeVideo_insertUserVideo = dseSession
				.prepare(QueryBuilder.insertInto(Schema.KEYSPACE, userVideosTableName)
						.value("userid", QueryBuilder.bindMarker()).value("videoid", QueryBuilder.bindMarker())
						.value("name", QueryBuilder.bindMarker())
						.value("preview_image_location", QueryBuilder.bindMarker())
						.value("added_date", QueryBuilder.bindMarker()))
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		submitYouTubeVideo_insertLatestVideo = dseSession.prepare(QueryBuilder
				.insertInto(Schema.KEYSPACE, latestVideosTableName).value("yyyymmdd", QueryBuilder.bindMarker())
				.value("userId", QueryBuilder.bindMarker()).value("videoid", QueryBuilder.bindMarker())
				.value("name", QueryBuilder.bindMarker()).value("preview_image_location", QueryBuilder.bindMarker())
				.value("added_date", QueryBuilder.bindMarker()).using(QueryBuilder.ttl(LATEST_VIDEOS_TTL_SECONDS)))
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		select_video = dseSession
				.prepare(
						"SELECT * " + "FROM " + Schema.KEYSPACE + "." + videosTableName + " " + "WHERE videoid = :vid ")
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

	}

	public void submitYouTubeVideo(Video request) {

		LOGGER.debug("-----Start submitting youtube video-----");

		final Date now = new Date();
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		final String yyyyMMdd = dateFormat.format(now);
		final String location = request.getLocation();
		final String name = request.getName();
		final String description = request.getDescription();
		final Set<String> tags = request.getTags();
		final String previewImageLocation = "//img.youtube.com/vi/" + location + "/hqdefault.jpg";
		final UUID videoId = UUID.randomUUID();
		final UUID userId = request.getUserId();

		final BoundStatement insertVideo = submitYouTubeVideo_insertVideo.bind().setUUID("videoid", videoId)
				.setUUID("userid", userId).setString("name", name).setString("description", description)
				.setString("location", location).setInt("location_type", VideoLocationType.YOUTUBE.ordinal())
				.setString("preview_image_location", previewImageLocation).setSet("tags", tags)
				.setTimestamp("added_date", now);

		final BoundStatement insertUserVideo = submitYouTubeVideo_insertUserVideo.bind().setUUID("userid", userId)
				.setUUID("videoid", videoId).setString("name", name)
				.setString("preview_image_location", previewImageLocation).setTimestamp("added_date", now);

		final BoundStatement insertLatestVideo = submitYouTubeVideo_insertLatestVideo.bind()
				.setString("yyyymmdd", yyyyMMdd).setUUID("userid", userId).setUUID("videoid", videoId)
				.setString("name", name).setString("preview_image_location", previewImageLocation)
				.setTimestamp("added_date", now);

		/**
		 * Logged batch insert for automatic retry
		 */
		final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.LOGGED);
		batchStatement.add(insertVideo);
		batchStatement.add(insertUserVideo);
		batchStatement.add(insertLatestVideo);
		batchStatement.setDefaultTimestamp(now.getTime());

		CompletableFuture<ResultSet> insertUserFuture = FutureUtils
				.buildCompletableFuture(dseSession.executeAsync(batchStatement)).handle((rs, ex) -> {
					if (rs != null) {
						/**
						 * See class {@link VideoAddedHandlers} for the impl
						 */
						final YouTubeVideoAdded youTubeVideoAdded = new YouTubeVideoAdded();

						youTubeVideoAdded.setAddedDate(now);
						youTubeVideoAdded.setDescription(description);
						youTubeVideoAdded.setLocation(location);
						youTubeVideoAdded.setName(name);
						youTubeVideoAdded.setPreviewImageLocation(previewImageLocation);
						youTubeVideoAdded.setTimestamp(now);
						youTubeVideoAdded.setUserId(request.getUserId().toString());
						youTubeVideoAdded.setVideoId(request.getVideoId().toString());

						youTubeVideoAdded.addAllTags(Sets.newHashSet(tags));

						/**
						 * eventbus.post() for youTubeVideoAdded below is
						 * located both in the VideoAddedhandlers and
						 * SuggestedVideos Service classes within the handle()
						 * method. The YouTubeVideoAdded type triggers the
						 * handler. The call in SuggestedVideos is responsible
						 * for adding data into our graph recommendation engine.
						 */
						eventBus.post(youTubeVideoAdded);

						LOGGER.debug("End submitting youtube video");

					} else if (ex != null) {
						LOGGER.error("Exception submitting youtube video : " + mergeStackTrace(ex));

						eventBus.post(new CassandraMutationError(request, ex));

					}
					return rs;
				});
	}

	public Video getVideo(String reqVideoId) {

		LOGGER.debug("-----Start getting video-----");

		if (reqVideoId == null) {
			return null;
		}

		final UUID videoId = UUID.fromString(reqVideoId);

		final BoundStatement selVideo = select_video.bind().setUUID("vid", videoId);
		CompletableFuture<ResultSet> selectUserFuture = FutureUtils
				.buildCompletableFuture(dseSession.executeAsync(selVideo));
		ResultSet rs;
		try {
			rs = selectUserFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new ApplicationException("Error occoured while retrieving user ", e);
		}
		if (rs == null) {
			return null;
		}
		Row row = rs.one();
		Video video = new Video();
		video.setVideoId(row.getUUID("videoid"));
		video.setAddedDate(row.getTimestamp("added_date"));
		video.setDescription(row.getString("description"));
		video.setLocation(row.getString("location"));
		video.setLocationType(VideoLocationType.valueOf(row.getString("location_type")));
		video.setName(row.getString("name"));
		video.setPreviewImageLocation(row.getString("preview_image_location"));
		video.setTags(row.getSet("tags", String.class));
		video.setUserId(row.getUUID("userid"));
		
		LOGGER.debug("End getting video");

		return video;
	}

	
	public void getVideoPreviews(GetVideoPreviewsRequest request,
			StreamObserver<GetVideoPreviewsResponse> responseObserver) {

		LOGGER.debug("-----Start getting video preview-----");

		if (!validator.isValid(request, responseObserver)) {
			return;
		}

		final GetVideoPreviewsResponse.Builder builder = GetVideoPreviewsResponse.newBuilder();

		if (request.getVideoIdsCount() == 0 || request.getVideoIdsList() == null) {
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();

			LOGGER.warn("No video id provided for video preview");

			return;
		}

		try {
			/**
			 * Fire a list of async SELECT, one for each video id
			 */
			final List<CompletableFuture<Video>> listFuture = request.getVideoIdsList().stream()
					.map(uuid -> UUID.fromString(uuid.getValue()))
					.map(uuid -> FutureUtils.buildCompletableFuture(videoMapper.getAsync(uuid))).collect(toList());

			/**
			 * Merge all the async SELECT results
			 */
			CompletableFuture.allOf(listFuture.toArray(new CompletableFuture[listFuture.size()]))
					.thenApply(v -> listFuture.stream().map(CompletableFuture::join).collect(toList()))
					.handle((list, ex) -> {
						if (list != null) {
							list.stream().filter(x -> x != null)
									.forEach(entity -> builder.addVideoPreviews(entity.toVideoPreview()));

							responseObserver.onNext(builder.build());
							responseObserver.onCompleted();

							LOGGER.debug("End getting video preview");

						} else if (ex != null) {
							LOGGER.error("Exception getting video preview : " + mergeStackTrace(ex));

							responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());

						}
						return list;
					});

		} catch (Exception ex) {
			LOGGER.error("Exception getting video preview : " + mergeStackTrace(ex));

			responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());
		}
	}

	/**
	 * In this method, we craft our own paging state. The custom paging state
	 * format is: <br/>
	 * <br/>
	 * <code>
	 * yyyyMMdd_yyyyMMdd_yyyyMMdd_yyyyMMdd_yyyyMMdd_yyyyMMdd_yyyyMMdd_yyyyMMdd,&lt;index&gt;,&lt;Cassandra paging state as string&gt;
	 * </code> <br/>
	 * <br/>
	 * <ul>
	 * <li>The first field is the date of 7 days in the past, starting from
	 * <strong>now</strong></li>
	 * <li>The second field is the index in this date list, to know at which day
	 * in the past we stop at the previous query</li>
	 * <li>The last field is the serialized form of the native Cassandra paging
	 * state</li>
	 * </ul>
	 *
	 * On the first query, we create our own custom paging state in the server
	 * by computing the list of 8 days in the past, the <strong>index</strong>
	 * is set to 0 and there is no native Cassandra paging state
	 *
	 * <br/>
	 * <br/>
	 *
	 * On subsequent request, we decode the custom paging state coming from the
	 * web app and resume querying from the appropriate date and we inject also
	 * the native Cassandra paging state.
	 *
	 * <br/>
	 * <br/>
	 *
	 * <strong>However, we can only use the native Cassandra paging state for
	 * the 1st query in the for loop. Indeed Cassandra paging state is a hash of
	 * query string and bound values. We may switch partition to move one day
	 * back in the past to fetch more results so the paging state will no longer
	 * be usable</strong>
	 *
	 *
	 */
	
	public void getLatestVideoPreviews(GetLatestVideoPreviewsRequest request,
			StreamObserver<GetLatestVideoPreviewsResponse> responseObserver) {

		LOGGER.debug("-----Start getting latest video preview-----");

		if (!validator.isValid(request, responseObserver)) {
			return;
		}

		final CustomPagingState customPagingState = parseCustomPagingState(
				Optional.ofNullable(request.getPagingState())).orElse(this.buildFirstCustomPagingState());

		final List<String> buckets = customPagingState.buckets;
		int bucketIndex = customPagingState.currentBucket;
		final String rowPagingState = customPagingState.cassandraPagingState;
		LOGGER.debug("Custom paging state is: buckets: " + buckets.size() + " index: " + bucketIndex + " state: "
				+ rowPagingState);

		final Optional<Date> startingAddedDate = Optional.ofNullable(request.getStartingAddedDate())
				.filter(x -> StringUtils.isNotBlank(x.toString()))
				.map(x -> Instant.ofEpochSecond(x.getSeconds(), x.getNanos())).map(Date::from);

		final Optional<UUID> startingVideoId = Optional.ofNullable(request.getStartingVideoId())
				.filter(x -> StringUtils.isNotBlank(x.toString())).map(x -> x.getValue())
				.filter(StringUtils::isNotBlank).map(UUID::fromString);

		final List<VideoPreview> results = new ArrayList<>();
		String nextPageState = "";

		/**
		 * Boolean to know if the native Cassandra paging state has been used
		 */
		final AtomicBoolean cassandraPagingStateUsed = new AtomicBoolean(false);

		try {
			while (bucketIndex < buckets.size()) {
				int recordsStillNeeded = request.getPageSize() - results.size();
				LOGGER.debug("\nrecordsStillNeeded is: " + recordsStillNeeded + " pageSize is: " + request.getPageSize()
						+ " results.size is: " + results.size());

				final String yyyyMMdd = buckets.get(bucketIndex);

				final Optional<String> pagingState = Optional.ofNullable(rowPagingState).filter(StringUtils::isNotBlank)
						.filter(pg -> !cassandraPagingStateUsed.get());

				BoundStatement bound;

				if (startingAddedDate.isPresent() && startingVideoId.isPresent()) {
					/**
					 * The startingPointPrepared statement can be found at the
					 * top of the class within PostConstruct
					 */
					bound = latestVideoPreview_startingPointPrepared.bind().setString("ymd", yyyyMMdd)
							.setTimestamp("ad", startingAddedDate.get()).setUUID("vid", startingVideoId.get());

					LOGGER.debug("Current query is: " + bound.preparedStatement().getQueryString());

				} else {
					/**
					 * The noStartingPointPrepared statement can be found at the
					 * top of the class within PostConstruct
					 */
					bound = latestVideoPreview_noStartingPointPrepared.bind().setString("ymd", yyyyMMdd);

					LOGGER.debug("Current query is: " + bound.preparedStatement().getQueryString());
				}

				bound.setFetchSize(recordsStillNeeded);
				LOGGER.debug("FETCH SIZE is: " + bound.getFetchSize() + " ymd is: " + yyyyMMdd);

				pagingState.ifPresent(x -> {
					bound.setPagingState(PagingState.fromString(x));
					cassandraPagingStateUsed.compareAndSet(false, true);
				});

				final CompletableFuture<Result<LatestVideos>> videosFuture = FutureUtils
						.buildCompletableFuture(latestVideosMapper.mapAsync(dseSession.executeAsync(bound)))
						.handle((latestVideos, ex) -> {
							if (latestVideos != null) {
								/**
								 * For those of you wondering where the call to
								 * fetchMoreResults() is take a look here for an
								 * explanation
								 * https://docs.datastax.com/en/drivers/java/3.2/com/datastax/driver/core/PagingIterable.html#getAvailableWithoutFetching--
								 * Quick summary, when
								 * getAvailableWithoutFetching() == 0 it
								 * automatically calls fetchMoreResults() We
								 * could use it to force a fetch in a "prefetch"
								 * scenario, but that is not what we are doing
								 * here.
								 */
								int remaining = latestVideos.getAvailableWithoutFetching();
								for (LatestVideos latestVideo : latestVideos) {
									LOGGER.debug("latest video is: " + latestVideo.getName());
									// Add each row to results
									results.add(latestVideo.toVideoPreview());

									if (--remaining == 0) {
										break;
									}
								}

							} else if (ex != null) {
								LOGGER.error("Exception getting latest video preview : " + mergeStackTrace(ex));
								responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());

							}
							return latestVideos;
						});

				final Result<LatestVideos> videos = videosFuture.get();

				// See if we can stop querying
				LOGGER.debug("results.size() is: " + results.size() + " request.getPageSize() is : "
						+ request.getPageSize());
				if (results.size() == request.getPageSize()) {
					final PagingState cassandraPagingState = videos.getAllExecutionInfo().get(0).getPagingState();

					if (cassandraPagingState != null) {
						LOGGER.debug("results.size() == request.getPageSize()");
						// Start from where we left off in this bucket if we get
						// the next page
						nextPageState = createPagingState(buckets, bucketIndex, cassandraPagingState.toString());

						break;
					}

					// Start from the beginning of the next bucket since we're
					// out of rows in this one
				} else if (bucketIndex == buckets.size() - 1) {
					LOGGER.debug("bucketIndex == buckets.size() - 1)");
					nextPageState = createPagingState(buckets, bucketIndex + 1, "");
				}

				LOGGER.debug("" + "buckets: " + buckets.size() + " index: " + bucketIndex + " state: " + nextPageState
						+ " results size: " + results.size() + " request pageSize: " + request.getPageSize());
				bucketIndex++;
			}

			responseObserver.onNext(GetLatestVideoPreviewsResponse.newBuilder().addAllVideoPreviews(results)
					.setPagingState(nextPageState).build());
			responseObserver.onCompleted();

		} catch (Throwable throwable) {
			LOGGER.error("Exception when getting latest preview videos : " + mergeStackTrace(throwable));
			responseObserver.onError(Status.INTERNAL.withCause(throwable).asRuntimeException());
		}
		LOGGER.debug("End getting latest video preview");
	}

	
	public void getUserVideoPreviews(GetUserVideoPreviewsRequest request,
			StreamObserver<GetUserVideoPreviewsResponse> responseObserver) {

		LOGGER.debug("-----Start getting user video preview-----");

		if (!validator.isValid(request, responseObserver)) {
			return;
		}

		final UUID userId = UUID.fromString(request.getUserId().getValue());
		final Optional<UUID> startingVideoId = Optional.ofNullable(request.getStartingVideoId()).map(Uuid::getValue)
				.filter(StringUtils::isNotBlank).map(UUID::fromString);

		final Optional<Date> startingAddedDate = Optional.ofNullable(request.getStartingAddedDate())
				.map(ts -> Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())).map(Date::from);

		final Optional<String> pagingState = Optional.ofNullable(request.getPagingState())
				.filter(StringUtils::isNotBlank);
		BoundStatement bound;

		/**
		 * If startingAddedDate and startingVideoId are provided, we do NOT use
		 * the paging state
		 */
		if (startingVideoId.isPresent() && startingAddedDate.isPresent()) {
			/**
			 * The startingPointPrepared statement can be found at the top of
			 * the class within PostConstruct
			 */
			bound = userVideoPreview_startingPointPrepared.bind().setUUID("uid", userId)
					.setTimestamp("ad", startingAddedDate.get()).setUUID("vid", startingVideoId.get());

			LOGGER.debug("Current query is: " + bound.preparedStatement().getQueryString());

		} else {
			/**
			 * The noStartingPointPrepared statement can be found at the top of
			 * the class within PostConstruct
			 */
			bound = userVideoPreview_noStartingPointPrepared.bind().setUUID("uid", userId);

			LOGGER.debug("Current query is: " + bound.preparedStatement().getQueryString());
		}

		bound.setFetchSize(request.getPageSize());
		pagingState.ifPresent(x -> bound.setPagingState(PagingState.fromString(x)));

		/**
		 * Notice since I am passing userVideosMapper.mapAsync() into my call I
		 * get back results that are already mapped to UserVideos entities. This
		 * is a really nice convenience the mapper provides.
		 */
		FutureUtils.buildCompletableFuture(userVideosMapper.mapAsync(dseSession.executeAsync(bound)))
				.handle((userVideos, ex) -> {
					try {
						if (userVideos != null) {
							final GetUserVideoPreviewsResponse.Builder builder = GetUserVideoPreviewsResponse
									.newBuilder();

							int remaining = userVideos.getAvailableWithoutFetching();
							for (UserVideos userVideo : userVideos) {
								builder.addVideoPreviews(userVideo.toVideoPreview());
								builder.setUserId(request.getUserId());

								if (--remaining == 0) {
									break;
								}
							}

							Optional.ofNullable(userVideos.getExecutionInfo().getPagingState())
									.map(PagingState::toString).ifPresent(builder::setPagingState);
							responseObserver.onNext(builder.build());
							responseObserver.onCompleted();

							LOGGER.debug("End getting user video preview");

						} else if (ex != null) {
							LOGGER.error("Exception getting user video preview : " + mergeStackTrace(ex));

							responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());

						}

					} catch (Exception e) {
						LOGGER.error("Exception CATCH getting user video preview : " + mergeStackTrace(e));

						responseObserver.onError(Status.INTERNAL.withCause(e).asRuntimeException());
					}
					return userVideos;

				});
	}

}
