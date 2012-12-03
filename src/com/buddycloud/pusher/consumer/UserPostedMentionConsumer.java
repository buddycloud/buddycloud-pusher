package com.buddycloud.pusher.consumer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;

public class UserPostedMentionConsumer extends AbstractItemConsumer {

	private static final Logger LOGGER = Logger.getLogger(UserPostedMentionConsumer.class);
	private static final Pattern JID_REGEX = Pattern.compile("(\\b(\\S+@[a-zA-Z0-9_\\-\\.]+)\\b)");
	
	public UserPostedMentionConsumer(PubSubManager manager,
			XMPPConnection xmppConnection, String pusherJid) {
		super(manager, xmppConnection, pusherJid);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void consume(Item item) {
		
		PayloadItem<PacketExtension> payloadItem = (PayloadItem<PacketExtension>) item;
		PacketExtension payload = payloadItem.getPayload();
		if (!(payload instanceof SimplePayload)) {
			return;
		}
		
		Element atomEntry = null;
		try {
			atomEntry = DocumentHelper.parseText(
					payload.toXML()).getRootElement();
		} catch (DocumentException e) {
			LOGGER.warn(e);
			return;
		}
		
		Element authorElement = atomEntry.element("author");
		String authorUri = authorElement.elementText("uri");
		String content = atomEntry.elementText("content");
		
		Matcher matcher = JID_REGEX.matcher(content);
		while (matcher.find()) {
			String mentionedJid = matcher.group();
			newMention(authorUri, mentionedJid, item.getNode(), content);
		}
	}
	
	private void newMention(final String authorJid, final String mentionedJid, 
			final String channelJid, final String content) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return createIq(authorJid, mentionedJid, channelJid, content);
			}
		};
		iq.setType(Type.SET);
		iq.setTo(getPusherJid());
		getXmppConnection().sendPacket(iq);
	}
	
	private String createIq(String authorJid, String mentionedJid, 
			String channelJid, String content) {
		StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"http://buddycloud.com/pusher/userposted-mention\">");
        buf.append("<userJid>").append(authorJid).append("</userJid>");
        buf.append("<mentionedJid>").append(mentionedJid).append("</mentionedJid>");
        buf.append("<channel>").append(channelJid).append("</channel>");
        buf.append("<postContent>").append(content).append("</postContent>");
        buf.append("</query>");
        return buf.toString();
	}
}
