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
package org.buddycloud.pusher.strategies.email;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

/**
 * @author Abmar
 *
 */
public class Email {

	public static final int FROM_HEADER = 0;
	public static final int TO_HEADER = 1;
	public static final int SUBJECT_HEADER = 2;
	
	public static final String FIRST_PART_JID = "FIRST_PART_JID";
	public static final String EMAIL_ADDRESS = "EMAIL";
	
	private static final String TEMPLATES_PATH = "templates/email/";
	
	private String from;
	private String to;
	private String subject;
	private String content;
	
	/**
	 * @return the from
	 */
	public String getFrom() {
		return from;
	}
	
	/**
	 * @param from the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * @return the to
	 */
	public String getTo() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(String to) {
		this.to = to;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	public static Email parse(Map<String, String> tokens, String templateFileName) {
		List<String> readLines;
		try {
			readLines = FileUtils.readLines(new File(TEMPLATES_PATH + templateFileName));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		
		Email email = new Email();
		int i = 0;
		StringBuilder content = new StringBuilder();
		for (String templateLine : readLines) {
			for (Entry<String, String> tokenEntry : tokens.entrySet()) {
				templateLine = templateLine.replace("<%" + tokenEntry.getKey() + "%>", tokenEntry.getValue());
			}
			if (i == FROM_HEADER) {
				email.setFrom(templateLine);
			} else if (i == TO_HEADER) {
				email.setTo(templateLine);
			} else if (i == SUBJECT_HEADER) {
				email.setSubject(templateLine);
			} else {
				content.append(templateLine);
			}
			i++;
		}
		email.setContent(content.toString());
		
		return email;
	}
	
}
