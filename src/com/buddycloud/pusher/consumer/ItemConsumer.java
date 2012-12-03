package com.buddycloud.pusher.consumer;

import org.jivesoftware.smackx.pubsub.Item;

public interface ItemConsumer {
	void consume(Item item);
}
