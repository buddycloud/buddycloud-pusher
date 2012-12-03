package com.buddycloud.pusher.consumer;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public abstract class AbstractItemConsumer implements ItemConsumer {

	private final PubSubManager manager;
	private final XMPPConnection xmppConnection;
	private final String pusherJid;

	public AbstractItemConsumer(PubSubManager manager, 
			XMPPConnection xmppConnection, String pusherJid) {
		this.manager = manager;
		this.xmppConnection = xmppConnection;
		this.pusherJid = pusherJid;
	}
	
	public PubSubManager getManager() {
		return manager;
	}
	
	public XMPPConnection getXmppConnection() {
		return xmppConnection;
	}
	
	public String getPusherJid() {
		return pusherJid;
	}
}
