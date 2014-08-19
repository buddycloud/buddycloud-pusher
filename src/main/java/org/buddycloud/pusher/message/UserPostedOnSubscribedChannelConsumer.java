package org.buddycloud.pusher.message;

import java.util.List;

import org.apache.log4j.Logger;
import org.buddycloud.pusher.XMPPComponent;
import org.dom4j.Element;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;


public class UserPostedOnSubscribedChannelConsumer extends AbstractMessageConsumer {

	private static final Logger LOGGER = Logger.getLogger(AbstractMessageConsumer.class);
	
	public UserPostedOnSubscribedChannelConsumer(XMPPComponent xmppComponent) {
		super(xmppComponent);
	}

	@Override
	public void consume(Message message, List<String> recipients) {
		
		Element eventEl = message.getChildElement("event", ConsumerUtils.PUBSUB_EVENT_NS);
		Element itemsEl = eventEl.element("items");
		AtomEntry entry = AtomEntry.parse(itemsEl);
		if (entry == null) {
			return;
		}
		
		List<Affiliation> followers = null;
		try {
			followers = ConsumerUtils.getAffiliations(entry.getNode(), getXmppComponent());
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		for (Affiliation follower : followers) {
			userPosted(follower.getJid(), entry, recipients);
		}
	}

	private void userPosted(String followerJid, AtomEntry entry, List<String> recipients) {
		if (followerJid.equals(entry.getAuthor()) || recipients.contains(followerJid)) {
			return;
		}
		IQ iq = createIq(entry.getAuthor(), followerJid, 
				entry.getNode(), entry.getContent());
		try {
			IQ iqResponse = getXmppComponent().handleIQLoopback(iq);
			if (iqResponse.getError() == null) {
				recipients.add(followerJid);
			}
		} catch (Exception e) {
			LOGGER.warn(e);
		}		
	}
	
	private IQ createIq(String authorJid, String followerJid, 
			String channelJid, String content) {
		IQ iq = new IQ();
		Element queryEl = iq.getElement().addElement("query", 
				"http://buddycloud.com/pusher/userposted-subscribedchannel");
		queryEl.addElement("authorJid").setText(authorJid);
		queryEl.addElement("followerJid").setText(followerJid);
		queryEl.addElement("channel").setText(ConsumerUtils.getChannelAddress(channelJid));
		queryEl.addElement("postContent").setText(content);
        return iq;
	}
	
}
