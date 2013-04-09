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
package com.buddycloud.pusher.handler;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.buddycloud.pusher.db.DataSource;
import com.buddycloud.pusher.utils.XMPPUtils;

/**
 * Generic QueryHandler. 
 * QueryHandler implementations should extend this class.
 * 
 * @see QueryHandler
 *  
 */
public abstract class AbstractQueryHandler implements QueryHandler {

	private final String namespace;
	private final Logger logger;
	private final Properties properties;
	private final DataSource dataSource;
	
	/**
	 * Creates a QueryHandler for a given namespace 
	 * @param namespace
	 * @param dataSource 
	 */
	public AbstractQueryHandler(String namespace, Properties properties, 
			DataSource dataSource) {
		this.namespace = namespace;
		this.properties = properties;
		this.dataSource = dataSource;
		this.logger = Logger.getLogger(getClass());
	}
	
	@Override
	public String getNamespace() {
		return namespace;
	}
	
	protected Logger getLogger() {
		return logger;
	}
	
	protected Properties getProperties() {
		return properties;
	}
	
	/* (non-Javadoc)
	 * @see com.buddycloud.pusher.handler.QueryHandler#handle(org.xmpp.packet.IQ)
	 */
	@Override
	public IQ handle(IQ query) {
		return handleQuery(query);
	}

	/**
	 * @param query
	 * @return
	 */
	protected abstract IQ handleQuery(IQ query);
	
	protected IQ createResponse(IQ iq, String info) {
		IQ result = IQ.createResultIQ(iq);
		Element queryElement = result.getElement().addElement("query", getNamespace());
		XMPPUtils.addInfo(info, queryElement);
		return result;
	}
	
	/**
	 * @return the dataSource
	 */
	public DataSource getDataSource() {
		return dataSource;
	}
}
