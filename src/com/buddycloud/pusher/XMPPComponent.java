package com.buddycloud.pusher;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.xmpp.component.AbstractComponent;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.handler.GetNotificationSettingsQueryHandler;
import com.buddycloud.pusher.handler.QueryHandler;
import com.buddycloud.pusher.handler.SetNotificationSettingsQueryHandler;
import com.buddycloud.pusher.handler.SignupQueryHandler;
import com.buddycloud.pusher.handler.internal.DeleteUserQueryHandler;
import com.buddycloud.pusher.handler.internal.FollowRequestQueryHandler;
import com.buddycloud.pusher.handler.internal.UserFollowedQueryHandler;
import com.buddycloud.pusher.handler.internal.UserPostedAfterMyPostQueryHandler;
import com.buddycloud.pusher.handler.internal.UserPostedMentionQueryHandler;
import com.buddycloud.pusher.handler.internal.UserPostedOnMyChannelQueryHandler;
import com.buddycloud.pusher.handler.internal.UserPostedOnSubscribedChannelQueryHandler;
import com.buddycloud.pusher.message.MessageProcessor;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 *
 */
public class XMPPComponent extends AbstractComponent {

	private static final String DESCRIPTION = "Pusher service for buddycloud";
	private static final String NAME = "Buddycloud pusher";
	private static final Logger LOGGER = Logger.getLogger(XMPPComponent.class);
	
	private final Map<String, QueryHandler> queryGetHandlers = new HashMap<String, QueryHandler>();
	private final Map<String, QueryHandler> querySetHandlers = new HashMap<String, QueryHandler>();
	private final Map<String, QueryHandler> loHandlers = new HashMap<String, QueryHandler>();
	private final Map<String, BlockingQueue<IQ>> iqResultQueues = new HashMap<String, BlockingQueue<IQ>>();
	
	private final Properties configuration;
	private DataSource dataSource;
	private MessageProcessor messageConsumer;
	
	/**
	 * @param configuration
	 */
	public XMPPComponent(Properties configuration) {
		this.configuration = configuration;
		this.dataSource = new DataSource(configuration);
		
		initHandlers();
		LOGGER.debug("XMPP component initialized.");
	}

	/**
	 * 
	 */
	private void initHandlers() {
		// Get handlers
		addHandler(new GetNotificationSettingsQueryHandler(configuration, dataSource), queryGetHandlers);
		// Set handlers
		addHandler(new SetNotificationSettingsQueryHandler(configuration, dataSource), querySetHandlers);
		addHandler(new SignupQueryHandler(configuration, dataSource), querySetHandlers);
		// Loopback handlers
		addHandler(new FollowRequestQueryHandler(configuration, dataSource), loHandlers);
		addHandler(new DeleteUserQueryHandler(configuration, dataSource), loHandlers);
		addHandler(new UserFollowedQueryHandler(configuration, dataSource), loHandlers);
		addHandler(new UserPostedAfterMyPostQueryHandler(configuration, dataSource), loHandlers);
		addHandler(new UserPostedMentionQueryHandler(configuration, dataSource), loHandlers);
		addHandler(new UserPostedOnMyChannelQueryHandler(configuration, dataSource), loHandlers);
		addHandler(new UserPostedOnSubscribedChannelQueryHandler(configuration, dataSource), loHandlers);
		// Message consumer
		this.messageConsumer = new MessageProcessor(this);
	}

	private void addHandler(QueryHandler queryHandler, Map<String, QueryHandler> handlers) {
		handlers.put(queryHandler.getNamespace(), queryHandler);
	}

	/* (non-Javadoc)
	 * @see org.xmpp.component.AbstractComponent#handleIQSet(org.xmpp.packet.IQ)
	 */
	@Override
	protected IQ handleIQSet(IQ iq) throws Exception {
		return handle(iq, querySetHandlers);
	}
	
	@Override
	protected IQ handleIQGet(IQ iq) throws Exception {
		return handle(iq, queryGetHandlers);
	}
	
	public IQ handleIQLoopback(IQ iq) throws Exception {
		return handle(iq, loHandlers);
	}
	
	@Override
	protected void handleIQResult(IQ iq) {
		BlockingQueue<IQ> queue = iqResultQueues.get(iq.getID());
		if (queue != null) {
			queue.add(iq);
		}
	}
	
	public IQ syncIQ(IQ iq) {
		String iqId = StringUtils.randomString(6);
		iq.setID(iqId);
		
		BlockingQueue<IQ> queue = new LinkedBlockingDeque<IQ>();
		iqResultQueues.put(iqId, queue);
		send(iq);
		try {
			IQ poll = queue.poll(SmackConfiguration.getPacketReplyTimeout(), TimeUnit.SECONDS);
			return poll;
		} catch (InterruptedException e) {
			return null;
		} finally {
			iqResultQueues.remove(iqId);
		}
	}
	
	@Override
	protected void handleMessage(Message message) {
		messageConsumer.consume(message);
	}
	
	private IQ handle(IQ iq, Map<String, QueryHandler> handlers) {
		Element queryElement = iq.getElement().element("query");
		if (queryElement == null) {
			return XMPPUtils.error(iq, "IQ does not contain query element.", 
					LOGGER);
		}
		
		Namespace namespace = queryElement.getNamespace();
		
		QueryHandler queryHandler = handlers.get(namespace.getURI());
		if (queryHandler == null) {
			return XMPPUtils.error(iq, "QueryHandler not found for namespace: " + namespace, 
					LOGGER);
		}
		
		return queryHandler.handle(iq);
	}
	
	/* (non-Javadoc)
	 * @see org.xmpp.component.AbstractComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	/* (non-Javadoc)
	 * @see org.xmpp.component.AbstractComponent#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	@Override 
	protected String discoInfoIdentityCategory() {
		return ("Pusher");
	}

	@Override 
	protected String discoInfoIdentityCategoryType() {
		return ("Notification");
	}
	
	public String getProperty(String property) {
		return configuration.getProperty(property);
	}

}
