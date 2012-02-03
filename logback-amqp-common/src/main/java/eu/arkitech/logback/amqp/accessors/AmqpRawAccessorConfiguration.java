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
import eu.arkitech.logback.common.WorkerConfiguration;


public abstract class AmqpRawAccessorConfiguration
		extends WorkerConfiguration
{
	protected AmqpRawAccessorConfiguration (final AmqpRawAccessorConfiguration override, final AmqpRawAccessorConfiguration overriden)
	{
		super (override, overriden);
		this.host = Objects.firstNonNull (override.host, overriden.host);
		this.port = Objects.firstNonNull (override.port, overriden.port);
		this.virtualHost = Objects.firstNonNull (override.virtualHost, overriden.virtualHost);
		this.username = Objects.firstNonNull (override.username, overriden.username);
		this.password = Objects.firstNonNull (override.password, overriden.password);
	}
	
	protected AmqpRawAccessorConfiguration (final String host, final Integer port, final String virtualHost, final String username, final String password, final Callbacks callbacks, final Object monitor)
	{
		super (callbacks, monitor);
		this.host = host;
		this.port = port;
		this.virtualHost = virtualHost;
		this.username = username;
		this.password = password;
	}
	
	public final String host;
	public final String password;
	public final Integer port;
	public final String username;
	public final String virtualHost;
	public static final String defaultHost = "127.0.0.1";
	public static final String defaultPassword = "guest";
	public static final Integer defaultPort = 5672;
	public static final String defaultUsername = "guest";
	public static final String defaultVirtualHost = "/";
}
