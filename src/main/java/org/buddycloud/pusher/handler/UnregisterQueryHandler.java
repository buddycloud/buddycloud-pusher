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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.buddycloud.pusher.db.DataSource;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.PacketError.Condition;


/**
 * @author Abmar
 * 
 */
public class UnregisterQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "jabber:iq:register";

	/**
	 * @param xmppComponent 
	 * @param namespace
	 * @param properties
	 */
	public UnregisterQueryHandler(Properties properties, DataSource dataSource) {
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
		Element queryElement = iq.getElement().element("query");
		Element removeEl = queryElement.element("remove");
		
		if (removeEl == null) {
			return createFeatureNotImplementedError(iq);
		}
		
		Element typeEl = removeEl.element("type");
		Element targetEl = removeEl.element("target");
		
		String from = iq.getFrom().toBareJID();
		try {
			removeSubscriber(from, 
					typeEl == null ? null : typeEl.getText(), 
					targetEl == null ? null : targetEl.getText());
		} catch (Exception e) {
			return createInternalServerError(iq);
		}
		
		return IQ.createResultIQ(iq);
	}
	
	private void removeSubscriber(String jid, String type, 
			String target) throws SQLException {
		PreparedStatement statement = null;
		try {
			List<String> params = new LinkedList<String>();
			StringBuilder stString = new StringBuilder();
			stString.append("DELETE FROM notification_settings WHERE jid = ?");
			params.add(jid);
			if (type != null) {
				stString.append(" AND type = ?");
				params.add(type);
			}
			if (target != null) {
				stString.append(" AND target = ?");
				params.add(target);
			}
			statement = getDataSource().prepareStatement(
					stString.toString(), params.toArray());
			statement.execute();
		} catch (SQLException e) {
			getLogger().error("Could not delete user [" + jid + "].", e);
			throw e;
		} finally {
			DataSource.close(statement);
		}
	}

	private IQ createFeatureNotImplementedError(IQ iq) {
		IQ result = IQ.createResultIQ(iq);
		result.setType(Type.error);
		PacketError pe = new PacketError(
				Condition.feature_not_implemented,
				org.xmpp.packet.PacketError.Type.cancel);
		result.setError(pe);
		return result;
	}
	
	private IQ createInternalServerError(IQ iq) {
		IQ result = IQ.createResultIQ(iq);
		result.setType(Type.error);
		PacketError pe = new PacketError(
				Condition.internal_server_error,
				org.xmpp.packet.PacketError.Type.cancel);
		result.setError(pe);
		return result;
	}
}
