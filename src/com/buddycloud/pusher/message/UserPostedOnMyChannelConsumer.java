package com.buddycloud.pusher.message;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.buddycloud.pusher.XMPPComponent;

public class UserPostedOnMyChannelConsumer extends AbstractMessageConsumer {

	private static final Logger LOGGER = Logger.getLogger(AbstractMessageConsumer.class);
	
	public UserPostedOnMyChannelConsumer(XMPPComponent xmppComponent) {
		super(xmppComponent);
	}

	@Override
	public void consume(Message message) {
		
		Element eventEl = message.getChildElement("event", ConsumerUtils.PUBSUB_EVENT_NS);
		Element itemsEl = eventEl.element("items");
		AtomEntry entry = AtomEntry.parse(itemsEl);
		if (entry == null) {
			return;
		}
		
		List<Affiliation> owners = null;
		try {
			owners = ConsumerUtils.getOwners(entry.getNode(), getXmppComponent());
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		for (Affiliation owner : owners) {
			userPosted(owner.getJid(), entry);
		}
	}

	private void userPosted(String ownerJid, AtomEntry entry) {
		IQ iq = createIq(entry.getAuthor(), ownerJid, 
				entry.getNode(), entry.getContent());
		try {
			getXmppComponent().handleIQLoopback(iq);
		} catch (Exception e) {
			LOGGER.warn(e);
		}		
	}
	
	private IQ createIq(String authorJid, String ownerJid, 
			String channelJid, String content) {
		IQ iq = new IQ();
		Element queryEl = iq.getElement().addElement("query", 
				"http://buddycloud.com/pusher/userposted-mychannel");
		queryEl.addElement("authorJid").setText(authorJid);
		queryEl.addElement("ownerJid").setText(ownerJid);
		queryEl.addElement("channel").setText(channelJid);
		queryEl.addElement("postContent").setText(content);
        return iq;
	}
}
