package killrvideo.service;

import static killrvideo.utils.ExceptionUtils.mergeStackTrace;

import java.util.Date;
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
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.dse.DseSession;
import com.google.common.eventbus.EventBus;

import killrvideo.entity.Schema;
import killrvideo.entity.User;
import killrvideo.utils.FutureUtils;
import killrvideo.utils.HashUtils;
import killrvideo.validation.KillrVideoInputValidator;
@Service
public class UserManagementService {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementService.class);

	@Inject
	EventBus eventBus;

	@Inject
	DseSession dseSession;

	@Inject
	KillrVideoInputValidator validator;

	private String usersTableName;
	private String userCredentialsTableName;
	private PreparedStatement createUser_checkEmailPrepared;
	private PreparedStatement createUser_insertUserPrepared;
	private PreparedStatement getUserUser_getUsersPrepared;
	private PreparedStatement getUser_credentials;

	@PostConstruct
	public void init() {
		usersTableName = "users";
		userCredentialsTableName = "user_credentials";

		createUser_checkEmailPrepared = dseSession.prepare(QueryBuilder
				.insertInto(Schema.KEYSPACE, userCredentialsTableName).value("email", QueryBuilder.bindMarker())
				.value("password", QueryBuilder.bindMarker()).value("userid", QueryBuilder.bindMarker()).ifNotExists() // use
																														// lightweight
																														// transaction
		).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		createUser_insertUserPrepared = dseSession.prepare(QueryBuilder.insertInto(Schema.KEYSPACE, usersTableName)
				.value("userid", QueryBuilder.bindMarker()).value("firstname", QueryBuilder.bindMarker())
				.value("lastname", QueryBuilder.bindMarker()).value("email", QueryBuilder.bindMarker())
				.value("created_date", QueryBuilder.bindMarker()).ifNotExists() // use
																				// lightweight
																				// transaction
		).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		getUserUser_getUsersPrepared = dseSession
				.prepare(QueryBuilder.select().all().from(Schema.KEYSPACE, usersTableName)
						.where(QueryBuilder.in("userid", QueryBuilder.bindMarker())))
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		getUser_credentials = dseSession
				.prepare(QueryBuilder.select().all().from(Schema.KEYSPACE, userCredentialsTableName)
						.where(QueryBuilder.in("email", QueryBuilder.bindMarker())))
				.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

	}

	public String createUser(User user) {

		LOGGER.debug("-----Start creating user-----");

		final Date now = new Date();
		final UUID userIdUUID = UUID.randomUUID();

		/** Trim the password **/
		final String hashedPassword = HashUtils.hashPassword(user.getPassword().trim());
		final String exceptionMessage = String.format("Exception creating user because it already exists with email %s",
				user.getEmail());

		/**
		 * We insert first the credentials since the LWT condition is on the
		 * user email
		 *
		 * Note, the LWT condition is set up at the prepared statement
		 */
		final BoundStatement checkEmailQuery = createUser_checkEmailPrepared.bind().setString("email", user.getEmail())
				.setString("password", hashedPassword).setUUID("userid", userIdUUID);

		/**
		 * Note that we have multiple executeAsync() calls in the following
		 * chain. We check our user_credentials first, if that passes, we move
		 * onto inserting the user into the users table. Both cases use
		 * lightweight transactions to ensure we are not duplicating already
		 * existing users within the database.
		 */
		CompletableFuture<ResultSet> checkEmailFuture = FutureUtils
				.buildCompletableFuture(dseSession.executeAsync(checkEmailQuery))
				/**
				 * I use the *Async() version of .handle below because I am
				 * chaining multiple async futures. In testing we found that
				 * chains like this would cause timeouts possibly from
				 * starvation.
				 */
				.handleAsync((rs, ex) -> {
					try {
						if (rs != null) {
							/**
							 * Check the result of the LWT, if it's false the
							 * email already exists within our user_credentials
							 * table and must not be duplicated. Note the use of
							 * wasApplied(), this is a convenience method
							 * described here ->
							 * http://docs.datastax.com/en/drivers/java/3.2/com/datastax/driver/core/ResultSet.html#wasApplied--
							 * that allows an easy check of a conditional
							 * statement.
							 */
							if (!rs.wasApplied()) {
								throw new Throwable(exceptionMessage);
							}

						} else { // throw in case our result set is null
							throw new Throwable(ex);
						}

					} catch (Throwable t) {
						final String message = t.getMessage();
						LOGGER.debug(this.getClass().getName() + ".createUser() " + message);
						throw new RuntimeException(t);
					}
					return rs;
				});

		/**
		 * No LWT error, we can proceed further Execute our insert statement in
		 * an async fashion as well and pass the result to the next line in the
		 * chain
		 */
		CompletableFuture<ResultSet> insertUserFuture = checkEmailFuture.thenCompose(rs -> {
			final BoundStatement insertUser = createUser_insertUserPrepared.bind().setUUID("userid", userIdUUID)
					.setString("firstname", user.getFirstName()).setString("lastname", user.getLastName())
					.setString("email", user.getEmail()).setTimestamp("created_date", now);

			return FutureUtils.buildCompletableFuture(dseSession.executeAsync(insertUser));
		});

		/**
		 * thenAccept in the same thread pool (not using thenAcceptAsync())
		 */
		insertUserFuture.thenAccept(rs -> {
			try {
				if (rs != null) {
					/**
					 * Check to see if userInsert was applied. userId should be
					 * unique, if not, the insert should fail
					 */
					if (rs.wasApplied()) {
						LOGGER.debug("User id is unique, creating user");

						/**
						 * eventbus.post() for UserCreated below is located in
						 * the SuggestedVideos Service class within the handle()
						 * method. The UserCreated type triggers the handler and
						 * is responsible for adding data to our graph
						 * recommendation engine.
						 */
						user.setUserId(userIdUUID.toString());
						eventBus.post(user);

						LOGGER.debug("End creating user");

					} else {
						throw new Throwable("User ID already exists");
					}
				}

			} catch (Throwable t) {
				/*
				 * eventBus.post(new CassandraMutationError(request, t));
				 * responseObserver.onError(Status.INTERNAL.withCause(t).
				 * asRuntimeException());
				 */
				LOGGER.error(this.getClass().getName() + ".createUser() " + "Exception creating user : "
						+ mergeStackTrace(t));
			}
		});
		return userIdUUID.toString();
	}

	public String verifyCredentials(String email, String password) throws InterruptedException, ExecutionException {

		LOGGER.debug("------Start verifying user credentials------");
		
		final BoundStatement getUserCredentialsQuery = getUser_credentials.bind().setString("email", email);
		
		CompletableFuture<User> checkEmailFuture = FutureUtils
				.buildCompletableFuture(dseSession.executeAsync(getUserCredentialsQuery))
				.handleAsync((rs, ex) -> {
					User userUser = null; 
					try {
						
						if (rs == null) {
	                        return null;
	                    }
						
						Row row = rs.one();
						if(row!=null){
							userUser = new User();
							userUser.setEmail(row.getString("email"));
							userUser.setPassword(row.getString("password"));
							userUser.setUserId(row.getString("user_id"));
						}
							
					} catch (Throwable t) {
						final String message = t.getMessage();
						LOGGER.debug(this.getClass().getName() + ".check credentials() " + message);
						throw new RuntimeException(t);
					}
					return userUser;
				});
	
		User userUser =  checkEmailFuture.get();
		if(userUser == null || !HashUtils.isPasswordValid(password, userUser.getPassword())){
            final String errorMessage = "Email address or password are not correct.";
            LOGGER.error(errorMessage);
            return null;
		}
		return userUser.getUserId();
		
	}


    public User getUserUser(String userId) throws InterruptedException, ExecutionException{
    	final BoundStatement getUser = getUserUser_getUsersPrepared.bind()
                .setUUID("userid", UUID.fromString(userId));
    	CompletableFuture<ResultSet> selectUserFuture = FutureUtils.buildCompletableFuture(dseSession.executeAsync(getUser));
    	CompletableFuture<User> profileFuture = selectUserFuture.thenApplyAsync(rs ->{
    		Row row = rs.one();
    		User profile = new User();
    		profile.setUserId(row.getUUID("userid").toString());
    		profile.setFirstName(row.getString("firstname"));
    		profile.setLastName(row.getString("lastname"));
    		profile.setEmail(row.getString("email"));
    		profile.setCreatedAt(row.getTimestamp("created_date"));
    		return profile;
    	});
    	return profileFuture.get();
    }
}
