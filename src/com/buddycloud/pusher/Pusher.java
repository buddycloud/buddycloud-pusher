/*
 * Copyright 2011 buddycloud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.buddycloud.pusher;

import java.util.Map;

/**
 * @author Abmar
 *
 */
public interface Pusher {

	enum Event {SIGNUP, FOLLOW_REQUEST, FOLLOW, UNFOLLOW, POST_AFTER_MY_POST, 
		MENTION, POST_ON_MY_CHANNEL, POST_ON_SUBSCRIBED_CHANNEL} 
	
	void push(String target, Event event, Map<String, String> tokens);
	
	Map<String, String> getMetadata();
	
}
