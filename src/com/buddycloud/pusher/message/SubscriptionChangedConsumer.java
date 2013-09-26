package com.buddycloud.pusher.message;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.buddycloud.pusher.XMPPComponent;
import com.buddycloud.pusher.handler.internal.FollowRequestApprovedQueryHandler;
import com.buddycloud.pusher.handler.internal.FollowRequestDeniedQueryHandler;
import com.buddycloud.pusher.handler.internal.FollowRequestQueryHandler;
import com.buddycloud.pusher.handler.internal.UserUnfollowedQueryHandler;
import com.buddycloud.pusher.handler.internal.UserFollowedQueryHandler;

public class SubscriptionChangedConsumer extends AbstractMessageConsumer {

	private static final Logger LOGGER = Logger.getLogger(AbstractMessageConsumer.class);
	
	public SubscriptionChangedConsumer(XMPPComponent xmppComponent) {
		super(xmppComponent);
	}

	@Override
	public void consume(Message message, List<String> recipients) {
	
		/*
		<message from="channels.buddycloud.org" type="headline" to="pusher.buddycloud.com">
		<event xmlns="http://jabber.org/protocol/pubsub#event">
		  <subscription subscription="pending" jid="abmargb@buddycloud.org" node="/user/abmargb-tester@buddycloud.org/posts"/>
		  <affiliations>
		    <affiliation node="/user/abmargb-tester@buddycloud.org/posts" jid="abmargb@buddycloud.org" affiliation="publisher"/>
		  </affiliations>
		</event>
		</message>
        */
		
		Element eventEl = message.getChildElement("event", ConsumerUtils.PUBSUB_EVENT_NS);
		Element subscriptionEl = eventEl.element("subscription");
		
		if (subscriptionEl == null) {
			return;
		}
		
		String subscriptionState = subscriptionEl.attributeValue("subscription");
		String followerJid = subscriptionEl.attributeValue("jid");
		String channelJid = subscriptionEl.attributeValue("node");
		
		List<Affiliation> moderators = null;
		try {
			moderators = ConsumerUtils.getOwnersAndModerators(channelJid, getXmppComponent());
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		boolean isApproval = eventEl.element("affiliations") == null;
		
		for (Affiliation moderator : moderators) {
			subscriptionChanged(moderator.getJid(), followerJid, channelJid, 
					recipients, subscriptionState, isApproval);
		}
	}

	private void subscriptionChanged(String ownerJid, String followerJid, String channelJid, 
			List<String> recipients, String subscriptionState, boolean isApproval) {
		if (ownerJid.equals(followerJid)) {
			return;
		}
		if (recipients.contains(ownerJid) && !isApproval) {
			return;
		}
		if (recipients.contains(followerJid) && isApproval) {
			return;
		}
		
		IQ iq = createIq(ownerJid, followerJid, channelJid, 
				subscriptionState, isApproval);
		try {
			IQ iqResponse = getXmppComponent().handleIQLoopback(iq);
			if (iqResponse.getError() == null) {
				if (isApproval) {
					recipients.add(followerJid);
				} else {
					recipients.add(ownerJid);
				}
			}
		} catch (Exception e) {
			LOGGER.warn(e);
		}
	}
	
	private IQ createIq(String ownerJid, String followerJid, 
			String channelJid, String subscriptionState, boolean isApproval) {
        String ns = null;
        if (isApproval) {
        	if (subscriptionState.equals("subscribed")) {
        		ns = FollowRequestApprovedQueryHandler.NAMESPACE;
        	} else if (subscriptionState.equals("none")) {
        		ns = FollowRequestDeniedQueryHandler.NAMESPACE;
        	}
        } else {
        	if (subscriptionState.equals("subscribed")) {
        		ns = UserFollowedQueryHandler.NAMESPACE;
        	} else if (subscriptionState.equals("none")) {
        		ns = UserUnfollowedQueryHandler.NAMESPACE;
        	} else if (subscriptionState.equals("pending")) {
        		ns = FollowRequestQueryHandler.NAMESPACE;
        	}
        }
		
		IQ iq = new IQ();
		Element queryEl = iq.getElement().addElement("query", ns.toString());
		queryEl.addElement("followerJid").setText(followerJid);
		queryEl.addElement("ownerJid").setText(ownerJid);
		queryEl.addElement("channel").setText(ConsumerUtils.getChannelAddress(channelJid));
        return iq;
	}
	
}
