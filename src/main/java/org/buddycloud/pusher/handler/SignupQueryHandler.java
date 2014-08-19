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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.buddycloud.pusher.Pushers;
import org.buddycloud.pusher.Pusher.Event;
import org.buddycloud.pusher.db.DataSource;
import org.buddycloud.pusher.strategies.email.EmailPusher;
import org.buddycloud.pusher.utils.XMPPUtils;
import org.dom4j.Element;
import org.xmpp.packet.IQ;


/**
 * @author Abmar
 *
 */
public class SignupQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/signup";
	
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public SignupQueryHandler(Properties properties, DataSource dataSource) {
		super(NAMESPACE, properties, dataSource);
	}

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element emailElement = queryElement.element("email");
		
		if (emailElement == null) {
			return XMPPUtils.error(iq,
					"You must provide an email address", getLogger());
		}
		
		String jid = iq.getFrom().toBareJID();
		String emailAddress = emailElement.getText();
		
		insertSubscriber(jid, emailAddress);
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("NEW_USER_JID", jid);
		tokens.put("EMAIL", emailAddress);
		
		Pushers.getInstance(getProperties()).get(EmailPusher.TYPE).push(emailAddress, 
				Event.SIGNUP, tokens);
		
		return createResponse(iq, "User [" + jid + "] signed up.");
	}
	
	private void insertSubscriber(String jid, String email) {
		PreparedStatement statement = null;
		try {
			statement = getDataSource().prepareStatement(
					"INSERT INTO notification_settings(jid, target, type) values (?, ?, ?)", 
					jid, email, "email");
			statement.execute();
		} catch (SQLException e) {
			getLogger().error("Could not insert user [" + jid + ", " + email + "].", e);
			throw new RuntimeException(e);
		} finally {
			DataSource.close(statement);
		}
	}
}