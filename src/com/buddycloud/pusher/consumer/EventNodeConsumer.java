package com.buddycloud.pusher.consumer;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

public class EventNodeConsumer {

	private static final String EVENTS_NODE = "events";
	private final Properties configuration;
	private final List<ItemConsumer> consumers = new LinkedList<ItemConsumer>();
	
	public EventNodeConsumer(Properties configuration) {
		this.configuration = configuration;
	}
	
	public void start() throws XMPPException {
		XMPPConnection xmppConnection = createConnection();
		PubSubManager manager = new PubSubManager(xmppConnection);
		
		initConsumers(xmppConnection, manager);
		
		Node node = manager.getNode(EVENTS_NODE);
		node.addItemEventListener(createEventListener(manager));
	    node.subscribe(xmppConnection.getConnectionID());
	}

	private void initConsumers(XMPPConnection xmppConnection,
			PubSubManager manager) {
		String pusherJid = configuration.getProperty("consumer.pusherJid");
		consumers.add(new UserFollowedConsumer(manager, xmppConnection, pusherJid));
	}

	private ItemEventListener<Item> createEventListener(final PubSubManager manager) {
		return new ItemEventListener<Item>() {
			@Override
			public void handlePublishedItems(ItemPublishEvent<Item> event) {
				List<Item> items = event.getItems();
				for (Item item : items) {
					process(item);
				}
			}
		};
	}

	protected void process(Item item) {
		for (ItemConsumer itemConsumer : consumers) {
			itemConsumer.consume(item);
		}
	}

	private XMPPConnection createConnection() throws XMPPException {
		String serviceName = configuration.getProperty("consumer.xmpp.servicename");
		String host = configuration.getProperty("consumer.xmpp.host");
		String userName = configuration.getProperty("consumer.xmpp.username");

		ConnectionConfiguration cc = new ConnectionConfiguration(host,
				Integer.parseInt(configuration.getProperty("consumer.xmpp.port")),
				serviceName);

		XMPPConnection connection = new XMPPConnection(cc);
		connection.connect();
		connection.login(userName, configuration.getProperty("consumer.xmpp.password"), 
				"consumer" + new Random().nextLong());

		return connection;
	}

}
