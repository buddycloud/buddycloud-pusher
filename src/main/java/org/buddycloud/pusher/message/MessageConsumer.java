package org.buddycloud.pusher.message;

import java.util.List;

import org.xmpp.packet.Message;

public interface MessageConsumer {
	void consume(Message message, List<String> recipients);
}
