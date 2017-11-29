package killrvideo.service;

import com.google.common.eventbus.Subscribe;

import killrvideo.entity.User;

public class UserCreatedSubscribe {
	
	@Subscribe
	public void userCreated(User user){
		System.out.println(user.getUserId() + " CREATED THIS USER " );
	}
}
