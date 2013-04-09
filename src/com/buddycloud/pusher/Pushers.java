package com.buddycloud.pusher;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.buddycloud.pusher.strategies.email.EmailPusher;
import com.buddycloud.pusher.strategies.gcm.GCMPusher;

public class Pushers {

	private final Map<String, Pusher> pushers = new HashMap<String, Pusher>();
	private static Pushers instance;
	
	public Pushers(Properties properties) {
		pushers.put(EmailPusher.TYPE, new EmailPusher(properties));
		pushers.put(GCMPusher.TYPE, new GCMPusher(properties));
	}

	public static Pushers getInstance(Properties properties) {
		if (instance == null) {
			instance = new Pushers(properties);
		}
		return instance;
	}
	
	public Pusher get(String type) {
		return pushers.get(type);
	}
}
