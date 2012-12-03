package com.buddycloud.pusher.consumer;

import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;

public class UserFollowedConsumer extends AbstractItemConsumer {

	private static final Logger LOGGER = Logger.getLogger(AbstractItemConsumer.class);
	
	public UserFollowedConsumer(PubSubManager manager,
			XMPPConnection xmppConnection, String pusherJid) {
		super(manager, xmppConnection, pusherJid);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void consume(Item item) {
		PayloadItem<PacketExtension> payloadItem = (PayloadItem<PacketExtension>) item;
		PacketExtension payload = payloadItem.getPayload();
		if (!(payload instanceof Subscription)) {
			return;
		}
		
		Subscription subscription = (Subscription) payload;
		String newSubscriberJid = subscription.getJid();
		List<String> owners = null;
		try {
			owners = ConsumerUtils.getOwners(getManager().getNode(item.getNode()));
		} catch (XMPPException e) {
			LOGGER.warn(e);
			return;
		}
		
		for (String owner : owners) {
			newFollower(newSubscriberJid, owner, item.getNode());
		}
	}

	private void newFollower(final String subscriberJid, final String ownerJid, final String channelJid) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return createIq(subscriberJid, ownerJid, channelJid);
			}
		};
		iq.setType(Type.SET);
		iq.setTo(getPusherJid());
		getXmppConnection().sendPacket(iq);
	}
	
	private String createIq(final String subscriberJid,
			final String ownerJid, final String channelJid) {
		StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"http://buddycloud.com/pusher/userfollowed\">");
        buf.append("<userJid>").append(subscriberJid).append("</userJid>");
        buf.append("<channelOwnerJid>").append(ownerJid).append("</channelOwnerJid>");
        buf.append("<channel>").append(channelJid).append("</channel>");
        buf.append("</query>");
        return buf.toString();
	}
	
}
