package com.buddycloud.pusher.consumer;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;

public class UserPostedOnMyChannelConsumer extends AbstractItemConsumer {

	private static final Logger LOGGER = Logger.getLogger(AbstractItemConsumer.class);
	
	public UserPostedOnMyChannelConsumer(PubSubManager manager,
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
		
		List<String> owners = null;
		try {
			owners = ConsumerUtils.getOwners(getManager().getNode(item.getNode()));
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		for (String owner : owners) {
			userPosted(authorUri, owner, item.getNode(), content);
		}
	}

	private void userPosted(final String authorJid, final String ownerJid, 
			final String channelJid, final String content) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return createIq(authorJid, ownerJid, channelJid, content);
			}
		};
		iq.setType(Type.SET);
		iq.setTo(getPusherJid());
		getXmppConnection().sendPacket(iq);
	}
	
	private String createIq(String authorJid, String ownerJid, 
			String channelJid, String content) {
		StringBuilder buf = new StringBuilder();
		buf.append("<query xmlns=\"http://buddycloud.com/pusher/userposted-mychannel\">");
        buf.append("<userJid>").append(authorJid).append("</userJid>");
        buf.append("<channelOwnerJid>").append(ownerJid).append("</channelOwnerJid>");
        buf.append("<channel>").append(channelJid).append("</channel>");
        buf.append("<postContent>").append(content).append("</postContent>");
        buf.append("</query>");
        return buf.toString();
	}
	
}
