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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.email.EmailPusher;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 *
 */
public class DeleteUserQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/deleteuser";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public DeleteUserQueryHandler(Properties properties, DataSource dataSource, 
			 EmailPusher emailPusher) {
		super(NAMESPACE, properties, dataSource, emailPusher);
	}

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element jidElement = queryElement.element("jid");
		
		if (jidElement == null) {
			return XMPPUtils.error(iq, "You must provide the user's jid", getLogger());
		}
		
		String jid = jidElement.getText();
		deleteSubscriber(jid);
		
		return createResponse(iq, "User [" + jid + "] was deleted.");
	}

	private void deleteSubscriber(String jid) {
		PreparedStatement statement = null;
		try {
			statement = getDataSource().prepareStatement(
					"DELETE FROM notification_settings WHERE jid=?", 
					jid);
			statement.execute();
		} catch (SQLException e) {
			getLogger().error("Could not delete user [" + jid + "].", e);
			throw new RuntimeException(e);
		} finally {
			DataSource.close(statement);
		}
	}
}
