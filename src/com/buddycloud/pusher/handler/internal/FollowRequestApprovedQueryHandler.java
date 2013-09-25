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
package com.buddycloud.pusher.handler.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.NotificationSettings;
import com.buddycloud.pusher.Pusher;
import com.buddycloud.pusher.Pusher.Event;
import com.buddycloud.pusher.Pushers;
import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.handler.AbstractQueryHandler;
import com.buddycloud.pusher.utils.NotificationUtils;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 *
 */
public class FollowRequestApprovedQueryHandler extends AbstractQueryHandler {

	public static final String NAMESPACE = "http://buddycloud.com/pusher/followrequestapproved";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public FollowRequestApprovedQueryHandler(Properties properties, DataSource dataSource) {
		super(NAMESPACE, properties, dataSource);
	}

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element followerElement = queryElement.element("followerJid");
		Element channelElement = queryElement.element("channel");
		Element channelOwnerElement = queryElement.element("ownerJid");
		
		if (followerElement == null || channelElement == null || channelOwnerElement == null) {
			return XMPPUtils.error(iq,
					"You must provide the userJid, the channel and the channelOwner", getLogger());
		}
		
		String followerJid = followerElement.getText();
		String ownerJid = channelOwnerElement.getText();
		String channelJid = channelElement.getText();
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("FOLLOWER_JID", followerJid);
		tokens.put("OWNER_JID", ownerJid);
		tokens.put("CHANNEL_JID", channelJid);

		List<NotificationSettings> allNotificationSettings = NotificationUtils.getNotificationSettings(
				followerJid, getDataSource());
		
		for (NotificationSettings notificationSettings : allNotificationSettings) {
			if (!notificationSettings.getFollowRequest()) {
				getLogger().warn("User " + followerJid + " won't receive follow request notifications.");
				continue;
			}
			
			if (notificationSettings.getTarget() == null) {
				getLogger().warn("User " + followerJid + " has no target registered.");
				continue;
			}
			
			Pusher pusher = Pushers.getInstance(getProperties()).get(notificationSettings.getType());
			pusher.push(notificationSettings.getTarget(), Event.FOLLOW_REQUEST_APPROVED, tokens);
		}
		
		return createResponse(iq, "User [" + followerJid + "] had its request to " +
				"follow channel [" + channelJid + "] approved.");
	}
}
