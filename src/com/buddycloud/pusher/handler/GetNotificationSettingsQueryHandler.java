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

import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.NotificationSettings;
import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.email.EmailPusher;
import com.buddycloud.pusher.utils.NotificationUtils;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 * 
 */
public class GetNotificationSettingsQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/notification-settings";

	/**
	 * @param namespace
	 * @param properties
	 */
	public GetNotificationSettingsQueryHandler(Properties properties,
			DataSource dataSource, EmailPusher emailPusher) {
		super(NAMESPACE, properties, dataSource, emailPusher);
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
		NotificationSettings notificationSettings = NotificationUtils
				.getNotificationSettings(userJid, getDataSource());
		if (notificationSettings == null) {
			return XMPPUtils.error(iq, 
					"The user [" + userJid + "] is not registered on the Pusher.", getLogger());
		}
		return createResponse(iq, userJid, notificationSettings);
	}

	/**
	 * @param iq
	 * @param userJid
	 * @param notificationSettings
	 * @return
	 */
	private IQ createResponse(IQ iq, String userJid,
			NotificationSettings notificationSettings) {
		IQ result = IQ.createResultIQ(iq);
		Element queryElement = result.getElement().addElement("query", getNamespace());
		NotificationUtils.appendXML(queryElement, notificationSettings);
		return result;
	}
}
