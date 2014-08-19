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
package org.buddycloud.pusher.handler.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.buddycloud.pusher.NotificationSettings;
import org.buddycloud.pusher.Pusher;
import org.buddycloud.pusher.Pushers;
import org.buddycloud.pusher.Pusher.Event;
import org.buddycloud.pusher.db.DataSource;
import org.buddycloud.pusher.handler.AbstractQueryHandler;
import org.buddycloud.pusher.utils.NotificationUtils;
import org.buddycloud.pusher.utils.XMPPUtils;
import org.dom4j.Element;
import org.xmpp.packet.IQ;


/**
 * @author Abmar
 *
 */
public class UserPostedOnSubscribedChannelQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/userposted-subscribedchannel";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public UserPostedOnSubscribedChannelQueryHandler(Properties properties, DataSource dataSource) {
		super(NAMESPACE, properties, dataSource);
	}

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element authorJidElement = queryElement.element("authorJid");
		Element channelElement = queryElement.element("channel");
		Element followerJidElement = queryElement.element("followerJid");
		Element postContentElement = queryElement.element("postContent");
		
		if (authorJidElement == null || channelElement == null || followerJidElement == null) {
			return XMPPUtils.error(iq,
					"You must provide the userJid, the channel and the channelOwner", getLogger());
		}
		
		String authorJid = authorJidElement.getText();
		String followerJid = followerJidElement.getText();
		String channelJid = channelElement.getText();
		String postContent = postContentElement.getText();
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("AUTHOR_JID", authorJid);
		tokens.put("FOLLOWER_JID", followerJid);
		tokens.put("CHANNEL_JID", channelJid);
		tokens.put("CONTENT", postContent);
		
		List<NotificationSettings> allNotificationSettings = NotificationUtils.getNotificationSettings(
				followerJid, getDataSource());
		
		for (NotificationSettings notificationSettings : allNotificationSettings) {
			if (!notificationSettings.getPostOnSubscribedChannel()) {
				getLogger().warn("User " + followerJid + " won't receive post on following channels notifications.");
				continue;
			}
			
			if (notificationSettings.getTarget() == null) {
				getLogger().warn("User " + followerJid + " has no target registered.");
				continue;
			}

			Pusher pusher = Pushers.getInstance(getProperties()).get(notificationSettings.getType());
			pusher.push(notificationSettings.getTarget(), Event.POST_ON_SUBSCRIBED_CHANNEL, tokens);
		}
		
		return createResponse(iq, "User [" + authorJid + "] has posted on channel [" + channelJid + "].");
	}
}
