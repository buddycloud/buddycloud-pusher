package com.buddycloud.pusher.message;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.buddycloud.pusher.XMPPComponent;
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
		  <subscription xmlns="" subscription="pending" jid="abmargb@buddycloud.org" node="/user/abmargb-tester@buddycloud.org/posts"/>
		  <affiliations xmlns="">
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
		
		List<Affiliation> owners = null;
		try {
			owners = ConsumerUtils.getOwners(channelJid, getXmppComponent());
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		for (Affiliation owner : owners) {
			subscriptionChanged(owner.getJid(), followerJid, channelJid, 
					recipients, subscriptionState);
		}
	}

	private void subscriptionChanged(String ownerJid, String followerJid, String channelJid, 
			List<String> recipients, String subscriptionState) {
		if (ownerJid.equals(followerJid) || recipients.contains(ownerJid)) {
			return;
		}
		
		IQ iq = createIq(ownerJid, followerJid, channelJid, subscriptionState);
		try {
			IQ iqResponse = getXmppComponent().handleIQLoopback(iq);
			if (iqResponse.getError() == null) {
				recipients.add(ownerJid);
			}
		} catch (Exception e) {
			LOGGER.warn(e);
		}
	}
	
	private IQ createIq(String ownerJid, String followerJid, 
			String channelJid, String subscriptionState) {
        String ns = null;
        if (subscriptionState.equals("subscribed")) {
        	ns = UserFollowedQueryHandler.NAMESPACE;
        } else if (subscriptionState.equals("none")) {
        	ns = UserUnfollowedQueryHandler.NAMESPACE;
        } if (subscriptionState.equals("pending")) {
        	ns = FollowRequestQueryHandler.NAMESPACE;
        }
		
		IQ iq = new IQ();
		Element queryEl = iq.getElement().addElement("query", ns.toString());
		queryEl.addElement("followerJid").setText(followerJid);
		queryEl.addElement("ownerJid").setText(ownerJid);
		queryEl.addElement("channel").setText(ConsumerUtils.getChannelAddress(channelJid));
        return iq;
	}
	
}
