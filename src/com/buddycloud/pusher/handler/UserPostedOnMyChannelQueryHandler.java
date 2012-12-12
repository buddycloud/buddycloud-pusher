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
package com.buddycloud.pusher.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.NotificationSettings;
import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.email.Email;
import com.buddycloud.pusher.email.EmailPusher;
import com.buddycloud.pusher.utils.NotificationUtils;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 *
 */
public class UserPostedOnMyChannelQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/userposted-mychannel";
	private static final String USERPOSTED_TEMPLATE = "userposted-mychannel.tpl";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public UserPostedOnMyChannelQueryHandler(Properties properties, DataSource dataSource, 
			EmailPusher emailPusher) {
		super(NAMESPACE, properties, dataSource, emailPusher);
	}

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element authorJidElement = queryElement.element("authorJid");
		Element ownerJidElement = queryElement.element("ownerJid");
		Element channelElement = queryElement.element("channel");
		Element postContentElement = queryElement.element("postContent");
		
		if (authorJidElement == null || channelElement == null || ownerJidElement == null) {
			return XMPPUtils.error(iq,
					"You must provide the userJid, the channel and the channelOwner", getLogger());
		}
		
		String authorJid = authorJidElement.getText();
		String ownerJid = ownerJidElement.getText();
		NotificationSettings notificationSettings = NotificationUtils.getNotificationSettings(
				ownerJid, getDataSource());
		if (notificationSettings == null) {
			return XMPPUtils.error(iq,
					"User " + ownerJid + " is not registered.");
		}
		
		String ownerEmail = notificationSettings.getEmail();
		if (ownerEmail == null) {
			return XMPPUtils.error(iq,
					"User " + ownerJid + " has no email registered.");
		}
		
		if (!notificationSettings.getPostOnMyChannel()) {
			return XMPPUtils.error(iq,
					"User " + ownerJid + " won't receive post on own channels notifications.");
		}
		
		String channelJid = channelElement.getText();
		String postContent = postContentElement.getText();
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("AUTHOR_JID", authorJid);
		tokens.put("OWNER_JID", ownerJid);
		tokens.put("CHANNEL_JID", channelJid);
		tokens.put("CONTENT", postContent);
		tokens.put("EMAIL", ownerEmail);
		
		Email email = getEmailPusher().createEmail(tokens, USERPOSTED_TEMPLATE);
		getEmailPusher().push(email);
		
		return createResponse(iq, "User [" + authorJid + "] has posted on channel [" + channelJid + "].");
	}
}
