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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;


public final class AmqpRawConsumer
		extends AmqpRawAccessor
{
	public AmqpRawConsumer (final AmqpRawConsumerConfiguration configuration, final BlockingQueue<AmqpRawMessage> buffer)
	{
		super (configuration);
		this.exchange = Preconditions.checkNotNull (!Strings.isNullOrEmpty (configuration.exchange) ? configuration.exchange : AmqpRawConsumerConfiguration.defaultExchange);
		Preconditions.checkArgument (!this.exchange.isEmpty ());
		this.queue = Preconditions.checkNotNull (!Strings.isNullOrEmpty (configuration.queue) ? configuration.queue : "");
		this.routingKey = Preconditions.checkNotNull (!Strings.isNullOrEmpty (configuration.routingKey) ? configuration.routingKey : AmqpRawConsumerConfiguration.defaultRoutingKey);
		Preconditions.checkArgument (!this.routingKey.isEmpty ());
		this.buffer = (buffer != null) ? buffer : new LinkedBlockingDeque<AmqpRawMessage> ();
	}
	
	@Override
	public final boolean isDrained ()
	{
		synchronized (this.monitor) {
			return (this.buffer.isEmpty () && !this.isConnected ());
		}
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
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft () || this.shouldStopHard ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
					if (this.declare ())
						break;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
				continue loop;
			}
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft () || this.shouldStopHard ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
					if (this.register ())
						break;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
				continue loop;
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp message consumer shoveling inbound messages from broker");
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft () || this.shouldStopHard ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
			}
		}
		this.disconnect ();
	}
	
	private final void consumeMessage (final Envelope envelope, final BasicProperties properties, final byte[] content)
	{
		try {
			final AmqpRawMessage message = new AmqpRawMessage (envelope.getExchange (), envelope.getRoutingKey (), properties.getContentType (), properties.getContentEncoding (), content);
			if (!this.buffer.offer (message))
				this.callbacks.handleLogEvent (Level.ERROR, null, "amqp message consumer (message) buffer overflow; ignoring!");
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "amqp message consumer encountered an unknown error while shoveling the message; ignoring!");
		}
	}
	
	private final boolean declare ()
	{
		try {
			final Channel channel = this.getChannel ();
			{
				this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message consumer declaring the exchange `%s`", this.exchange);
				try {
					channel.exchangeDeclare (this.exchange, "topic", true, false, null);
				} catch (final IOException exception) {
					this.callbacks.handleException (exception, "amqp message consumer encountered a network error while declaring the exchange `%s`; aborting and disconnecting!", this.exchange);
					this.disconnect ();
					return (false);
				}
			}
			{
				final boolean unique = this.queue.isEmpty ();
				this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message consumer declaring the queue `%s`", this.queue);
				this.queue1 = null;
				try {
					this.queue1 = channel.queueDeclare (this.queue, true, unique, unique, null).getQueue ();
				} catch (final IOException exception) {
					this.callbacks.handleException (exception, "amqp message consumer encountered a network error while declaring the queue `%s`; aborting and disconnecting!", this.queue);
					this.disconnect ();
					return (false);
				}
			}
			{
				this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message consumer binding the queue `%s` to exchange `%s` with routing key `%s`", this.queue1, this.exchange, this.routingKey);
				try {
					channel.queueBind (this.queue1, this.exchange, this.routingKey, null);
				} catch (final IOException exception) {
					this.callbacks.handleException (exception, "amqp message consumer encountered a network error while binding the queue `%s` to exchange `%s` with routing key `%s`; aborting and disconnecting!", this.queue1, this.exchange, this.routingKey);
					this.disconnect ();
					return (false);
				}
			}
			return (true);
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "amqp message consumer encountered an unknown error while declaring; aborting and disconnecting!");
			this.disconnect ();
			return (false);
		}
	}
	
	private final boolean register ()
	{
		try {
			this.callbacks.handleLogEvent (Level.DEBUG, null, "amqp message consumer registering the consumer to the queue `%s`", this.queue1);
			final Channel channel = this.getChannel ();
			try {
				channel.basicConsume (this.queue1, true, this.queue1, true, true, null, new ConsumerCallback ());
				return (true);
			} catch (final IOException exception) {
				this.callbacks.handleException (exception, "amqp message consumer encountered an error while registering the consummer to the queue `%s`; aborting and disconnerting!", this.queue1);
				this.disconnect ();
				return (false);
			}
		} catch (final Error exception) {
			this.callbacks.handleException (exception, "amqp message consumer encountered an unknown error while registering; aborting and disconnecting!");
			this.disconnect ();
			return (false);
		}
	}
	
	public final BlockingQueue<AmqpRawMessage> buffer;
	private final String exchange;
	private final String queue;
	private String queue1;
	private final String routingKey;
	
	private final class ConsumerCallback
			implements
				Consumer
	{
		@Override
		public void handleCancelOk (final String consumerTag)
		{}
		
		@Override
		public void handleConsumeOk (final String consumerTag)
		{}
		
		@Override
		public void handleDelivery (final String consumerTag, final Envelope envelope, final BasicProperties properties, final byte[] content)
		{
			AmqpRawConsumer.this.consumeMessage (envelope, properties, content);
		}
		
		@Override
		public void handleRecoverOk ()
		{}
		
		@Override
		public void handleShutdownSignal (final String consumerTag, final ShutdownSignalException exception)
		{}
	}
}
