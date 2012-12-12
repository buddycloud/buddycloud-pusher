package com.buddycloud.pusher.message;

import java.util.LinkedList;
import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.smack.XMPPException;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.XMPPComponent;

public class ConsumerUtils {

	public static final String PUBSUB_NS = "http://jabber.org/protocol/pubsub";
	public static final String RSM_NS = "http://jabber.org/protocol/rsm";
	public static final String PUBSUB_OWNER_NS = PUBSUB_NS + "#owner";
	public static final String PUBSUB_EVENT_NS = PUBSUB_NS + "#event";
	
	@SuppressWarnings("unchecked")
	public static List<Affiliation> getAffiliations(String node, XMPPComponent xmppComponent) throws XMPPException {
		String channelServerJid = xmppComponent.getProperty("channel.server");
		IQ affiliationRequestIq = createAffiliationsRequest(node, channelServerJid, null);
		
		List<Affiliation> affiliations = new LinkedList<Affiliation>();
		while (true) {
			IQ affiliationsIq = xmppComponent.syncIQ(affiliationRequestIq);
			
			Element pubsubResponseEl = affiliationsIq.getChildElement();
			Element affiliationsResponseEl = pubsubResponseEl.element("affiliations");
			List<Element> affiliationsChildren = affiliationsResponseEl.elements("affiliation");
			
			for (Element affiliationEl : affiliationsChildren) {
				affiliations.add(Affiliation.parse(affiliationEl));
			}
			
			Element rsmEl = pubsubResponseEl.element("rsm");
			if (rsmEl == null) {
				break;
			}
			Element lastEl = rsmEl.element("last");
			if (lastEl == null) {
				break;
			}
			
			affiliationRequestIq = createAffiliationsRequest(node, channelServerJid, 
					lastEl.getText());
		}
		
		return affiliations;
	}

	private static IQ createAffiliationsRequest(String node,
			String channelServerJid, String after) {
		IQ affiliationRequestIq = new IQ();
		affiliationRequestIq.setTo(channelServerJid);
		Element pubsubEl = affiliationRequestIq.getElement().addElement("pubsub", PUBSUB_OWNER_NS);
		Element affiliationsEl = pubsubEl.addElement("affiliations");
		affiliationsEl.addAttribute("node", node);
		
		if (after != null) {
			Element rsmEl = pubsubEl.addElement("set", RSM_NS);
			rsmEl.addElement("after").setText(after);
		}
		
		return affiliationRequestIq;
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
	
	public static String getChannelAddress(String nodeJid) {
		return nodeJid.split("/")[2];
	}
}
