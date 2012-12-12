package com.buddycloud.pusher.message;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.buddycloud.pusher.XMPPComponent;

public class UserFollowedConsumer extends AbstractMessageConsumer {

	private static final Logger LOGGER = Logger.getLogger(AbstractMessageConsumer.class);
	
	public UserFollowedConsumer(XMPPComponent xmppComponent) {
		super(xmppComponent);
	}

	@Override
	public void consume(Message message, List<String> recipients) {
	
		Element eventEl = message.getChildElement("event", ConsumerUtils.PUBSUB_EVENT_NS);
		Element subscriptionEl = eventEl.element("subscription");
		
		if (subscriptionEl == null) {
			return;
		}
		
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
			newFollower(owner.getJid(), followerJid, channelJid, recipients);
		}
	}

	private void newFollower(String ownerJid, String followerJid, String channelJid, List<String> recipients) {
		if (ownerJid.equals(followerJid) || recipients.contains(ownerJid)) {
			return;
		}
		
		IQ iq = createIq(ownerJid, followerJid, channelJid);
		try {
			IQ iqResponse = getXmppComponent().handleIQLoopback(iq);
			if (iqResponse.getError() == null) {
				recipients.add(ownerJid);
			}
		} catch (Exception e) {
			LOGGER.warn(e);
		}
	}
	
	private IQ createIq(String ownerJid, String followerJid, String channelJid) {
        IQ iq = new IQ();
		Element queryEl = iq.getElement().addElement("query", 
				"http://buddycloud.com/pusher/userfollowed");
		queryEl.addElement("followerJid").setText(followerJid);
		queryEl.addElement("ownerJid").setText(ownerJid);
		queryEl.addElement("channel").setText(ConsumerUtils.getChannelAddress(channelJid));
        return iq;
	}
	
}
