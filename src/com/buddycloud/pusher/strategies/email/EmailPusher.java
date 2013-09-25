/*
 * Copyright 2011 buddycloud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.buddycloud.pusher.strategies.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.buddycloud.pusher.Pusher;
import com.buddycloud.pusher.utils.Configuration;

/**
 * @author Abmar
 *
 */
public class EmailPusher implements Pusher {

	public static final String TYPE = "email";
	
	private static final Logger LOGGER = Logger.getLogger(EmailPusher.class);
	private static final String TEMPLATE_PREFIX = "mail.template.";
	
	private static final String USERFOLLOWED_TEMPLATE = "userfollowed-mychannel.tpl";
	private static final String USERUNFOLLOWED_TEMPLATE = "userunfollowed-mychannel.tpl";
	private static final String FOLLOWREQUEST_TEMPLATE = "followrequest.tpl";
	private static final String FOLLOWREQUESTAPPROVED_TEMPLATE = "followrequest-approved.tpl";
	private static final String FOLLOWREQUESTDENIED_TEMPLATE = "followrequest-denied.tpl";
	private static final String POST_AFTER_MY_POST_TEMPLATE = "userposted-aftermypost.tpl";
	private static final String MENTION_TEMPLATE = "userposted-mention.tpl";
	private static final String POST_ON_MY_CHANNEL_TEMPLATE = "userposted-mychannel.tpl";
	private static final String POST_ON_SUBSCRIBED_CHANNEL_TEMPLATE = "userposted-subscribedchannel.tpl";
	private static final String WELCOME_TEMPLATE = "welcome.tpl";
	
	private final Properties properties;
	private Map<String, String> defaultTokens;
	private ConcurrentLinkedQueue<Email> emailsToSend = new ConcurrentLinkedQueue<Email>();

	/**
	 * 
	 */
	public EmailPusher(Properties properties) {
		this.properties = properties;
		this.defaultTokens = readMailTemplateTokens();
		start();
	}
	
	/**
	 * 
	 */
	private void start() {
		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(10);
		threadPool.scheduleAtFixedRate(createPusherRunnable(), 0, 30, TimeUnit.SECONDS);
	}
	
	/**
	 * @return
	 */
	private Runnable createPusherRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				int emailCount = emailsToSend.size();
				Session session = getSMTPSession();
				
				for (int i = 0; i < emailCount; i++) {
					Email email = emailsToSend.poll();
					if (email == null) {
						return;
					}
					try {
						send(email, session);
						LOGGER.trace("Sending out email to " + email.getTo());
					} catch (MessagingException e) {
						emailsToSend.offer(email);
						LOGGER.error(e);
					}
				}
				
				try {
					session.getTransport().close();
				} catch (MessagingException e) {
					LOGGER.error(e);
				}
			}
		};
	}

	private void send(Email email, Session session) throws MessagingException {
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(email.getFrom()));
		message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(email.getTo()));
		message.setSubject(email.getSubject());
		message.setContent(email.getContent(), "text/html");
		Transport.send(message);
	}

	private Session getSMTPSession() {
		final String username = properties.getProperty(Configuration.MAIL_USERNAME);
		final String password = properties.getProperty(Configuration.MAIL_PASSWORD);
		Properties smtpProps = getSMTPProperties();

		String useAuthStr = properties.getProperty("mail.smtp.auth");
		
		Session session = null;
		if (Boolean.valueOf(useAuthStr)) {
			session = Session.getInstance(smtpProps,
					new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});
		} else {
			session = Session.getInstance(smtpProps);
		}
		
		return session;
	}

	private Properties getSMTPProperties() {
		Properties smtpProps = new Properties();
		for (Object key : properties.keySet()) {
			String keyStr = (String) key;
			if (keyStr.startsWith("mail.smtp") || keyStr.startsWith("mail.transport")) {
				smtpProps.put(key, properties.get(key));
			}
		}
		return smtpProps;
	}
	
	private Map<String, String> readMailTemplateTokens() {
		Map<String, String> mailTemplateTokens = new HashMap<String, String>();
		for (Object key : properties.keySet()) {
			String keyStr = (String) key;
			if (keyStr.startsWith(TEMPLATE_PREFIX)) {
				mailTemplateTokens.put(keyStr.substring(TEMPLATE_PREFIX.length()), 
						((String) properties.get(key)));
			}
		}
		return mailTemplateTokens;
	}

	public Email createEmail(Map<String, String> tokens, String templateFileName) {
		HashMap<String, String> allTokens = new HashMap<String, String>();
		allTokens.putAll(defaultTokens);
		allTokens.putAll(tokens);
		return Email.parse(allTokens, templateFileName);
	}

	@Override
	public void push(String target, Event event, Map<String, String> tokens) {
		tokens.put("EMAIL", target);
		Email email = createEmail(tokens, getTemplate(event));
		emailsToSend.offer(email);
	}

	private String getTemplate(Event event) {
		switch (event) {
		case SIGNUP:
			return WELCOME_TEMPLATE;
		case FOLLOW:
			return USERFOLLOWED_TEMPLATE;
		case UNFOLLOW:
			return USERUNFOLLOWED_TEMPLATE;
		case FOLLOW_REQUEST:
			return FOLLOWREQUEST_TEMPLATE;
		case POST_AFTER_MY_POST:
			return POST_AFTER_MY_POST_TEMPLATE;
		case MENTION:
			return MENTION_TEMPLATE;
		case POST_ON_MY_CHANNEL:
			return POST_ON_MY_CHANNEL_TEMPLATE;
		case POST_ON_SUBSCRIBED_CHANNEL:
			return POST_ON_SUBSCRIBED_CHANNEL_TEMPLATE;
		case FOLLOW_REQUEST_APPROVED:
			return FOLLOWREQUESTAPPROVED_TEMPLATE;
		case FOLLOW_REQUEST_DENIED:
			return FOLLOWREQUESTDENIED_TEMPLATE;
		default:
			break;
		}
		return null;
	}

	@Override
	public Map<String, String> getMetadata() {
		return new HashMap<String, String>();
	}
}
