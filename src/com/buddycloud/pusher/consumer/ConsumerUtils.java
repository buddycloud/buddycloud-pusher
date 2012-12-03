package com.buddycloud.pusher.consumer;

import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.packet.RSMSet;
import org.jivesoftware.smackx.pubsub.Affiliation;
import org.jivesoftware.smackx.pubsub.AffiliationsExtension;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.NodeExtension;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

public class ConsumerUtils {

	static {
		Object affiliationsProvider = ProviderManager.getInstance().getExtensionProvider(
				PubSubElementType.AFFILIATIONS.getElementName(), PubSubNamespace.BASIC.getXmlns());
		ProviderManager.getInstance().addExtensionProvider(PubSubElementType.AFFILIATIONS.getElementName(), 
				PubSubNamespace.OWNER.getXmlns(), affiliationsProvider);
		
		Object affiliationProvider = ProviderManager.getInstance().getExtensionProvider(
				"affiliation", PubSubNamespace.BASIC.getXmlns());
		ProviderManager.getInstance().addExtensionProvider("affiliation", 
				PubSubNamespace.OWNER.getXmlns(), affiliationProvider);
	}
	
	public static List<String> getOwners(Node node) throws XMPPException {
		List<Affiliation> affiliations = getAffiliations(node);
		List<String> owners = new LinkedList<String>();
		for (Affiliation affiliation : affiliations) {
			if (affiliation.getType().equals(Affiliation.Type.owner)) {
				owners.add(affiliation.getNodeId());
			}
		}
		return owners;
	}

	public static List<Affiliation> getAffiliations(Node node)
			throws XMPPException {
		List<Affiliation> affiliations = new LinkedList<Affiliation>();
		
		PubSub request = node.createPubsubPacket(Type.GET, 
				new NodeExtension(PubSubElementType.AFFILIATIONS, node.getId()), 
				PubSubNamespace.OWNER);
		
		while (true) {
			PubSub reply = (PubSub) node.sendPubsubPacket(Type.GET, request);
			AffiliationsExtension subElem = (AffiliationsExtension) reply.getExtension(
					PubSubElementType.AFFILIATIONS.getElementName(), 
					PubSubNamespace.BASIC.getXmlns());
			affiliations.addAll(subElem.getAffiliations());
			if (reply.getRsmSet() == null || affiliations.size() == reply.getRsmSet().getCount()) {
				break;
			}
			RSMSet rsmSet = new RSMSet();
			rsmSet.setAfter(reply.getRsmSet().getLast());
			request.setRsmSet(rsmSet);
		}
		return affiliations;
	}
	
}
