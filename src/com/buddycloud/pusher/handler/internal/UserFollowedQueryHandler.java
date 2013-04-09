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
public class UserFollowedQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/userfollowed";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public UserFollowedQueryHandler(Properties properties, DataSource dataSource) {
		super(NAMESPACE, properties, dataSource);
	}

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element followerJidElement = queryElement.element("followerJid");
		Element channelElement = queryElement.element("channel");
		Element ownerJidElement = queryElement.element("ownerJid");
		
		if (followerJidElement == null || channelElement == null || ownerJidElement == null) {
			return XMPPUtils.error(iq,
					"You must provide the userJid, the channel and the channelOwner");
		}
		
		String followerJid = followerJidElement.getText();
		String ownerJid = ownerJidElement.getText();
		String channelJid = channelElement.getText();
		
		List<NotificationSettings> allNotificationSettings = NotificationUtils.getNotificationSettings(
				ownerJid, getDataSource());
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("FOLLOWER_JID", followerJid);
		tokens.put("OWNER_JID", ownerJid);
		tokens.put("CHANNEL_JID", channelJid);
		
		for (NotificationSettings notificationSettings : allNotificationSettings) {
			if (!notificationSettings.getFollowedMyChannel()) {
				getLogger().warn("User " + ownerJid + " won't receive follow notifications.");
				continue;
			}
			
			if (notificationSettings.getTarget() == null) {
				getLogger().warn("User " + ownerJid + " has no target registered.");
				continue;
			}

			Pusher pusher = Pushers.getInstance(getProperties()).get(notificationSettings.getType());
			pusher.push(notificationSettings.getTarget(), Event.FOLLOW, tokens);
		}
		
		return createResponse(iq, "User [" + followerJid + "] has followed channel [" + channelJid + "].");
	}
}
