package com.buddycloud.pusher.consumer;

import java.util.LinkedList;
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
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;

public class UserPostedAfterMyPostConsumer extends AbstractItemConsumer {

	private static final Logger LOGGER = Logger.getLogger(UserPostedAfterMyPostConsumer.class);
	
	public UserPostedAfterMyPostConsumer(PubSubManager manager,
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
		
		Element inReplyToElement = atomEntry.element("in-reply-to");
		if (inReplyToElement == null) {
			return;
		}
		
		String replyRef = inReplyToElement.attributeValue("ref");
		LeafNode node = null;
		try {
			node = (LeafNode) getManager().getNode(item.getNode());
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		List<String> itemIds = new LinkedList<String>();
		itemIds.add(replyRef);
		
		List<Item> refItems = null;
		try {
			refItems = node.getItems(itemIds);
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		if (refItems.isEmpty()) {
			return;
		}
		
		Item refItem = refItems.iterator().next();
		PayloadItem<PacketExtension> payloadRefItem = (PayloadItem<PacketExtension>) refItem;
		PacketExtension payloadRef = payloadRefItem.getPayload();
		
		Element atomRefEntry = null;
		try {
			atomRefEntry = DocumentHelper.parseText(
					payloadRef.toXML()).getRootElement();
		} catch (DocumentException e) {
			LOGGER.warn(e);
			return;
		}
		
		Element authorRefElement = atomRefEntry.element("author");
		String authorRefUri = authorRefElement.elementText("uri");
		
		postAfterMe(authorUri, authorRefUri, item.getNode(), content);
	}
	
	private void postAfterMe(final String authorJid, final String authorRefJid, 
			final String channelJid, final String content) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return createIq(authorJid, authorRefJid, channelJid, content);
			}
		};
		iq.setType(Type.SET);
		iq.setTo(getPusherJid());
		getXmppConnection().sendPacket(iq);
	}
	
	private String createIq(String authorJid, String authorRefJid, 
			String channelJid, String content) {
		StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"http://buddycloud.com/pusher/userposted-aftermypost\">");
        buf.append("<userJid>").append(authorJid).append("</userJid>");
        buf.append("<referencedJid>").append(authorRefJid).append("</referencedJid>");
        buf.append("<channel>").append(channelJid).append("</channel>");
        buf.append("<postContent>").append(content).append("</postContent>");
        buf.append("</query>");
        return buf.toString();
	}
}
