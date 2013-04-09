package com.buddycloud.pusher.strategies.gcm;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.buddycloud.pusher.Pusher;

public class GCMPusher implements Pusher {

	public static final String TYPE = "gcm";
	private final Properties properties;

	public GCMPusher(Properties properties) {
		this.properties = properties;
	}
	
	@Override
	public void push(String target, Event event, Map<String, String> tokens) {
		// TODO Auto-generated method stub
	}

	@Override
	public Map<String, String> getMetadata() {
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("google_project_id", properties.getProperty("gcm.google_project_id"));
		return metadata;
	}

}
