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

/**
 * @author Abmar
 *
 */
public class NotificationSettings {
	
	private String email;
	private String jid;
	private Boolean postAfterMe = true;
	private Boolean postMentionedMe = true;
	private Boolean postOnMyChannel = true;
	private Boolean postOnSubscribedChannel = false;
	private Boolean followedMyChannel = true;
	private Boolean followRequest = true;
	
	/**
	 * @return the postAfterMe
	 */
	public Boolean getPostAfterMe() {
		return postAfterMe;
	}
	/**
	 * @param postAfterMe the postAfterMe to set
	 */
	public void setPostAfterMe(Boolean postAfterMe) {
		this.postAfterMe = postAfterMe;
	}
	/**
	 * @return the postMentionedMe
	 */
	public Boolean getPostMentionedMe() {
		return postMentionedMe;
	}
	/**
	 * @param postMentionedMe the postMentionedMe to set
	 */
	public void setPostMentionedMe(Boolean postMentionedMe) {
		this.postMentionedMe = postMentionedMe;
	}
	/**
	 * @return the postOnMyChannel
	 */
	public Boolean getPostOnMyChannel() {
		return postOnMyChannel;
	}
	/**
	 * @param postOnMyChannel the postOnMyChannel to set
	 */
	public void setPostOnMyChannel(Boolean postOnMyChannel) {
		this.postOnMyChannel = postOnMyChannel;
	}
	/**
	 * @return the postOnSubscribedChannel
	 */
	public Boolean getPostOnSubscribedChannel() {
		return postOnSubscribedChannel;
	}
	/**
	 * @param postOnSubscribedChannel the postOnSubscribedChannel to set
	 */
	public void setPostOnSubscribedChannel(Boolean postOnSubscribedChannel) {
		this.postOnSubscribedChannel = postOnSubscribedChannel;
	}
	/**
	 * @return the followedMyChannel
	 */
	public Boolean getFollowedMyChannel() {
		return followedMyChannel;
	}
	/**
	 * @param followedMyChannel the followedMyChannel to set
	 */
	public void setFollowedMyChannel(Boolean followedMyChannel) {
		this.followedMyChannel = followedMyChannel;
	}
	
	public Boolean getFollowRequest() {
		return followRequest;
	}
	
	public void setFollowRequest(Boolean followRequest) {
		this.followRequest = followRequest;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public void setJid(String jid) {
		this.jid = jid;
	}
	
	public String getJid() {
		return jid;
	}
}
