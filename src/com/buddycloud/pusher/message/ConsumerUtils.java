package com.buddycloud.pusher.message;

import java.util.LinkedList;
import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.XMPPComponent;

public class ConsumerUtils {

	public static final String PUBSUB_NS = "http://jabber.org/protocol/pubsub";
	public static final String PUBSUB_OWNER_NS = PUBSUB_NS + "#owner";
	public static final String PUBSUB_EVENT_NS = PUBSUB_NS + "#event";
	
//	<iq from="channeluser@example.com/ChannelCompatibleClient" to="channelserver.example.com" type="get" id="26:85"> 
//	  <pubsub xmlns="http://jabber.org/protocol/pubsub#owner"> 
//	    <affiliations node="/user/channeluser@example.com/posts"/> 
//	    <set xmlns="http://jabber.org/protocol/rsm"> 
//	      <max>30</max> 
//	    </set> 
//	  </pubsub> 
//	</iq>
	
	@SuppressWarnings("unchecked")
	public static List<Affiliation> getAffiliations(String node, XMPPComponent xmppComponent) throws XMPPException {
		String channelServerJid = xmppComponent.getProperty("channel.server");
		IQ affiliationRequestIq = new IQ();
		affiliationRequestIq.setTo(channelServerJid);
		Element pubsubEl = affiliationRequestIq.getElement().addElement("pubsub", PUBSUB_OWNER_NS);
		Element affiliationsEl = pubsubEl.addElement("affiliations");
		affiliationsEl.addAttribute("node", node);
		
		IQ affiliationsIq = xmppComponent.syncIQ(affiliationRequestIq);
		
		List<Affiliation> affiliations = new LinkedList<Affiliation>();
		Element affiliationsResponseEl = affiliationsIq.getChildElement().element("affiliations");
		List<Element> affiliationsChildren = affiliationsResponseEl.elements("affiliation");
		
		//TODO RSM
		for (Element affiliationEl : affiliationsChildren) {
			affiliations.add(Affiliation.parse(affiliationEl));
		}
		
		return affiliations;
	}
	
	public static List<Affiliation> getOwners(String node, XMPPComponent xmppComponent) throws XMPPException {
		List<Affiliation> owners = new LinkedList<Affiliation>();
		for (Affiliation affiliation : getAffiliations(node, xmppComponent)) {
			if (affiliation.getAffiliation().equals("owner")) {
				owners.add(affiliation);
			}
		}
		return owners;
	}

	public static IQ getSingleItem(String replyRef, String node, XMPPComponent xmppComponent) {
		String channelServerJid = xmppComponent.getProperty("channel.server");
		IQ iq = new IQ();
		iq.setTo(channelServerJid);
		Element pubsubEl = iq.getElement().addElement("pubsub", PUBSUB_NS);
		Element itemsEl = pubsubEl.addElement("items");
		itemsEl.addAttribute("node", node);
		itemsEl.addElement("item").addAttribute("id", replyRef);
		return xmppComponent.syncIQ(iq);
	}
}
