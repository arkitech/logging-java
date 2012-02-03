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


import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;


public final class AmqpRawPublisher
		extends AmqpRawAccessor
{
	public AmqpRawPublisher (final AmqpRawPublisherConfiguration configuration, final BlockingDeque<AmqpRawMessage> buffer)
	{
		super (configuration);
		this.buffer = (buffer != null) ? buffer : new LinkedBlockingDeque<AmqpRawMessage> ();
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (this.buffer.isEmpty ());
	}
	
	@Override
	protected final void executeLoop ()
	{
		loop : while (true) {
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft () || this.shouldStopHard ())
						break loop;
					if (!this.shouldReconnect ())
						break;
					if (this.reconnect ())
						break;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
				continue loop;
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp message publisher shoveling outbound messages to broker");
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft () || this.shouldStopHard ())
						break loop;
					if (this.shouldReconnect ())
						break;
				}
				this.shovelMessage ();
			}
		}
		this.disconnect ();
	}
	
	private final boolean publishMessage (final AmqpRawMessage message)
	{
		final AMQP.BasicProperties properties;
		try {
			properties = new AMQP.BasicProperties ();
			properties.setContentType (message.contentType);
			properties.setContentEncoding (message.contentEncoding);
			properties.setDeliveryMode (2);
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "amqp message publisher encountered an unknown error while preparing the message; aborting!");
			return (false);
		}
		synchronized (this.monitor) {
			try {
				final Channel channel = this.getChannel ();
				channel.basicPublish (message.exchange, message.routingKey, false, false, properties, message.content);
				return (true);
			} catch (final IOException exception) {
				this.callbacks.handleException (exception, "amqp message publisher encountered a network error while publishing the message; aborting and disconnecting!");
				this.disconnect ();
				return (false);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp message publisher encountered an unknown error while publishing the message; aborting and disconnecting!");
				this.disconnect ();
				return (false);
			}
		}
	}
	
	private final void shovelMessage ()
	{
		try {
			final AmqpRawMessage message;
			try {
				message = this.buffer.poll (this.waitTimeout, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException exception) {
				return;
			}
			if (message == null)
				return;
			if (!this.publishMessage (message))
				if (!this.buffer.offerFirst (message))
					this.callbacks.handleLogEvent (Level.WARN, null, "amqp message publisher (message) buffer overflow; ignoring!");
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "amqp message publisher encountered an unknown error while shoveling the message; ignoring!");
		}
	}
	
	public final BlockingDeque<AmqpRawMessage> buffer;
}
