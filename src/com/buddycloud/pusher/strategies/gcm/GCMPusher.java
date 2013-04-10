package com.buddycloud.pusher.strategies.gcm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.buddycloud.pusher.Pusher;
import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class GCMPusher implements Pusher {

	public static final String TYPE = "gcm";
	private static final Logger LOGGER = Logger.getLogger(GCMPusher.class);
	
	private final Properties properties;

	public GCMPusher(Properties properties) {
		this.properties = properties;
	}
	
	@Override
	public void push(String target, Event event, Map<String, String> tokens) {
		LOGGER.debug("Sending out GCM to " + target + ", " + tokens);
		
		Sender sender = new Sender(properties.getProperty("gcm.api_key"));
		Message.Builder msgBuilder = new Message.Builder();
		msgBuilder.collapseKey("bc_notification")
			.timeToLive(60)
			.delayWhileIdle(true)
			.addData("event", event.toString());
		
		for (Entry<String, String> token : tokens.entrySet()) {
			msgBuilder.addData(token.getKey(), token.getValue());
		}
		Message message = msgBuilder.build();
		Result result = null;
		try {
			result = sender.send(message, target, 5);
		} catch (IOException e) {
			LOGGER.error("Failed to send GCM.", e);
			return;
		}
		
		if (result.getMessageId() != null) {
			String canonicalRegId = result.getCanonicalRegistrationId();
			if (canonicalRegId != null) {
				//TODO same device has more than on registration ID: update database
			}
		} else {
			LOGGER.error("Failed to send GCM: " + result.getErrorCodeName());
			String error = result.getErrorCodeName();
			if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
				//TODO application has been removed from device - unregister database
			}
		}
		
	}

	@Override
	public Map<String, String> getMetadata() {
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("google_project_id", properties.getProperty("gcm.google_project_id"));
		return metadata;
	}
}
