package com.buddycloud.pusher.message;

import org.xmpp.packet.Message;

public interface MessageConsumer {
	void consume(Message message);
}
