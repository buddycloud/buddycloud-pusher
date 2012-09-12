package com.buddycloud.pusher;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.xmpp.component.AbstractComponent;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.handler.FollowRequestQueryHandler;
import com.buddycloud.pusher.handler.GetNotificationSettingsQueryHandler;
import com.buddycloud.pusher.handler.QueryHandler;
import com.buddycloud.pusher.handler.SetNotificationSettingsQueryHandler;
import com.buddycloud.pusher.handler.SignupQueryHandler;
import com.buddycloud.pusher.handler.DeleteUserQueryHandler;
import com.buddycloud.pusher.handler.UserFollowedQueryHandler;
import com.buddycloud.pusher.handler.UserPostedAfterMyPostQueryHandler;
import com.buddycloud.pusher.handler.UserPostedMentionQueryHandler;
import com.buddycloud.pusher.handler.UserPostedOnMyChannelQueryHandler;
import com.buddycloud.pusher.handler.UserPostedOnSubscribedChannelQueryHandler;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * @author Abmar
 *
 */
public class XMPPComponent extends AbstractComponent implements PusherSubmitter {

	private static final String DESCRIPTION = "Pusher service for buddycloud";
	private static final String NAME = "Buddycloud pusher";
	private static final Logger LOGGER = Logger.getLogger(XMPPComponent.class);
	private final ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	private final Map<String, QueryHandler> queryGetHandlers = new HashMap<String, QueryHandler>();
	private final Map<String, QueryHandler> querySetHandlers = new HashMap<String, QueryHandler>();
	
	private final Properties configuration;
	private DataSource dataSource;
	
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
		addHandler(new GetNotificationSettingsQueryHandler(configuration, dataSource, this), queryGetHandlers);
		// Set handlers
		addHandler(new FollowRequestQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new SetNotificationSettingsQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new SignupQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new DeleteUserQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new UserFollowedQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new UserPostedAfterMyPostQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new UserPostedMentionQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new UserPostedOnMyChannelQueryHandler(configuration, dataSource, this), querySetHandlers);
		addHandler(new UserPostedOnSubscribedChannelQueryHandler(configuration, dataSource, this), querySetHandlers);
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

	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.PusherSubmitter#submitPusher(com.buddycloud.pusher.Pusher)
	 */
	@Override
	public void submitPusher(final Pusher pusher) {
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				pusher.push();
			}
		});
	}
}
