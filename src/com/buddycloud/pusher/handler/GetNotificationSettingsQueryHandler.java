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
import com.buddycloud.pusher.PusherSubmitter;
import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.utils.UserUtils;

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
			DataSource dataSource, PusherSubmitter pusherSubmitter) {
		super(NAMESPACE, properties, dataSource, pusherSubmitter);
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
		NotificationSettings notificationSettings = UserUtils
				.getNotificationSettings(userJid, getDataSource());
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
		appendXML(queryElement, notificationSettings);
		return result;
	}

	/**
	 * @param queryElement 
	 * @param notificationSettings
	 * @return
	 */
	private static void appendXML(Element queryElement, NotificationSettings notificationSettings) {
		Element settingsEl = queryElement.addElement("notificationsettings");
		settingsEl.addElement("postAfterMine").setText(
				notificationSettings.getPostAfterMe().toString());
		settingsEl.addElement("postMentionedMe").setText(
				notificationSettings.getPostMentionedMe().toString());
		settingsEl.addElement("postOnMyChannel").setText(
				notificationSettings.getPostOnMyChannel().toString());
		settingsEl.addElement("postOnSubscribedChannel").setText(
				notificationSettings.getPostOnSubscribedChannel().toString());
		settingsEl.addElement("followMyChannel").setText(
				notificationSettings.getFollowedMyChannel().toString());
	}
}
