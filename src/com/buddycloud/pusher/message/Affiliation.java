package com.buddycloud.pusher.message;

import org.dom4j.Element;

public class Affiliation {

	private String jid;
	private String affiliation;
	
	public Affiliation(String jid, String affiliation) {
		super();
		this.jid = jid;
		this.affiliation = affiliation;
	}
	
	public String getJid() {
		return jid;
	}
	
	public String getAffiliation() {
		return affiliation;
	}
	
	public static Affiliation parse(Element affiliationEl) {
		return new Affiliation(affiliationEl.attributeValue("jid"), 
				affiliationEl.attributeValue("affiliation"));
	}
}
