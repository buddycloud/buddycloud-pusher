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
package com.buddycloud.pusher.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.dom4j.Element;

import com.buddycloud.pusher.NotificationSettings;
import com.buddycloud.pusher.db.DataSource;

/**
 * @author Abmar
 *
 */
public class NotificationUtils {

	public static NotificationSettings getNotificationSettings(String jid, DataSource dataSource) {
		PreparedStatement statement = null;
		try {
			statement = dataSource.prepareStatement(
					"SELECT * FROM notification_settings WHERE jid=?", 
					jid);
			ResultSet resultSet = statement.getResultSet();
			if (resultSet.next()) {
				NotificationSettings notificationSettings = new NotificationSettings();
				notificationSettings.setPostAfterMe(resultSet.getBoolean("post_after_me"));
				notificationSettings.setPostMentionedMe(resultSet.getBoolean("post_mentioned_me"));
				notificationSettings.setPostOnMyChannel(resultSet.getBoolean("post_on_my_channel"));
				notificationSettings.setPostOnSubscribedChannel(resultSet.getBoolean("post_on_subscribed_channel"));
				notificationSettings.setFollowedMyChannel(resultSet.getBoolean("follow_my_channel"));
				return notificationSettings;
			}
			return null;
		} catch (SQLException e) {
			UserUtils.LOGGER.error("Could not get notification settings from user [" + jid + "].", e);
			throw new RuntimeException(e);
		} finally {
			DataSource.close(statement);
		}
	}

	public static NotificationSettings updateNotificationSettings(String jid, DataSource dataSource, 
			NotificationSettings notificationSettings) {
		
		NotificationSettings updatedNotificationsSettings = getNotificationSettings(jid, dataSource);
		if (updatedNotificationsSettings == null) {
			updatedNotificationsSettings = notificationSettings;
		} else {
			NotificationUtils.updateNotificationSettings(notificationSettings, updatedNotificationsSettings);
		}
		
		PreparedStatement deleteStatement = null;
		PreparedStatement insertStatement = null;
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			deleteStatement = dataSource.prepareStatement(
					"DELETE FROM notification_settings WHERE jid=?", 
					connection, jid);
			deleteStatement.executeUpdate();
			
			insertStatement = dataSource.prepareStatement(
					"INSERT INTO notification_settings(jid, post_after_me, post_mentioned_me, post_on_my_channel, " +
					"post_on_subscribed_channel, follow_my_channel) values (?, ?, ?, ?, ?, ?)", connection, 
					jid, updatedNotificationsSettings.getPostAfterMe(), 
					updatedNotificationsSettings.getPostMentionedMe(), 
					updatedNotificationsSettings.getPostOnMyChannel(), 
					updatedNotificationsSettings.getPostOnSubscribedChannel(), 
					updatedNotificationsSettings.getFollowedMyChannel());
			insertStatement.executeUpdate();
			
			connection.commit();
			
			return updatedNotificationsSettings;
		} catch (SQLException e) {
			UserUtils.LOGGER.error("Could not update notification settings from user [" + jid + "].", e);
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					UserUtils.LOGGER.error("Could not rollback update notification settings operation.", e1);
				}
			}
			throw new RuntimeException(e);
		} finally {
			if (deleteStatement != null) {
				try {
					deleteStatement.close();
				} catch (SQLException e) {
					UserUtils.LOGGER.error(e);
				}
			}
			if (insertStatement != null) {
				try {
					insertStatement.close();
				} catch (SQLException e) {
					UserUtils.LOGGER.error(e);
				}
			}
			try {
				connection.close();
			} catch (SQLException e) {
				UserUtils.LOGGER.error(e);
			}
		}
	}

	private static void updateNotificationSettings(
			NotificationSettings notificationSettings,
			NotificationSettings oldNotificationsSettings) {
		
		if (notificationSettings.getPostAfterMe() != null) {
			oldNotificationsSettings.setPostAfterMe(
					notificationSettings.getPostAfterMe());
		}
		if (notificationSettings.getPostMentionedMe() != null) {
			oldNotificationsSettings.setPostMentionedMe(
					notificationSettings.getPostMentionedMe());
		}
		if (notificationSettings.getPostOnMyChannel() != null) {
			oldNotificationsSettings.setPostOnMyChannel(
					notificationSettings.getPostOnMyChannel());
		}
		if (notificationSettings.getPostOnSubscribedChannel() != null) {
			oldNotificationsSettings.setPostOnSubscribedChannel(
					notificationSettings.getPostOnSubscribedChannel());
		}
		if (notificationSettings.getFollowedMyChannel() != null) {
			oldNotificationsSettings.setFollowedMyChannel(
					notificationSettings.getFollowedMyChannel());
		}
	}

	/**
	 * @param queryElement 
	 * @param notificationSettings
	 * @return
	 */
	public static void appendXML(Element queryElement, NotificationSettings notificationSettings) {
		Element settingsEl = queryElement.addElement("notificationsettings");
		settingsEl.addElement("postAfterMe").setText(
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

	public static NotificationSettings fromXML(Element settingsEl) {
		NotificationSettings notificationSettings = new NotificationSettings();
		
		Boolean postAfterMe = NotificationUtils.getBoolean(settingsEl, "postAfterMe");
		Boolean postMentionedMe = NotificationUtils.getBoolean(settingsEl, "postMentionedMe");
		Boolean postOnMyChannel = NotificationUtils.getBoolean(settingsEl, "postOnMyChannel");
		Boolean postOnSubscribedChannel = NotificationUtils.getBoolean(settingsEl, "postOnSubscribedChannel");
		Boolean followMyChannel = NotificationUtils.getBoolean(settingsEl, "followMyChannel");
		
		notificationSettings.setPostAfterMe(postAfterMe);
		notificationSettings.setPostMentionedMe(postMentionedMe);
		notificationSettings.setPostOnMyChannel(postOnMyChannel);
		notificationSettings.setPostOnSubscribedChannel(postOnSubscribedChannel);
		notificationSettings.setFollowedMyChannel(followMyChannel);
		return notificationSettings;
	}

	private static Boolean getBoolean(Element settingsEl, String key) {
		Element el = settingsEl.element(key);
		if (el == null) {
			return null;
		}
		return Boolean.valueOf(el.getText());
	}

}
