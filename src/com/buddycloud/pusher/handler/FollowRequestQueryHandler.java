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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.PusherSubmitter;
import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.email.EmailPusher;
import com.buddycloud.pusher.utils.UserUtils;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 *
 */
public class FollowRequestQueryHandler extends AbstractQueryHandler {

	private static final String NAMESPACE = "http://buddycloud.com/pusher/followrequest";
	private static final String FOLLOWREQUEST_TEMPLATE = "followrequest.tpl";
	
	/**
	 * @param namespace
	 * @param properties
	 */
	public FollowRequestQueryHandler(Properties properties, DataSource dataSource, 
			PusherSubmitter pusherSubmitter) {
		super(NAMESPACE, properties, dataSource, pusherSubmitter);
	}

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.AbstractQueryHandler#handleQuery(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleQuery(IQ iq) {
		Element queryElement = iq.getElement().element("query");
		Element jidElement = queryElement.element("userJid");
		Element channelElement = queryElement.element("channel");
		Element channelOwnerElement = queryElement.element("channelOwnerJid");
		
		if (jidElement == null || channelElement == null || channelOwnerElement == null) {
			return XMPPUtils.error(iq,
					"You must provide the userJid, the channel and the channelOwner", getLogger());
		}
		
		String userJid = jidElement.getText();
		String ownerJid = channelOwnerElement.getText();
		String ownerEmail = UserUtils.getUserEmail(ownerJid, getDataSource());
		String channelJid = channelElement.getText();
		
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("FIRST_PART_JID", userJid.split("@")[0]);
		tokens.put("FIRST_PART_OWNER_JID", ownerJid.split("@")[0]);
		tokens.put("CHANNEL_JID", channelJid);
		tokens.put("EMAIL", ownerEmail);
		
		EmailPusher pusher = new EmailPusher(getProperties(), tokens, FOLLOWREQUEST_TEMPLATE);
		getPusherSubmitter().submitPusher(pusher);
		
		return createResponse(iq, "User [" + userJid + "] sent a request to follow your channel [" + channelJid + "].");
	}
}
