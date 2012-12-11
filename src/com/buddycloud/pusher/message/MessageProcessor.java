package com.buddycloud.pusher.message;

import java.util.LinkedList;
import java.util.List;

import org.xmpp.packet.Message;

import com.buddycloud.pusher.XMPPComponent;

public class MessageProcessor implements MessageConsumer {

	private final List<MessageConsumer> consumers = new LinkedList<MessageConsumer>();
	private final XMPPComponent component;
	
	public MessageProcessor(XMPPComponent component) {
		this.component = component;
		initConsumers();
	}
	
	private void initConsumers() {
		consumers.add(new UserFollowedConsumer(component));
		consumers.add(new UserPostedAfterMyPostConsumer(component));
		consumers.add(new UserPostedMentionConsumer(component));
		consumers.add(new UserPostedOnMyChannelConsumer(component));
		consumers.add(new UserPostedOnSubscribedChannelConsumer(component));
	}

	public void consume(Message message) {
		for (MessageConsumer itemConsumer : consumers) {
			itemConsumer.consume(message);
		}
	}
}
