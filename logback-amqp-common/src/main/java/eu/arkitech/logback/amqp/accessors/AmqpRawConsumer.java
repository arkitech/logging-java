
package eu.arkitech.logback.amqp.accessors;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import ch.qos.logback.classic.Level;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import eu.arkitech.logback.common.Callbacks;


public final class AmqpRawConsumer
		extends AmqpRawAccessor
{
	public AmqpRawConsumer (
			final String host, final Integer port, final String virtualHost, final String username, final String password,
			final String exchange, final String queue, final String routingKey, final BlockingQueue<AmqpMessage> buffer,
			final Callbacks callbacks, final Object monitor)
	{
		super (host, port, virtualHost, username, password, callbacks, monitor);
		synchronized (this.monitor) {
			this.exchange = (exchange != null) ? exchange : AmqpRawConsumer.defaultExchange;
			this.queue = (queue != null) ? queue : AmqpRawConsumer.defaultQueue;
			this.routingKey = (routingKey != null) ? routingKey : AmqpRawConsumer.defaultRoutingKey;
			this.buffer = (buffer != null) ? buffer : new LinkedBlockingDeque<AmqpMessage> ();
		}
	}
	
	public final BlockingQueue<AmqpMessage> getBuffer ()
	{
		return (this.buffer);
	}
	
	protected final void loop ()
	{
		loop : while (true) {
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
						break loop;
					if (this.reconnect ())
						break;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
			}
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
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
					if (this.shouldStopSoft ())
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
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer shoveling inbound messages");
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
			}
		}
		synchronized (this.monitor) {
			if (this.isConnected ())
				this.disconnect ();
		}
	}
	
	private final void consume (final Envelope envelope, final BasicProperties properties, final byte[] content)
	{
		final AmqpMessage message =
				new AmqpMessage (
						envelope.getExchange (), envelope.getRoutingKey (), properties.getContentType (),
						properties.getContentEncoding (), content);
		if (!this.buffer.offer (message))
			this.callbacks.handleLogEvent (Level.ERROR, null, "amqp consumer buffer overrun; ignoring!");
	}
	
	private final boolean declare ()
	{
		final Channel channel = this.getChannel ();
		{
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer declaring the exchange `%s`", this.exchange);
			try {
				channel.exchangeDeclare (this.exchange, "topic", true, false, null);
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while declaring the exchange `%s`; aborting!",
						this.exchange);
				return (false);
			}
		}
		{
			final String queue;
			final boolean unique;
			if ((this.queue == null) || this.queue.isEmpty ()) {
				queue = "";
				unique = true;
			} else {
				queue = this.queue;
				unique = false;
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer declaring the queue `%s`", queue);
			this.queue1 = null;
			try {
				this.queue1 = channel.queueDeclare (queue, true, unique, unique, null).getQueue ();
			} catch (final Throwable exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while declaring the queue `%s`; aborting!", queue);
				return (false);
			}
		}
		{
			this.callbacks.handleLogEvent (
					Level.INFO, null, "amqp consumer binding the queue `%s` to exchange `%s` with routing key `%s`",
					this.queue1, this.exchange, this.routingKey);
			try {
				channel.queueBind (this.queue1, this.exchange, this.routingKey, null);
			} catch (final Throwable exception) {
				this.callbacks
						.handleException (
								exception,
								"amqp consumer encountered an error while binding the queue `%s` to exchange `%s` with routing key `%s`; aborting!",
								this.queue1, this.exchange, this.routingKey);
				return (false);
			}
		}
		return (true);
	}
	
	private final boolean register ()
	{
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer registering the consumer");
		final Channel channel = this.getChannel ();
		try {
			channel.basicConsume (this.queue1, true, this.queue1, true, true, null, new ConsumerCallback ());
			return (true);
		} catch (final Throwable exception) {
			this.callbacks.handleException (
					exception, "amqp consumer encountered an error while registering the consummer; aborting!");
			return (false);
		}
	}
	
	private final BlockingQueue<AmqpMessage> buffer;
	private final String exchange;
	private final String queue;
	private String queue1;
	private final String routingKey;
	
	public static final String defaultExchange = "logging";
	public static final String defaultQueue = "";
	public static final String defaultRoutingKey = "#";
	
	private final class ConsumerCallback
			implements
				Consumer
	{
		public void handleCancelOk (final String consumerTag)
		{}
		
		public void handleConsumeOk (final String consumerTag)
		{}
		
		public void handleDelivery (
				final String consumerTag, final Envelope envelope, final BasicProperties properties, final byte[] content)
		{
			AmqpRawConsumer.this.consume (envelope, properties, content);
		}
		
		public void handleRecoverOk ()
		{}
		
		public void handleShutdownSignal (final String consumerTag, final ShutdownSignalException exception)
		{}
	}
}
