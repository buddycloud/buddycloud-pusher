package com.buddycloud.pusher.message;

import com.buddycloud.pusher.XMPPComponent;

public abstract class AbstractMessageConsumer implements MessageConsumer {

	private final XMPPComponent xmppComponent;

	public AbstractMessageConsumer(XMPPComponent xmppComponent) {
		this.xmppComponent = xmppComponent;
	}
	
	public XMPPComponent getXmppComponent() {
		return xmppComponent;
	}
}
