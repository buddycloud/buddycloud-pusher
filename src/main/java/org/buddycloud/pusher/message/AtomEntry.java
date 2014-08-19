package org.buddycloud.pusher.message;

import org.dom4j.Element;

public class AtomEntry {

	private static final String ACCT = "acct:";
	
	private String id;
	private String node;
	private String author;
	private String content;
	private String inReplyTo;
	
	public AtomEntry(String id, String node, String author, String content,
			String inReplyTo) {
		this.id = id;
		this.node = node;
		this.author = author;
		this.content = content;
		this.inReplyTo = inReplyTo;
	}

	public String getId() {
		return id;
	}

	public String getNode() {
		return node;
	}

	public String getAuthor() {
		return author;
	}

	public String getContent() {
		return content;
	}

	public String getInReplyTo() {
		return inReplyTo;
	}

	public static AtomEntry parse(Element itemsEl) {
		if (itemsEl == null) {
			return null;
		}
		
		Element itemEl = itemsEl.element("item");
		if (itemEl == null) {
			return null;
		}
		
		Element atomEl = itemEl.element("entry");
		if (atomEl == null) {
			return null;
		}
		
		String node = itemsEl.attributeValue("node");
		
		Element authorElement = atomEl.element("author");
		String authorUri = authorElement.elementText("uri");
		authorUri = authorUri.substring(ACCT.length());
		String content = atomEl.elementText("content");
		
		Element inReplyToElement = atomEl.element("in-reply-to");
		String replyRef = null;
		if (inReplyToElement != null) {
			replyRef = inReplyToElement.attributeValue("ref");
		}
		
		String id = itemEl.attributeValue("id");
		
		return new AtomEntry(id, node, authorUri, content, replyRef);
	}
	
}
