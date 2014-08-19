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
package org.buddycloud.pusher.handler;

import java.util.Properties;

import org.buddycloud.pusher.NotificationSettings;
import org.buddycloud.pusher.db.DataSource;
import org.buddycloud.pusher.utils.NotificationUtils;
import org.dom4j.Element;
import org.xmpp.packet.IQ;


/**
 * @author Abmar
 * 
 */
public class SetNotificationSettingsQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/notification-settings";

	/**
	 * @param namespace
	 * @param properties
	 */
	public SetNotificationSettingsQueryHandler(Properties properties,
			DataSource dataSource) {
		super(NAMESPACE, properties, dataSource);
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
		Element settingsEl = queryElement.element("notificationSettings");
		
		NotificationSettings notificationSettings = NotificationUtils.fromXML(settingsEl);
		NotificationSettings updatedNotificationSettings = 
				NotificationUtils.updateNotificationSettings(userJid, 
						notificationSettings.getType(), notificationSettings.getTarget(), 
						getDataSource(), notificationSettings);
		
		return createResponse(iq, userJid, updatedNotificationSettings);
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
