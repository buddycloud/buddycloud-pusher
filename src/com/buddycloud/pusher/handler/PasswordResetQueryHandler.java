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

import org.apache.commons.lang3.RandomStringUtils;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.NotificationSettings;
import com.buddycloud.pusher.Pusher.Event;
import com.buddycloud.pusher.Pushers;
import com.buddycloud.pusher.XMPPComponent;
import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.strategies.email.EmailPusher;
import com.buddycloud.pusher.utils.NotificationUtils;

/**
 * @author Abmar
 * 
 */
public class PasswordResetQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/password-reset";
	private final XMPPComponent xmppComponent;

	/**
	 * @param xmppComponent 
	 * @param namespace
	 * @param properties
	 */
	public PasswordResetQueryHandler(XMPPComponent xmppComponent, Properties properties,
			DataSource dataSource) {
		super(NAMESPACE, properties, dataSource);
		this.xmppComponent = xmppComponent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp
	 * .packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		String userJid = iq.getFrom().toBareJID();
		Element queryElement = iq.getElement().element("query");
		Element usernameEl = queryElement.element("username");
		String username = usernameEl.getTextTrim();
		
		String[] splitUsername = username.split("@");
		
		//TODO use XEP-0133 instead
		IQ resetIq = new IQ();
		resetIq.setFrom(username);
		resetIq.setTo(splitUsername[1]);
		resetIq.setType(org.xmpp.packet.IQ.Type.set);
		Element resetQueryEl = resetIq.getElement().addElement("query", "jabber:iq:register");
		resetQueryEl.addElement("username").setText(splitUsername[0]);
		
		String randomPassword = RandomStringUtils.randomAlphanumeric(8);
		resetQueryEl.addElement("password").setText(randomPassword);
		
		IQ resetReplyIq = xmppComponent.syncIQ(resetIq);
		
		if (resetReplyIq.getError() != null) {
			NotificationSettings notificationSettings = NotificationUtils
					.getNotificationSettingsByType(userJid, "email", getDataSource());
			
			String emailAddress = notificationSettings.getTarget();
			
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("NEW_PASSWORD", randomPassword);
			tokens.put("EMAIL", emailAddress);
			tokens.put("JID", userJid);
			
			Pushers.getInstance(getProperties()).get(EmailPusher.TYPE).push(
					emailAddress, Event.PASSWORD_RESET, tokens);
		}
		
		return createResponse(iq, resetReplyIq);
	}

	/**
	 * @param iq
	 * @param userJid
	 * @param notificationSettings
	 * @return
	 */
	private IQ createResponse(IQ iq, IQ resetReplyIq) {
		IQ result = IQ.createResultIQ(iq);
		result.setType(resetReplyIq.getType());
		result.getElement().addElement("query", getNamespace());
		if (resetReplyIq.getError() != null) {
			result.setError(resetReplyIq.getError());
		}
		return result;
	}
}
