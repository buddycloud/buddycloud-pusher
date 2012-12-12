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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.email.Email;
import com.buddycloud.pusher.email.EmailPusher;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 *
 */
public class SignupQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/signup";
	private static final String WELCOME_TEMPLATE = "welcome.tpl";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public SignupQueryHandler(Properties properties, DataSource dataSource, 
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
		Element emailElement = queryElement.element("email");
		
		if (jidElement == null || emailElement == null) {
			return XMPPUtils.error(iq,
					"You must provide the jid and the email", getLogger());
		}
		
		String fullJid = jidElement.getText();
		String emailAddress = emailElement.getText();
		
		String jid = fullJid.split("/")[0];
		insertSubscriber(jid, emailAddress);
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("NEW_USER_JID", jid);
		tokens.put("EMAIL", emailAddress);
		
		Email email = getEmailPusher().createEmail(tokens, WELCOME_TEMPLATE);
		getEmailPusher().push(email);
		
		return createResponse(iq, "User [" + jid + "] signed up.");
	}
	
	private void insertSubscriber(String jid, String email) {
		PreparedStatement statement = null;
		try {
			statement = getDataSource().prepareStatement(
					"INSERT INTO notification_settings(jid, email) values (?, ?)", 
					jid, email);
			statement.execute();
		} catch (SQLException e) {
			getLogger().error("Could not insert user [" + jid + ", " + email + "].", e);
			throw new RuntimeException(e);
		} finally {
			DataSource.close(statement);
		}
	}
}