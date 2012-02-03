/*
 * #%L
 * arkitech-logback-amqp-common
 * %%
 * Copyright (C) 2011 - 2012 Arkitech
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.arkitech.logback.amqp.accessors;


import com.google.common.base.Objects;
import eu.arkitech.logback.common.Callbacks;


public class AmqpRawConsumerConfiguration
		extends AmqpRawAccessorConfiguration
{
	public AmqpRawConsumerConfiguration ()
	{
		this (null, null, null, null, null, null, null, null, null, null);
	}
	
	public AmqpRawConsumerConfiguration (final AmqpRawConsumerConfiguration override, final AmqpRawConsumerConfiguration overriden)
	{
		super (override, overriden);
		this.exchange = Objects.firstNonNull (override.exchange, overriden.exchange);
		this.queue = Objects.firstNonNull (override.queue, overriden.queue);
		this.routingKey = Objects.firstNonNull (override.routingKey, overriden.routingKey);
	}
	
	public AmqpRawConsumerConfiguration (final String host, final Integer port, final String virtualHost, final String username, final String password, final String exchange, final String queue, final String routingKey, final Callbacks callbacks, final Object monitor)
	{
		super (host, port, virtualHost, username, password, callbacks, monitor);
		this.exchange = exchange;
		this.queue = queue;
		this.routingKey = routingKey;
	}
	
	public final String exchange;
	public final String queue;
	public final String routingKey;
	public static final String defaultExchange = "logging";
	public static final String defaultRoutingKey = "#";
}
