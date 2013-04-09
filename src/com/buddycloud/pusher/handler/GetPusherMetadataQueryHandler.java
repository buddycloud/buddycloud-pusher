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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.Pusher;
import com.buddycloud.pusher.Pushers;
import com.buddycloud.pusher.db.DataSource;

/**
 * @author Abmar
 * 
 */
public class GetPusherMetadataQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/metadata";

	/**
	 * @param namespace
	 * @param properties
	 */
	public GetPusherMetadataQueryHandler(Properties properties,
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
		
		Pusher pusher = Pushers.getInstance(getProperties()).get(typeEl.getText());
		
		return createResponse(iq, userJid, pusher.getMetadata());
	}

	/**
	 * @param iq
	 * @param userJid
	 * @param metadata
	 * @return
	 */
	private IQ createResponse(IQ iq, String userJid,
			Map<String, String> metadata) {
		IQ result = IQ.createResultIQ(iq);
		Element queryElement = result.getElement().addElement("query", getNamespace());
		for (Entry<String, String> entry : metadata.entrySet()) {
			queryElement.addElement(entry.getKey()).setText(entry.getValue());
		}
		return result;
	}
}
