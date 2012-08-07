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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.buddycloud.pusher.Pusher;
import com.buddycloud.pusher.utils.Configuration;

/**
 * @author Abmar
 *
 */
public class EmailPusher implements Pusher {

	private final Properties properties;
	private final Map<String, String> tokens;
	private final String templateFileName;

	/**
	 * 
	 */
	public EmailPusher(Properties properties, Map<String, String> tokens, 
			String templateFileName) {
		this.properties = properties;
		this.tokens = new HashMap<String, String>();
		this.tokens.putAll(getMailTemplateTokens());
		this.tokens.putAll(tokens);
		this.templateFileName = templateFileName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void push() {
		send(Email.parse(tokens, templateFileName));
	}

	private void send(Email email) {
		final String username = properties.getProperty(Configuration.MAIL_USERNAME);
		final String password = properties.getProperty(Configuration.MAIL_PASSWORD);
		Properties smtpProps = getSMTPProperties();

		Session session = Session.getInstance(smtpProps,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(email.getFrom()));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(email.getTo()));
			message.setSubject(email.getSubject());
			message.setContent(email.getContent(), "text/html");

			Transport.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
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
	
	private Map<String, String> getMailTemplateTokens() {
		Map<String, String> mailTemplateTokens = new HashMap<String, String>();
		for (Object key : properties.keySet()) {
			String keyStr = (String) key;
			if (keyStr.startsWith("mail.template")) {
				mailTemplateTokens.put(keyStr, (String) properties.get(key));
			}
		}
		return mailTemplateTokens;
	}

}
