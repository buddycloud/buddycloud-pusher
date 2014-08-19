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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
public class GetNotificationSettingsQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/notification-settings";

	/**
	 * @param namespace
	 * @param properties
	 */
	public GetNotificationSettingsQueryHandler(Properties properties,
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
		Element typeEl = queryElement.element("type");
		Element targetEl = queryElement.element("target");
		
		List<NotificationSettings> notificationSettingsByType = null;
		
		if (targetEl != null) {
			NotificationSettings notificationSettings = NotificationUtils
					.getNotificationSettingsByType(userJid, typeEl.getText(),
							targetEl.getText(), getDataSource());
			notificationSettingsByType = notificationSettings == null ? 
					new LinkedList<NotificationSettings>() : Arrays.asList(notificationSettings);
		} else {
			notificationSettingsByType = 
					NotificationUtils.getNotificationSettingsByType(userJid, 
							typeEl.getText(), getDataSource());
		}
		
		return createResponse(iq, userJid, notificationSettingsByType);
	}

	/**
	 * @param iq
	 * @param userJid
	 * @param notificationSettings
	 * @return
	 */
	private IQ createResponse(IQ iq, String userJid,
			List<NotificationSettings> allNotificationSettings) {
		IQ result = IQ.createResultIQ(iq);
		Element queryElement = result.getElement().addElement("query", getNamespace());
		for (NotificationSettings notificationSettings : allNotificationSettings) {
			NotificationUtils.appendXML(queryElement, notificationSettings);
		}
		return result;
	}
}
