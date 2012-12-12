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
public class UserPostedMentionQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/userposted-mention";
	private static final String USERPOSTED_TEMPLATE = "userposted-mention.tpl";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public UserPostedMentionQueryHandler(Properties properties, DataSource dataSource, 
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
		Element channelElement = queryElement.element("channel");
		Element mentionedJidElement = queryElement.element("mentionedJid");
		Element postContentElement = queryElement.element("postContent");
		
		if (authorJidElement == null || channelElement == null || mentionedJidElement == null) {
			return XMPPUtils.error(iq,
					"You must provide the userJid, the channel and the channelOwner", getLogger());
		}
		
		String authorJid = authorJidElement.getText();
		String mentionedJid = mentionedJidElement.getText();
		NotificationSettings notificationSettings = NotificationUtils.getNotificationSettings(
				mentionedJid, getDataSource());
		if (notificationSettings == null) {
			return XMPPUtils.error(iq,
					"User " + mentionedJid + " is not registered.");
		}
		
		String mentionedEmail = notificationSettings.getEmail();
		if (mentionedEmail == null) {
			return XMPPUtils.error(iq,
					"User " + mentionedJid + " has no email registered.");
		}
		
		if (!notificationSettings.getPostMentionedMe()) {
			return XMPPUtils.error(iq,
					"User " + mentionedJid + " won't receive mention notifications.");
		}
		
		String channelJid = channelElement.getText();
		String postContent = postContentElement.getText();
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("AUTHOR_JID", authorJid);
		tokens.put("MENTIONED_JID", mentionedJid);
		tokens.put("CHANNEL_JID", channelJid);
		tokens.put("CONTENT", postContent);
		tokens.put("EMAIL", mentionedEmail);
		
		Email email = getEmailPusher().createEmail(tokens, USERPOSTED_TEMPLATE);
		getEmailPusher().push(email);
		
		return createResponse(iq, "User [" + authorJid + "] mentioned [" + mentionedJidElement + "] " +
				"on channel [" + channelJid + "].");
	}
}
