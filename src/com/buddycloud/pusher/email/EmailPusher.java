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
package com.buddycloud.pusher.email;

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
public class EmailPusher implements Pusher<Email> {

	private static final Logger LOGGER = Logger.getLogger(EmailPusher.class);
	private final Properties properties;
	private Map<String, String> defaultTokens;
	private ConcurrentLinkedQueue<Email> emailsToSend = new ConcurrentLinkedQueue<Email>();

	/**
	 * 
	 */
	public EmailPusher(Properties properties) {
		this.properties = properties;
		this.defaultTokens = readMailTemplateTokens();
	}
	
	/**
	 * 
	 */
	public void start() {
		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(10);
		threadPool.scheduleAtFixedRate(createPusherRunnable(), 0, 1, TimeUnit.MINUTES);
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
					} catch (MessagingException e) {
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

		Session session = Session.getInstance(smtpProps,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});
		return session;
	}

	private Properties getSMTPProperties() {
		Properties smtpProps = new Properties();
		for (Object key : properties.keySet()) {
			String keyStr = (String) key;
			if (keyStr.startsWith("mail.smtp")) {
				smtpProps.put(key, properties.get(key));
			}
		}
		return smtpProps;
	}
	
	private Map<String, String> readMailTemplateTokens() {
		Map<String, String> mailTemplateTokens = new HashMap<String, String>();
		for (Object key : properties.keySet()) {
			String keyStr = (String) key;
			if (keyStr.startsWith("mail.template")) {
				mailTemplateTokens.put(keyStr, (String) properties.get(key));
			}
		}
		return mailTemplateTokens;
	}
	
	public void push(Email email) {
		emailsToSend.offer(email);
	}

	public Email createEmail(Map<String, String> tokens, String templateFileName) {
		HashMap<String, String> allTokens = new HashMap<String, String>();
		allTokens.putAll(defaultTokens);
		allTokens.putAll(tokens);
		return Email.parse(allTokens, templateFileName);
	}
}
