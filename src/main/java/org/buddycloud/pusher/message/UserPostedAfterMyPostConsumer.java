package org.buddycloud.pusher.message;

import java.util.List;

import org.apache.log4j.Logger;
import org.buddycloud.pusher.XMPPComponent;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;


public class UserPostedAfterMyPostConsumer extends AbstractMessageConsumer {

	private static final Logger LOGGER = Logger.getLogger(UserPostedAfterMyPostConsumer.class);
	
	public UserPostedAfterMyPostConsumer(XMPPComponent xmppComponent) {
		super(xmppComponent);
	}

	@Override
	public void consume(Message message, List<String> recipients) {
		
		Element eventEl = message.getChildElement("event", ConsumerUtils.PUBSUB_EVENT_NS);
		Element itemsEl = eventEl.element("items");
		
		AtomEntry entry = AtomEntry.parse(itemsEl);
		if (entry == null || entry.getInReplyTo() == null) {
			return;
		}
		
		IQ iq = ConsumerUtils.getSingleItem(entry.getInReplyTo(), entry.getNode(), 
				getXmppComponent());
		if (iq == null) {
			return;
		}
		
		Element replyItemsEl = iq.getChildElement().element("items");
		if (replyItemsEl == null) {
			return;
		}
		
		AtomEntry refEntry = AtomEntry.parse(replyItemsEl);
		postAfterMe(entry.getAuthor(), refEntry.getAuthor(), 
				entry.getNode(), entry.getContent(), recipients);
	}
	
	private void postAfterMe(String authorJid, String referencedJid, 
			String channelJid, String content, List<String> recipients) {
		
		if (authorJid.equals(referencedJid) || recipients.contains(referencedJid)) {
			return;
		}
		
		IQ iq = createIq(authorJid, referencedJid, channelJid, content);
		try {
			IQ iqResponse = getXmppComponent().handleIQLoopback(iq);
			if (iqResponse.getError() == null) {
				recipients.add(referencedJid);
			}
		} catch (Exception e) {
			LOGGER.warn(e);
		}
	}
	
	private IQ createIq(String authorJid, String authorRefJid, 
			String channelJid, String content) {
		IQ iq = new IQ();
		Element queryEl = iq.getElement().addElement("query", 
				"http://buddycloud.com/pusher/userposted-aftermypost");
		queryEl.addElement("authorJid").setText(authorJid);
		queryEl.addElement("referencedJid").setText(authorRefJid);
		queryEl.addElement("channel").setText(ConsumerUtils.getChannelAddress(channelJid));
		queryEl.addElement("postContent").setText(content);
        return iq;
	}
}
