package org.buddycloud.pusher.message;

import java.util.LinkedList;
import java.util.List;

import org.buddycloud.pusher.XMPPComponent;
import org.xmpp.packet.Message;


public class MessageProcessor implements MessageConsumer {

	private final List<MessageConsumer> consumers = new LinkedList<MessageConsumer>();
	private final XMPPComponent component;
	
	public MessageProcessor(XMPPComponent component) {
		this.component = component;
		initConsumers();
	}
	
	private void initConsumers() {
		consumers.add(new UserPostedMentionConsumer(component));
		consumers.add(new UserPostedAfterMyPostConsumer(component));
		consumers.add(new UserPostedOnMyChannelConsumer(component));
		consumers.add(new UserPostedOnSubscribedChannelConsumer(component));
		consumers.add(new SubscriptionChangedConsumer(component));
	}

	public void consume(Message message) {
		consume(message, new LinkedList<String>());
	}
	
	public void consume(Message message, List<String> recipient) {
		String channelServerJid = component.getProperty("channel.server");
		if (!message.getFrom().toString().equals(channelServerJid)) {
			return;
		}
		for (MessageConsumer itemConsumer : consumers) {
			itemConsumer.consume(message, recipient);
		}
	}
}
