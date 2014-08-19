package org.buddycloud.pusher.message;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.buddycloud.pusher.XMPPComponent;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;


public class UserPostedMentionConsumer extends AbstractMessageConsumer {

	private static final Logger LOGGER = Logger.getLogger(UserPostedMentionConsumer.class);
	private static final Pattern JID_REGEX = Pattern.compile("(\\b(\\S+@[a-zA-Z0-9_\\-\\.]+)\\b)");
	
	public UserPostedMentionConsumer(XMPPComponent xmppComponent) {
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
		
		Matcher matcher = JID_REGEX.matcher(entry.getContent());
		while (matcher.find()) {
			String mentionedJid = matcher.group();
			newMention(mentionedJid, entry, recipients);
		}
	}
	
	private void newMention(String mentionedJid, AtomEntry entry, List<String> recipients) {
		if (mentionedJid.equals(entry.getAuthor()) || recipients.contains(mentionedJid)) {
			return;
		}
		
		IQ iq = createIq(entry.getAuthor(), mentionedJid, 
				entry.getNode(), entry.getContent());
		try {
			IQ iqResponse = getXmppComponent().handleIQLoopback(iq);
			if (iqResponse.getError() == null) {
				recipients.add(mentionedJid);
			}
		} catch (Exception e) {
			LOGGER.warn(e);
		}
	}

	private IQ createIq(String authorJid, String mentionedJid, 
			String channelJid, String content) {
		IQ iq = new IQ();
		Element queryEl = iq.getElement().addElement("query", 
				"http://buddycloud.com/pusher/userposted-mention");
		queryEl.addElement("authorJid").setText(authorJid);
		queryEl.addElement("mentionedJid").setText(mentionedJid);
		queryEl.addElement("channel").setText(ConsumerUtils.getChannelAddress(channelJid));
		queryEl.addElement("postContent").setText(content);
        return iq;
	}
}
