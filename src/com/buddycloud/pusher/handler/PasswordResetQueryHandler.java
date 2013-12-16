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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.IQ.Type;

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
	@SuppressWarnings("unchecked")
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element usernameEl = queryElement.element("username");
		String username = usernameEl.getTextTrim();
		
		String[] splitUsername = username.split("@");
		String domain = splitUsername[1];
		
		IQ issueCommandIq = new IQ();
		issueCommandIq.setTo(domain);
		issueCommandIq.setFrom(iq.getTo());
		issueCommandIq.setType(org.xmpp.packet.IQ.Type.set);
		
		Element issueCommandEl = issueCommandIq.getElement().addElement("command", 
				"http://jabber.org/protocol/commands");
		issueCommandEl.addAttribute("action", "execute");
		issueCommandEl.addAttribute("node", "http://jabber.org/protocol/admin#change-user-password");
		
		IQ changeCommandIq = xmppComponent.syncIQ(issueCommandIq);
		
		if (changeCommandIq.getError() != null) {
			return createResponse(iq, changeCommandIq);
		}
		
		String randomPassword = RandomStringUtils.randomAlphanumeric(8);
		
		changeCommandIq.setType(Type.set);
		changeCommandIq.setTo(domain);
		changeCommandIq.setFrom(iq.getTo());
		
		Element xEl = changeCommandIq.getElement().element("command").element("x");
		List<Element> fields = xEl.elements("field");
		for (Element field : fields) {
			String var = field.attributeValue("var");
			if (var.equals("accountjid")) {
				field.addElement("value").setText(username);
			} else if (var.equals("password")) {
				field.addElement("value").setText(randomPassword);
			}
		}
		
		IQ changeCommandReplyIq = xmppComponent.syncIQ(changeCommandIq);
		
		if (changeCommandReplyIq.getError() == null) {
			List<NotificationSettings> emailNotificationSettings = NotificationUtils
					.getNotificationSettingsByType(username, "email", getDataSource());
			
			for (NotificationSettings notificationSettings : emailNotificationSettings) {
				
				String emailAddress = notificationSettings.getTarget();
				Map<String, String> tokens = new HashMap<String, String>();
				tokens.put("NEW_PASSWORD", randomPassword);
				tokens.put("EMAIL", emailAddress);
				tokens.put("USER_JID", username);
				
				Pushers.getInstance(getProperties()).get(EmailPusher.TYPE).push(
						emailAddress, Event.PASSWORD_RESET, tokens);
			}
			
		}
		
		return createResponse(iq, changeCommandReplyIq);
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
			PacketError pe = new PacketError(
					resetReplyIq.getError().getCondition(),
					resetReplyIq.getError().getType());
			result.setError(pe);
		}
		return result;
	}
}
